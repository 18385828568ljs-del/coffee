const fs = require('fs')
const path = require('path')

const baseUrl = process.argv[2] || 'http://127.0.0.1:18081'
const outputDir = process.argv[3]
const playwrightPath = process.argv[4] || 'playwright'
const browserExecutable = process.argv[5]
if (!outputDir) throw new Error('Output directory is required')

const { chromium } = require(playwrightPath)
fs.mkdirSync(outputDir, { recursive: true })

const aboutImage = '/static/skin/vintage/section-banner.png'
const productOneImage = '/static/skin/vintage/home-banner.png'
const productTwoImage = '/static/skin/vintage/empty-cart.png'

function previewConfig(productImages) {
	return {
		schemaVersion: 1,
		themeVersion: 1,
		assets: { aboutImage },
		productImages: productImages || {}
	}
}

async function loadPreviewFrame(page, route) {
	await page.goto(`${baseUrl}/?skinImageQa=${Date.now()}`, { waitUntil: 'networkidle' })
	await page.evaluate(({ url, targetRoute }) => {
		document.body.innerHTML = ''
		window.__skinImageQaMessages = []
		window.addEventListener('message', (event) => window.__skinImageQaMessages.push(event.data || {}), { once: false })
		const frame = document.createElement('iframe')
		frame.id = 'skinImageQaFrame'
		frame.src = `${url}/?skinImageQaFrame=${Date.now()}#${targetRoute}`
		frame.style.cssText = 'width:390px;height:844px;border:0;display:block'
		document.body.appendChild(frame)
	}, { url: baseUrl, targetRoute: route })
	await page.locator('#skinImageQaFrame').waitFor()
	await page.waitForFunction(() => window.__skinImageQaMessages.some((message) => message.type === 'MINI_PREVIEW_READY'))
	const frame = page.frames().find((candidate) => candidate.url().includes(route.split('?')[0]))
	if (!frame) throw new Error(`preview frame did not load for ${route}`)
	return frame
}

async function postConfig(page, config) {
	await page.evaluate((payload) => {
		const frame = document.getElementById('skinImageQaFrame')
		frame.contentWindow.postMessage({ type: 'SKIN_CONFIG_UPDATE', payload, assetUrls: {} }, location.origin)
	}, config)
}

async function imageSource(frame, productId) {
	return frame.locator(`[data-skin-component="productImages"][data-product-id="${productId}"] img`).first().getAttribute('src')
}

let browser
;(async () => {
	browser = await chromium.launch({ headless: true, executablePath: browserExecutable || undefined })
	const context = await browser.newContext({ viewport: { width: 430, height: 900 }, deviceScaleFactor: 1 })
	const page = await context.newPage()
	const runtimeErrors = []
	page.on('pageerror', (error) => runtimeErrors.push(error.message))

	const homeFrame = await loadPreviewFrame(page, '/pages/index/index?decoratorPreview=1')
	await homeFrame.waitForSelector('.about-image')
	await postConfig(page, previewConfig())
	await page.waitForTimeout(700)
	const actualAboutImage = await homeFrame.locator('.about-image img').getAttribute('src')
	await page.screenshot({ path: path.join(outputDir, 'skin-about-image.png'), fullPage: true })

	const menuFrame = await loadPreviewFrame(page, '/pages/scan/menu?decoratorPreview=1')
	await menuFrame.waitForSelector('[data-skin-component="productImages"][data-product-id="1"] img')
	await menuFrame.waitForSelector('[data-skin-component="productImages"][data-product-id="2"] img')
	const originalOne = await imageSource(menuFrame, 1)
	const originalTwo = await imageSource(menuFrame, 2)
	await postConfig(page, previewConfig({ 1: productOneImage, 2: productTwoImage }))
	await menuFrame.waitForFunction(({ one, two }) => {
		const first = document.querySelector('[data-skin-component="productImages"][data-product-id="1"] img')
		const second = document.querySelector('[data-skin-component="productImages"][data-product-id="2"] img')
		return first && second && first.getAttribute('src') === one && second.getAttribute('src') === two
	}, { one: productOneImage, two: productTwoImage })
	const overriddenOne = await imageSource(menuFrame, 1)
	const overriddenTwo = await imageSource(menuFrame, 2)
	await page.screenshot({ path: path.join(outputDir, 'skin-product-images.png'), fullPage: true })

	await postConfig(page, previewConfig())
	await menuFrame.waitForFunction(({ one, two }) => {
		const first = document.querySelector('[data-skin-component="productImages"][data-product-id="1"] img')
		const second = document.querySelector('[data-skin-component="productImages"][data-product-id="2"] img')
		return first && second && first.getAttribute('src') === one && second.getAttribute('src') === two
	}, { one: originalOne, two: originalTwo })
	const restoredOne = await imageSource(menuFrame, 1)
	const restoredTwo = await imageSource(menuFrame, 2)
	const displayHealth = await menuFrame.evaluate(() => ({
		brokenImages: Array.from(document.images)
			.filter((image) => image.complete && image.naturalWidth === 0)
			.map((image) => image.currentSrc || image.src),
		horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 1
	}))

	const report = {
		actualAboutImage,
		originalOne,
		originalTwo,
		overriddenOne,
		overriddenTwo,
		restoredOne,
		restoredTwo,
		displayHealth,
		runtimeErrors
	}
	fs.writeFileSync(path.join(outputDir, 'skin-image-qa-report.json'), JSON.stringify(report, null, 2))
	console.log(JSON.stringify(report, null, 2))
	await browser.close()

	const failures = []
	if (actualAboutImage !== aboutImage) failures.push('about image override failed')
	if (overriddenOne !== productOneImage || overriddenTwo !== productTwoImage) failures.push('product image override failed')
	if (overriddenOne === overriddenTwo) failures.push('products share one override image')
	if (restoredOne !== originalOne || restoredTwo !== originalTwo) failures.push('product image restore failed')
	if (displayHealth.brokenImages.length) failures.push('broken images detected')
	if (displayHealth.horizontalOverflow) failures.push('horizontal overflow detected')
	if (runtimeErrors.length) failures.push('runtime errors detected')
	if (failures.length) throw new Error(failures.join('; '))
})().catch((error) => {
	console.error(error)
	process.exitCode = 1
	if (browser) browser.close().catch(() => {})
})
