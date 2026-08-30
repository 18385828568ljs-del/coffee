import { themeRpx } from './units.js'

const role = (label, defaults, controls = {}) => Object.freeze({
	label,
	defaults: Object.freeze({ fontStyle: 'normal', ...defaults }),
	controls: Object.freeze({
		fontSize: { min: 10, max: 32, step: 1 },
		fontWeight: [400, 500, 600, 700, 800],
		lineHeight: { min: 1, max: 2, step: 0.1 },
		letterSpacing: { min: 0, max: 4, step: 0.5 },
		...controls
	})
})

export const TYPOGRAPHY_REGISTRY = Object.freeze({
	pageTitle: role('页面标题', { color: '#332C28', fontSize: 18, fontWeight: 700, lineHeight: 1.3 }),
	sectionTitle: role('分区标题', { color: '#332C28', fontSize: 16, fontWeight: 700, lineHeight: 1.35 }),
	bannerTitle: role('Banner 标题', { color: '#332C28', fontSize: 26, fontWeight: 800, lineHeight: 1.2 }),
	bannerSubtitle: role('Banner 副标题', { color: '#88776A', fontSize: 12, fontWeight: 500, lineHeight: 1.5 }),
	actionTitle: role('功能标题', { color: '#38271F', fontSize: 15, fontWeight: 700, lineHeight: 1.3 }),
	actionSubtitle: role('功能副标题', { color: '#88776A', fontSize: 10, fontWeight: 500, lineHeight: 1.4 }),
	productTitle: role('商品名称', { color: '#38271F', fontSize: 18, fontWeight: 700, lineHeight: 1.3 }, { fontSize: { min: 14, max: 22, step: 1 } }),
	price: role('价格', { color: '#875629', fontSize: 22, fontWeight: 800, lineHeight: 1.2 }, { fontSize: { min: 16, max: 28, step: 1 } }),
	metaText: role('辅助文字', { color: '#88776A', fontSize: 11, fontWeight: 400, lineHeight: 1.4 }),
	bodyText: role('正文', { color: '#57483E', fontSize: 13, fontWeight: 400, lineHeight: 1.55 }),
	panelTitle: role('面板标题', { color: '#332C28', fontSize: 17, fontWeight: 700, lineHeight: 1.35 }),
	optionTitle: role('规格组标题', { color: '#38271F', fontSize: 14, fontWeight: 700, lineHeight: 1.35 }),
	optionText: role('规格选项', { color: '#57483E', fontSize: 13, fontWeight: 500, lineHeight: 1.35 }),
	buttonPrimary: role('主要按钮', { color: '#FFFFFF', fontSize: 15, fontWeight: 700, lineHeight: 1.2 }),
	buttonSecondary: role('次要按钮', { color: '#6F4E37', fontSize: 14, fontWeight: 600, lineHeight: 1.2 }),
	memberTitle: role('会员标题', { color: '#FFF9EF', fontSize: 16, fontWeight: 700, lineHeight: 1.3 }),
	memberValue: role('会员数值', { color: '#FFFFFF', fontSize: 22, fontWeight: 800, lineHeight: 1.2 }),
	emptyTitle: role('空状态标题', { color: '#38271F', fontSize: 17, fontWeight: 700, lineHeight: 1.35 }),
	emptyDescription: role('空状态说明', { color: '#88776A', fontSize: 12, fontWeight: 400, lineHeight: 1.55 }),
	tabText: role('Tab 文字', { color: '#8A7B70', fontSize: 11, fontWeight: 400, lineHeight: 1.2 }),
	tabTextActive: role('选中 Tab 文字', { color: '#875629', fontSize: 11, fontWeight: 700, lineHeight: 1.2 })
})

export const TYPOGRAPHY_ROLES = Object.freeze(Object.keys(TYPOGRAPHY_REGISTRY))

const toKebab = (value) => value.replace(/[A-Z]/g, (match) => `-${match.toLowerCase()}`)

export function cloneDefaultTypography() {
	return TYPOGRAPHY_ROLES.reduce((result, key) => {
		result[key] = { ...TYPOGRAPHY_REGISTRY[key].defaults }
		return result
	}, {})
}

export function buildTypographyVariables(typography = {}, fontResources = {}) {
	return TYPOGRAPHY_ROLES.reduce((style, key) => {
		const token = { ...TYPOGRAPHY_REGISTRY[key].defaults, ...(typography[key] || {}) }
		const prefix = `--skin-${toKebab(key)}`
		style[`${prefix}-color`] = token.color
		style[`${prefix}-size`] = themeRpx(Number(token.fontSize) * 2)
		style[`${prefix}-weight`] = String(token.fontWeight)
		style[`${prefix}-line-height`] = String(token.lineHeight)
		style[`${prefix}-letter-spacing`] = themeRpx(Number(token.letterSpacing || 0) * 2)
		const font = token.fontId ? fontResources[String(token.fontId)] : null
		style[`${prefix}-family`] = (font && font.familyName) || token.fontFamily || 'inherit'
		style[`${prefix}-style`] = token.fontStyle || (font && font.style) || 'normal'
		style[`${prefix}-shadow`] = token.textShadow || 'none'
		return style
	}, {})
}
