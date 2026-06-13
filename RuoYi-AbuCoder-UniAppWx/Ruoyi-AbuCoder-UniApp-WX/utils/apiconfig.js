/**
 * 咖啡商城接口配置
 * 正式上线时请将 baseUrl 替换为真实服务地址。
 */

// export const baseUrl = 'https://your-domain.com'
// 真机/cpolar 公网调试地址;切回本机可改为 http://127.0.0.1:8080
const DEFAULT_BASE_URL = 'http://127.0.0.1:8080'

function normalizeBaseUrl(value) {
	return String(value || '').trim().replace(/\/+$/, '')
}

function resolveRuntimeBaseUrl() {
	// 优先使用 DEFAULT_BASE_URL,避免 storage 里残留旧地址覆盖。
	// 如需运行时切换,手动改 DEFAULT_BASE_URL 后重新编译即可。
	return normalizeBaseUrl(DEFAULT_BASE_URL)
}

export const baseUrl = resolveRuntimeBaseUrl()
export const uploadUrl = `${baseUrl}/common/upload`

export function isLoopbackBaseUrl(url = baseUrl) {
	return /^(https?:\/\/)?(localhost|127\.0\.0\.1|\[::1\]|::1)(:\d+)?$/i.test(
		normalizeBaseUrl(url)
	)
}

export function getApiBaseUrlHint(url = baseUrl) {
	return [
		`当前小程序接口地址：${normalizeBaseUrl(url) || DEFAULT_BASE_URL}`,
		'如果你在真机、局域网预览或非本机环境运行，localhost 无法访问。',
		'请把 utils/apiconfig.js 里的 baseUrl，或缓存里的 apiBaseUrl，改成后端可访问地址。',
		'示例：http://你的电脑局域网IP:8080'
	].join('\n')
}

function getBaseOrigin() {
	const match = String(baseUrl || '').match(/^(https?:\/\/[^/]+)/i)
	return match ? match[1] : String(baseUrl || '').replace(/\/+$/, '')
}

function normalizeSlashes(value) {
	return String(value || '').replace(/\\/g, '/')
}

function parseHttpUrl(value) {
	const match = String(value || '').match(/^(https?:)?\/\/([^/]+)(.*)$/i)
	if (!match) {
		return null
	}
	return {
		protocol: match[1] || 'http:',
		host: match[2] || '',
		pathname: match[3] || ''
	}
}

export function resolveImageUrl(url) {
	if (!url) {
		return ''
	}
	const normalizedUrl = normalizeSlashes(url).trim()
	if (!normalizedUrl) {
		return ''
	}
	if (/^data:/i.test(normalizedUrl) || /^\/static\//.test(normalizedUrl)) {
		return normalizedUrl
	}
	if (/^(https?:)?\/\//i.test(normalizedUrl)) {
		const parsedUrl = parseHttpUrl(normalizedUrl)
		if (!parsedUrl) {
			return normalizedUrl
		}
		if (/^(localhost|127\.0\.0\.1|::1)(:\d+)?$/i.test(parsedUrl.host)) {
			return `${getBaseOrigin()}${parsedUrl.pathname}`
		}
		return normalizedUrl
	}
	if (/^profile\//i.test(normalizedUrl)) {
		return `${getBaseOrigin()}/${normalizedUrl.replace(/^\/+/, '')}`
	}
	if (normalizedUrl.startsWith('/')) {
		return `${baseUrl}${normalizedUrl}`
	}
	if (/^\.\//.test(normalizedUrl)) {
		return `${baseUrl}/${normalizedUrl.replace(/^\.?\//, '')}`
	}
	if (/^[a-zA-Z]:\//.test(normalizedUrl)) {
		return ''
	}
	if (/^static\//i.test(normalizedUrl)) {
		return `/${normalizedUrl.replace(/^\/+/, '')}`
	}
	if (/^upload\//i.test(normalizedUrl)) {
		return `${getBaseOrigin()}/${normalizedUrl.replace(/^\/+/, '')}`
	}
	if (/^common\//i.test(normalizedUrl)) {
		return `${getBaseOrigin()}/${normalizedUrl.replace(/^\/+/, '')}`
	}
	if (/^profile\/upload\//i.test(normalizedUrl)) {
		return `${getBaseOrigin()}/${normalizedUrl.replace(/^\/+/, '')}`
	}
	if (/^(blob:|wxfile:|file:)/i.test(normalizedUrl)) {
		return normalizedUrl
	}
	if (/^(https?:)?\/\//i.test(url) || /^data:/i.test(url) || /^\/static\//.test(url)) {
		return url
	}
	return `${getBaseOrigin()}/${normalizedUrl.replace(/^\.?\//, '')}`
}

export const productApi = {
	categories: `${baseUrl}/api/product/categories`,
	list: `${baseUrl}/api/product/list`,
	categoryProducts: `${baseUrl}/api/product/category/`,
	detail: `${baseUrl}/api/product/`,
	search: `${baseUrl}/api/product/search`
}

export const cartApi = {
	list: `${baseUrl}/api/cart/list`,
	add: `${baseUrl}/api/cart/add`,
	update: `${baseUrl}/api/cart/update`,
	delete: `${baseUrl}/api/cart/`,
	clear: `${baseUrl}/api/cart/clear/`
}

export const orderApi = {
	list: `${baseUrl}/api/order/list`,
	detail: `${baseUrl}/api/order/`,
	create: `${baseUrl}/api/order/create`,
	// 当前为业务联调占位接口，后续可替换为真实支付能力。
	pay: `${baseUrl}/api/order/pay/`,
	cancel: `${baseUrl}/api/order/cancel/`,
	confirm: `${baseUrl}/api/order/confirm/`
}

export const addressApi = {
	list: `${baseUrl}/api/address/list`,
	default: `${baseUrl}/api/address/default`,
	detail: `${baseUrl}/api/address/`,
	add: `${baseUrl}/api/address/add`,
	update: `${baseUrl}/api/address/update`,
	delete: `${baseUrl}/api/address/`,
	setDefault: `${baseUrl}/api/address/setDefault/`
}

export const loginApi = {
	wxLogin: `${baseUrl}/wxapi/wxlogin`
}

export const bannerApi = {
	list: `${baseUrl}/wxapi/loadBanner`
}

export const activityApi = {
	list: `${baseUrl}/api/activity/list`,
	preview: `${baseUrl}/api/activity/preview`
}

export const offlineActivityApi = {
	list: `${baseUrl}/api/offlineActivity/list`,
	detail: `${baseUrl}/api/offlineActivity/`,
	signup: `${baseUrl}/api/offlineActivity/signup`,
	cancel: `${baseUrl}/api/offlineActivity/cancel`,
	my: `${baseUrl}/api/offlineActivity/my`
}

export const memberApi = {
	info: `${baseUrl}/api/member/info`,
	levelConfig: `${baseUrl}/api/member/level-config`
}

export const walletApi = {
	info: `${baseUrl}/api/wallet/info`,
	log: `${baseUrl}/api/wallet/log`,
	rechargeTemplates: `${baseUrl}/api/wallet/recharge/templates`,
	recharge: `${baseUrl}/api/wallet/recharge`,
	rechargeCallback: `${baseUrl}/api/wallet/recharge/callback`,
	rechargeRecords: `${baseUrl}/api/wallet/recharge/records`
}

export const scanMenuApi = {
	categories: `${baseUrl}/api/scanMenu/categories`,
	products: `${baseUrl}/api/scanMenu/products`,
	productDetail: `${baseUrl}/api/scanMenu/products/`,
	parseTable: `${baseUrl}/api/scanMenu/table/parse`
}

export const scanCartApi = {
	list: `${baseUrl}/api/scanCart/list`,
	add: `${baseUrl}/api/scanCart/add`,
	update: `${baseUrl}/api/scanCart/update`,
	delete: `${baseUrl}/api/scanCart/`,
	clear: `${baseUrl}/api/scanCart/clear`
}

export const scanOrderApi = {
	list: `${baseUrl}/api/scanOrder/list`,
	detail: `${baseUrl}/api/scanOrder/`,
	subscribeConfig: `${baseUrl}/api/scanOrder/subscribe-config`,
	create: `${baseUrl}/api/scanOrder/create`,
	preview: `${baseUrl}/api/scanOrder/preview`,
	pay: `${baseUrl}/api/scanOrder/pay/`,
	cancel: `${baseUrl}/api/scanOrder/cancel/`,
	urge: `${baseUrl}/api/scanOrder/urge/`
}
