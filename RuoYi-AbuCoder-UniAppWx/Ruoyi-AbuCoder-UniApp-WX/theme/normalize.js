import { cloneDefaultTheme, cloneDefaultSkin } from './defaults.js'
import { COMPONENT_REGISTRY, isAllowedVariant } from './registry.js'
import { normalizeBackground, normalizeStructuredBackground } from './background-slots.js'
import { SKIN_COMPONENT_KEYS } from './skin-registry.js'
import { TYPOGRAPHY_REGISTRY, TYPOGRAPHY_ROLES } from './typography-registry.js'
import { CONTENT_PRESETS, SAFE_AREA_PRESETS } from './layout-registry.js'
import { DECORATION_KEYS, PLACEMENT_PRESETS, SIZE_PRESETS } from './decoration-registry.js'

const COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/
const SKIN_COLOR_PATTERN = /^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/

function copyColors(target, source) {
	Object.keys(target).forEach((key) => {
		if (source && COLOR_PATTERN.test(String(source[key] || ''))) target[key] = source[key].toUpperCase()
	})
}

function copyRadius(target, source) {
	Object.keys(target).forEach((key) => {
		const value = Number(source && source[key])
		if (Number.isInteger(value) && value >= 0 && value <= 32) target[key] = value
	})
}

export function normalizeThemeConfig(input, templateKey = 'coffee') {
	const fallback = cloneDefaultTheme(templateKey)
	if (input && typeof input === 'object' && Number(input.schemaVersion) === 1) {
		return normalizeLegacyTheme(normalizeSkinConfigV1(input), fallback)
	}
	if (!input || typeof input !== 'object' || input.schemaVersion !== '1.0.0') return fallback

	copyColors(fallback.tokens.colors, input.tokens && input.tokens.colors)
	copyRadius(fallback.tokens.radius, input.tokens && input.tokens.radius)
	const shadow = input.tokens && input.tokens.shadow && input.tokens.shadow.card
	if (['none', 'soft', 'medium'].includes(shadow)) fallback.tokens.shadow.card = shadow

	const brand = input.brand || {}
	;['logoAssetId', 'headerAssetId'].forEach((key) => {
		if (/^\d+$/.test(String(brand[key] || ''))) fallback.brand[key] = String(brand[key])
	})

	Object.keys(COMPONENT_REGISTRY).forEach((key) => {
		const source = input.components && input.components[key]
		if (!source || typeof source !== 'object') return
		if (isAllowedVariant(key, source.variant)) fallback.components[key].variant = source.variant
		if (key === 'activityBanner' && typeof source.visible === 'boolean') fallback.components[key].visible = source.visible
		const background = normalizeBackground(key, source.background)
		if (background) fallback.components[key].background = background
	})
	return fallback
}

function copySkinColors(target, source) {
	Object.keys(target).forEach((key) => {
		if (source && SKIN_COLOR_PATTERN.test(String(source[key] || ''))) target[key] = source[key].toUpperCase()
	})
}

function copyTypographyToken(target, source, registry) {
	if (!source || typeof source !== 'object') return
	if (SKIN_COLOR_PATTERN.test(String(source.color || ''))) target.color = source.color.toUpperCase()
	const fontSize = Number(source.fontSize)
	const sizeControl = registry.controls.fontSize
	if (Number.isFinite(fontSize) && fontSize >= sizeControl.min && fontSize <= sizeControl.max) target.fontSize = fontSize
	const fontWeight = Number(source.fontWeight)
	if (registry.controls.fontWeight.includes(fontWeight)) target.fontWeight = fontWeight
	const fontId = Number(source.fontId)
	if (Number.isInteger(fontId) && fontId > 0) target.fontId = fontId
	if (source.fontStyle === 'normal' || source.fontStyle === 'italic') target.fontStyle = source.fontStyle
	const lineHeight = Number(source.lineHeight)
	if (Number.isFinite(lineHeight) && lineHeight >= 1 && lineHeight <= 2) target.lineHeight = lineHeight
	const letterSpacing = Number(source.letterSpacing)
	if (Number.isFinite(letterSpacing) && letterSpacing >= 0 && letterSpacing <= 4) target.letterSpacing = letterSpacing
	if (typeof source.fontFamily === 'string' && source.fontFamily.length <= 120) target.fontFamily = source.fontFamily
	if (typeof source.textShadow === 'string' && source.textShadow.length <= 120) target.textShadow = source.textShadow
}

function isSkinAssetValue(value) {
	return value === null || (typeof value === 'string' && value.length <= 1000)
		|| (Number.isInteger(value) && value > 0)
}

export function normalizeSkinConfigV1(input) {
	const fallback = cloneDefaultSkin()
	if (!input || typeof input !== 'object' || Number(input.schemaVersion) !== 1) return fallback
	fallback.themeVersion = Number.isInteger(Number(input.themeVersion)) && Number(input.themeVersion) > 0 ? Number(input.themeVersion) : 1
	copySkinColors(fallback.page, input.page)
	fallback.page.background = normalizeStructuredBackground(input.page && input.page.background, fallback.page.backgroundColor)
	copySkinColors(fallback.colors, input.colors)
	fallback.colors.pageBackground = fallback.page.backgroundColor
	fallback.colors.textPrimary = fallback.page.textColor
	fallback.colors.textSecondary = fallback.page.secondaryTextColor
	const homeBannerContent = input.content && input.content.homeBanner
	if (homeBannerContent && typeof homeBannerContent === 'object') {
		if (typeof homeBannerContent.visible === 'boolean') fallback.content.homeBanner.visible = homeBannerContent.visible
		if (typeof homeBannerContent.title === 'string' && homeBannerContent.title.length <= 40) fallback.content.homeBanner.title = homeBannerContent.title
		if (typeof homeBannerContent.subtitle === 'string' && homeBannerContent.subtitle.length <= 60) fallback.content.homeBanner.subtitle = homeBannerContent.subtitle
	}
	const shopHeaderContent = input.content && input.content.shopHeader
	if (shopHeaderContent && typeof shopHeaderContent === 'object') {
		const logoAssetId = Number(shopHeaderContent.logoAssetId)
		if (Number.isInteger(logoAssetId) && logoAssetId > 0) fallback.content.shopHeader.logoAssetId = logoAssetId
		if (typeof shopHeaderContent.title === 'string' && shopHeaderContent.title.length <= 40) fallback.content.shopHeader.title = shopHeaderContent.title
		if (typeof shopHeaderContent.subtitle === 'string' && shopHeaderContent.subtitle.length <= 60) fallback.content.shopHeader.subtitle = shopHeaderContent.subtitle
	}
	const homeBannerLayout = input.layout && input.layout.homeBanner
	if (homeBannerLayout && typeof homeBannerLayout === 'object') {
		if (CONTENT_PRESETS.includes(homeBannerLayout.contentPreset)) fallback.layout.homeBanner.contentPreset = homeBannerLayout.contentPreset
		if (SAFE_AREA_PRESETS.includes(homeBannerLayout.safeAreaPreset)) fallback.layout.homeBanner.safeAreaPreset = homeBannerLayout.safeAreaPreset
	}
	DECORATION_KEYS.forEach((key) => {
		const source = input.decorations && input.decorations[key]
		const assetId = Number(source && source.assetId)
		if (!Number.isInteger(assetId) || assetId <= 0 || !PLACEMENT_PRESETS.includes(source.placementPreset)
			|| !SIZE_PRESETS.includes(source.sizePreset)) return
		fallback.decorations[key] = { assetId, placementPreset: source.placementPreset, sizePreset: source.sizePreset }
	})

	SKIN_COMPONENT_KEYS.forEach((key) => {
		const value = input.assets && input.assets[key]
		if (key === 'homeBanner') {
			const items = Array.isArray(value) ? value : (value == null ? [] : [value])
			fallback.assets.homeBanner = items.slice(0, 10).filter((item) => item !== null && isSkinAssetValue(item))
			return
		}
		if (isSkinAssetValue(value)) fallback.assets[key] = value
	})
	const productImages = input.productImages && typeof input.productImages === 'object' && !Array.isArray(input.productImages)
		? input.productImages
		: {}
	Object.keys(productImages).slice(0, 200).forEach((productId) => {
		const value = productImages[productId]
		if (!/^[1-9][0-9]*$/.test(productId)) return
		if (value === null || (typeof value === 'string' && value.length <= 1000)
			|| (Number.isInteger(value) && value > 0)) fallback.productImages[productId] = value
	})
	TYPOGRAPHY_ROLES.forEach((key) => copyTypographyToken(fallback.typography[key], input.typography && input.typography[key], TYPOGRAPHY_REGISTRY[key]))

	Object.keys(fallback.slots).forEach((key) => {
		const source = input.slots && input.slots[key]
		if (!source || typeof source !== 'object') return
		Object.keys(fallback.slots[key]).forEach((field) => {
			if (source[field] !== undefined) fallback.slots[key][field] = source[field]
		})
		if (typeof source.assetId === 'string') fallback.slots[key].assetId = source.assetId
	})
	return fallback
}

function normalizeLegacyTheme(input, fallback) {
	const page = input.page || {}
	const slots = input.slots || {}
	copyColors(fallback.tokens.colors, {
		pageBackground: page.backgroundColor,
		textPrimary: page.textColor,
		textSecondary: page.secondaryTextColor
	})
	const order = slots.orderCard || {}
	copyColors(fallback.tokens.colors, {
		primary: (input.colors || {}).primary || order.iconColor,
		buttonBackground: (input.colors || {}).primary || order.backgroundColor,
		buttonText: order.textColor,
		surface: (input.colors || {}).cardBackground || order.backgroundColor
	})
	copyRadius(fallback.tokens.radius, { card: order.radius, image: (slots.welcomeBanner || {}).radius })
	if (order.shadow && ['none', 'soft', 'medium'].includes(order.shadow)) fallback.tokens.shadow.card = order.shadow === 'light' ? 'soft' : order.shadow
	Object.keys(fallback.components).forEach((key) => { fallback.components[key].background = undefined })
	return fallback
}
