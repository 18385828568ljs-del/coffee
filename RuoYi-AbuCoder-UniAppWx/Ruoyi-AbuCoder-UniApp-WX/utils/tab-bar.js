export const TAB_PAGE_ROUTES = [
	'/pages/index/index',
	'/pages/scan/menu',
	'/pages/cart/cart',
	'/pages/me/me'
]

function currentRoute() {
	if (typeof getCurrentPages !== 'function') return ''
	const pages = getCurrentPages()
	const route = pages.length && pages[pages.length - 1].route
	return route ? `/${String(route).replace(/^\/+/, '')}` : ''
}

function nativePage(vm) {
	const candidates = [vm && vm.$mp && vm.$mp.page, vm && vm.$scope, vm]
	return candidates.find((candidate) => candidate && typeof candidate.getTabBar === 'function')
}

export function syncCustomTabBar(vm) {
	const index = TAB_PAGE_ROUTES.indexOf(currentRoute())
	if (index < 0) return false

	const page = nativePage(vm)
	const tabBar = page && page.getTabBar()
	if (!tabBar || typeof tabBar.setData !== 'function') return false

	tabBar.setData({ current: index })
	return true
}
