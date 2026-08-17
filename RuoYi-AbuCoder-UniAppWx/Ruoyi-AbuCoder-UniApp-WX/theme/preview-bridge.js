import { themeRuntime } from './runtime.js'

let installed = false
const PREVIEW_PAGE_ROUTES = Object.freeze({
	home: '/pages/index/index?decoratorPreview=1',
	menu: '/pages/scan/menu?decoratorPreview=1',
	cart: '/pages/cart/cart?decoratorPreview=1',
	checkout: '/pages/order/confirm?decoratorPreview=1',
	me: '/pages/me/me?decoratorPreview=1'
})

export function installDecoratorPreviewBridge() {
	// #ifdef H5
	if (installed || typeof window === 'undefined' || window.parent === window) return false
	let parentOrigin = ''
	try {
		parentOrigin = document.referrer ? new URL(document.referrer).origin : ''
	} catch (error) {}
	if (!parentOrigin) return false
	installed = true
	themeRuntime.setPreviewMode(true)
	window.addEventListener('message', (event) => {
		if (event.source !== window.parent || event.origin !== parentOrigin) return
		const message = event.data || {}
		if (message.type === 'SKIN_CONFIG_UPDATE' && message.payload) {
			themeRuntime.apply(message.payload, {
				assetUrls: message.assetUrls,
				preview: true,
				persist: false
			})
			return
		}
		if (message.type === 'SKIN_PREVIEW_NAVIGATE' && PREVIEW_PAGE_ROUTES[message.page]) {
			uni.reLaunch({ url: PREVIEW_PAGE_ROUTES[message.page] })
		}
	})
	document.addEventListener('click', (event) => {
		const target = event.target && event.target.closest ? event.target : null
		if (!target) return
		const roleNode = target.closest('[data-text-role]')
		const componentNode = target.closest('[data-skin-component]')
		if (!roleNode && !componentNode) return
		window.parent.postMessage({
			type: 'SKIN_EDITOR_SELECT',
			componentKey: componentNode ? componentNode.getAttribute('data-skin-component') : '',
			textRole: roleNode ? roleNode.getAttribute('data-text-role') : ''
		}, parentOrigin)
	}, true)
	window.parent.postMessage({ type: 'MINI_PREVIEW_READY' }, parentOrigin)
	return true
	// #endif
	return false
}
