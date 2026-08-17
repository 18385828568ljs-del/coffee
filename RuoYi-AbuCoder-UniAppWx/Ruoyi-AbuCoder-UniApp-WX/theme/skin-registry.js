export const SKIN_COMPONENTS = Object.freeze([
	{ key: 'homeBanner', name: '头部 Banner 背景', page: 'home', stretchMode: 'cover', logicalWidth: 750, logicalHeight: 360, textRoles: ['bannerTitle', 'bannerSubtitle'] },
	{ key: 'actionCard', name: '功能按钮卡片背景', page: 'home', stretchMode: 'fill', logicalWidth: 335, logicalHeight: 180, textRoles: ['actionTitle', 'actionSubtitle'] },
	{ key: 'sectionBanner', name: '欢迎牌 / 分区横幅背景', page: 'home', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 144, textRoles: ['sectionTitle'] },
	{ key: 'aboutImage', name: '关于我们图片', page: 'home', stretchMode: 'contain', logicalWidth: 686, logicalHeight: 1000, textRoles: ['sectionTitle'] },
	{ key: 'productCard', name: '商品卡片背景', page: 'menu', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 320, textRoles: ['productTitle', 'price', 'metaText', 'bodyText', 'buttonPrimary'] },
	{ key: 'specPanel', name: '商品规格弹窗背景', page: 'menu', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 720, textRoles: ['panelTitle', 'optionTitle', 'optionText', 'price', 'buttonPrimary', 'buttonSecondary'] },
	{ key: 'emptyCart', name: '空购物车面板背景', page: 'cart', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 560, textRoles: ['emptyTitle', 'emptyDescription', 'buttonPrimary'] },
	{ key: 'cartPanel', name: '购物车结算面板背景', page: 'cart', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 180, textRoles: ['panelTitle', 'price', 'metaText', 'buttonPrimary'] },
	{ key: 'checkoutBar', name: '确认订单底部操作条背景', page: 'checkout', stretchMode: 'fill', logicalWidth: 750, logicalHeight: 124, textRoles: ['price', 'metaText', 'buttonPrimary'] },
	{ key: 'memberCard', name: '会员卡片背景', page: 'me', stretchMode: 'fill', logicalWidth: 686, logicalHeight: 224, textRoles: ['memberTitle', 'memberValue', 'metaText', 'buttonSecondary'] },
	{ key: 'tabBar', name: '底部 TabBar 背景', page: 'global', stretchMode: 'fill', logicalWidth: 750, logicalHeight: 112, textRoles: ['tabText', 'tabTextActive'] }
].map((item) => Object.freeze({
	...item,
	backgroundEditable: true,
	layoutLocked: true,
	aspectRatio: Number((item.logicalWidth / item.logicalHeight).toFixed(4))
})))

export const SKIN_COMPONENT_KEYS = Object.freeze(SKIN_COMPONENTS.map((item) => item.key))

export const SKIN_COMPONENT_MAP = Object.freeze(SKIN_COMPONENTS.reduce((result, item) => {
	result[item.key] = item
	return result
}, {}))

export const SKIN_PAGES = Object.freeze([
	{ key: 'home', name: '首页' },
	{ key: 'menu', name: '点单' },
	{ key: 'cart', name: '购物车' },
	{ key: 'checkout', name: '确认订单' },
	{ key: 'me', name: '我的' },
	{ key: 'global', name: '全局' }
])

export function isSkinComponentKey(key) {
	return !!SKIN_COMPONENT_MAP[key]
}
