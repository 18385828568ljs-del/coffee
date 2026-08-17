const fs = require('fs')
const path = require('path')

const baseUrl = process.argv[2] || 'http://localhost:8081'
const outputDir = process.argv[3]
const playwrightPath = process.argv[4] || 'playwright'
const browserExecutable = process.argv[5]
if (!outputDir) throw new Error('Output directory is required')

const { chromium } = require(playwrightPath)
fs.mkdirSync(outputDir, { recursive: true })

async function inspect(scope, name, screenshotPage = scope) {
	await screenshotPage.waitForTimeout(900)
	const result = await scope.evaluate(() => {
		const components = Array.from(document.querySelectorAll('[data-skin-component]')).map((node) => {
			const rect = node.getBoundingClientRect()
			return {
				key: node.getAttribute('data-skin-component'),
				backgroundImage: getComputedStyle(node).backgroundImage,
				width: Math.round(rect.width),
				height: Math.round(rect.height)
			}
		})
		const brokenImages = Array.from(document.images).filter((image) => image.complete && image.naturalWidth === 0).map((image) => image.currentSrc || image.src)
		return {
			components,
			brokenImages,
			horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 1,
			viewport: { width: window.innerWidth, height: window.innerHeight },
			bodyTextLength: (document.body.innerText || '').length
		}
	})
	await screenshotPage.screenshot({ path: path.join(outputDir, `${name}.png`), fullPage: true })
	return result
}

async function navigate(page, route) {
	await page.goto(`${baseUrl}/?skinQa=${Date.now()}#${route}`, { waitUntil: 'networkidle' })
}

;(async () => {
	const browser = await chromium.launch({ headless: true, executablePath: browserExecutable || undefined })
	const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 })
	const page = await context.newPage()
	const runtimeErrors = []
	page.on('pageerror', (error) => runtimeErrors.push(error.message))

	await navigate(page, '/pages/index/index?decoratorPreview=1')
	await page.locator('.skin-switcher').click()
	await page.waitForTimeout(250)
	await page.locator('.skin-switcher').click()
	await page.waitForTimeout(700)
	const mobileHome = await inspect(page, 'skin-home-mobile')

	await page.setViewportSize({ width: 1200, height: 900 })
	const desktopHome = await inspect(page, 'skin-home-desktop')

	await page.setViewportSize({ width: 390, height: 844 })
	await navigate(page, '/pages/cart/cart?decoratorPreview=1')
	const mobileCart = await inspect(page, 'skin-cart-mobile')

	await page.goto(`${baseUrl}/?skinQaParent=${Date.now()}`, { waitUntil: 'networkidle' })
	await page.evaluate((url) => {
		document.body.innerHTML = ''
		const frame = document.createElement('iframe')
		frame.id = 'skinQaFrame'
		frame.src = `${url}/?skinQaFrame=${Date.now()}#/pages/order/confirm?decoratorPreview=1`
		frame.style.cssText = 'width:390px;height:844px;border:0;display:block'
		document.body.appendChild(frame)
	}, baseUrl)
	await page.locator('#skinQaFrame').waitFor()
	await page.waitForTimeout(1200)
	const checkoutFrame = page.frames().find((frame) => frame.url().includes('/pages/order/confirm'))
	if (!checkoutFrame) throw new Error('checkout preview frame did not load: ' + page.frames().map((frame) => frame.url()).join(', '))
	await checkoutFrame.waitForSelector('[data-skin-component="checkoutBar"]')
	const mobileCheckout = await inspect(checkoutFrame, 'skin-checkout-mobile', page)

	const report = { mobileHome, desktopHome, mobileCart, mobileCheckout, runtimeErrors }
	fs.writeFileSync(path.join(outputDir, 'skin-qa-report.json'), JSON.stringify(report, null, 2))
	console.log(JSON.stringify(report, null, 2))
	await browser.close()

	const observed = [mobileHome, desktopHome, mobileCart, mobileCheckout]
	const failures = []
	if (observed.some((item) => item.horizontalOverflow)) failures.push('horizontal overflow detected')
	if (observed.some((item) => item.brokenImages.length)) failures.push('broken images detected')
	if (!mobileHome.components.some((item) => item.key === 'homeBanner' && item.backgroundImage !== 'none')) failures.push('homeBanner background missing')
	if (!mobileCart.components.some((item) => item.key === 'emptyCart' && item.backgroundImage !== 'none')) failures.push('emptyCart background missing')
	if (!mobileCheckout.components.some((item) => item.key === 'checkoutBar' && item.backgroundImage !== 'none')) failures.push('checkoutBar background missing')
	if (failures.length) throw new Error(failures.join('; '))
})().catch((error) => {
	console.error(error)
	process.exitCode = 1
})
