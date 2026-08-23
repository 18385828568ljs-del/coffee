export const CONTENT_PRESETS = Object.freeze(['LEFT_CENTER', 'CENTER', 'RIGHT_CENTER'])
export const SAFE_AREA_PRESETS = Object.freeze(['LEFT_CENTER_LARGE', 'CENTER_LARGE', 'RIGHT_CENTER_LARGE'])

const CONTENT_STYLES = Object.freeze({
	LEFT_CENTER: Object.freeze({ left: '42rpx', right: 'auto', width: '54%', alignItems: 'flex-start', textAlign: 'left' }),
	CENTER: Object.freeze({ left: '15%', right: '15%', width: '70%', alignItems: 'center', textAlign: 'center' }),
	RIGHT_CENTER: Object.freeze({ left: 'auto', right: '42rpx', width: '54%', alignItems: 'flex-end', textAlign: 'right' })
})

export function layoutPresetStyle(layout = {}) {
	return { ...(CONTENT_STYLES[layout.contentPreset] || CONTENT_STYLES.LEFT_CENTER) }
}
