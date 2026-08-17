import { themeRpx } from './units.js'

export const BACKGROUND_SLOTS = Object.freeze({
	'shopHeader.background': { componentKey: 'shopHeader', renderMode: 'cover', specVersion: 1 },
	'activityBanner.background': { componentKey: 'activityBanner', renderMode: 'cover', specVersion: 1 },
	'productCard.background': { componentKey: 'productCard', renderMode: 'repeat', specVersion: 1 }
})

export function normalizeBackground(componentKey, background) {
	if (!background || typeof background !== 'object') return null
	const rule = BACKGROUND_SLOTS[background.slotKey]
	const opacity = Number(background.opacity)
	if (!rule || rule.componentKey !== componentKey || rule.renderMode !== background.renderMode) return null
	if (Number(background.slotSpecVersion) !== rule.specVersion || opacity < 0 || opacity > 1) return null
	if (!/^\d+$/.test(String(background.assetId || ''))) return null
	return {
		assetId: String(background.assetId),
		slotKey: background.slotKey,
		slotSpecVersion: rule.specVersion,
		renderMode: rule.renderMode,
		opacity
	}
}

export function buildBackgroundStyle(background, assetUrl) {
	if (!background || !assetUrl) return {}
	return {
		backgroundImage: `linear-gradient(rgba(255,255,255,${1 - background.opacity}), rgba(255,255,255,${1 - background.opacity})), url(${assetUrl})`,
		backgroundRepeat: background.renderMode === 'repeat' ? 'repeat' : 'no-repeat',
		backgroundSize: background.renderMode === 'repeat' ? `${themeRpx(256)} ${themeRpx(256)}` : 'cover',
		backgroundPosition: 'center'
	}
}

const COLOR_PATTERN = /^#[0-9a-fA-F]{3,8}$/

export function normalizeStructuredBackground(input, fallbackColor = '#FFFFFF') {
	const fallback = { type: 'solid', color: fallbackColor }
	if (!input || typeof input !== 'object') return fallback
	if (input.type === 'solid' && COLOR_PATTERN.test(String(input.color || ''))) {
		return { type: 'solid', color: String(input.color).toUpperCase() }
	}
	if (input.type !== 'gradient' || !input.gradient || typeof input.gradient !== 'object') return fallback
	const direction = Number(input.gradient.direction)
	const colors = Array.isArray(input.gradient.colors) ? input.gradient.colors : []
	if (!Number.isFinite(direction) || direction < 0 || direction > 360 || colors.length < 2) return fallback
	const stops = colors.map((item) => ({
		color: String(item && item.color || '').toUpperCase(),
		position: Number(item && item.position)
	})).filter((item) => COLOR_PATTERN.test(item.color) && Number.isInteger(item.position) && item.position >= 0 && item.position <= 100)
	if (stops.length < 2 || stops.some((item, index) => index > 0 && item.position < stops[index - 1].position)) return fallback
	return { type: 'gradient', gradient: { type: 'linear', direction, colors: stops } }
}

export function buildStructuredBackgroundStyle(input, fallbackColor = '#FFFFFF') {
	const background = normalizeStructuredBackground(input, fallbackColor)
	if (background.type === 'gradient') {
		const stops = background.gradient.colors.map((item) => `${item.color} ${item.position}%`).join(', ')
		return { background: `linear-gradient(${background.gradient.direction}deg, ${stops})` }
	}
	return { backgroundColor: background.color }
}
