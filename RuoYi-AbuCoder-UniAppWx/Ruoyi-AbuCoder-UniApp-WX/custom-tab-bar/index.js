const FALLBACK_BASE_URL = 'http://127.0.0.1:8080'
const TABS = [
	{ key: 'home', label: '首页', url: '/pages/index/index', icon: '/static/tabbar/home-inactive.svg', activeIcon: '/static/tabbar/home-active.svg' },
	{ key: 'scan', label: '点单', url: '/pages/scan/menu', icon: '/static/tabbar/scan-inactive.svg', activeIcon: '/static/tabbar/scan-active.svg' },
	{ key: 'cart', label: '购物车', url: '/pages/cart/cart', icon: '/static/tabbar/cart-inactive.svg', activeIcon: '/static/tabbar/cart-active.svg' },
	{ key: 'me', label: '我的', url: '/pages/me/me', icon: '/static/tabbar/me-inactive.svg', activeIcon: '/static/tabbar/me-active.svg' }
]

function apiBaseUrl() {
	try {
		const app = getApp && getApp()
		const configured = app && app.globalData && app.globalData.apiBaseUrl
		if (configured) return String(configured).replace(/\/+$/, '')
		const stored = wx.getStorageSync('apiBaseUrl')
		if (stored) return String(stored).replace(/\/+$/, '')
	} catch (error) {}
	return FALLBACK_BASE_URL
}

function resolveAsset(value, assetUrls, baseUrl) {
	if (value === null || value === undefined || value === '') return ''
	const mapped = assetUrls[String(value)] || value
	if (/^data:/i.test(mapped)) return mapped
	const localHost = String(mapped).match(/^(https?:\/\/)(localhost|127\.0\.0\.1|::1)(:\d+)?(\/.*)?$/i)
	if (localHost) return `${baseUrl}${localHost[4] || '/'}`
	if (/^(https?:)?\/\//i.test(mapped)) return mapped
	if (/^\//.test(mapped)) return `${baseUrl}${mapped}`
	return `${baseUrl}/${String(mapped).replace(/^\.?\//, '')}`
}

Component({
	data: {
		tabs: TABS,
		current: 0,
		backgroundStyle: '',
		backgroundImage: '',
		backgroundColor: '#ffffff',
		textColor: '#777777',
		activeTextColor: '#44352C',
		activeBackgroundColor: 'transparent'
	},
	attached() { this.loadTheme() },
	pageLifetimes: {
		show() {
			const pages = getCurrentPages()
			const route = pages.length ? `/${pages[pages.length - 1].route}` : ''
			const index = TABS.findIndex((item) => item.url === route)
			if (index >= 0) this.setData({ current: index })
			this.loadTheme()
		}
	},
	methods: {
		loadTheme() {
			const baseUrl = apiBaseUrl()
			const storeId = wx.getStorageSync('skin_store_code') || wx.getStorageSync('storeId') || 1
			wx.request({
				url: `${baseUrl}/api/mini/skin?storeId=${encodeURIComponent(storeId)}&_t=${Date.now()}`,
				method: 'GET',
				success: (response) => {
					const payload = response && response.data && response.data.data
					if (!payload) return
					let config = payload.config || payload.configJson
					if (typeof config === 'string') {
						try { config = JSON.parse(config) } catch (error) { return }
					}
					const skin = config && config.slots && config.slots.tabBar
					if (!skin) return
					const assets = (config.assets && config.assets.tabBar) || skin.backgroundImage || skin.assetId
					const assetUrls = payload.assetUrls || {}
					const image = resolveAsset(Array.isArray(assets) ? assets[0] : assets, assetUrls, baseUrl)
					this.setData({
						backgroundStyle: '',
						backgroundImage: image,
						backgroundColor: skin.backgroundColor || '#ffffff',
						textColor: skin.textColor || '#777777',
						activeTextColor: skin.activeTextColor || '#44352C',
						activeBackgroundColor: skin.activeBackgroundColor || 'transparent'
					})
				}
			})
		},
		selectTab(event) {
			const index = Number(event.currentTarget.dataset.index)
			const tab = TABS[index]
			if (!tab || index === this.data.current) return
			this.setData({ current: index })
			wx.switchTab({ url: tab.url })
		}
	}
})
