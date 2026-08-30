const PLACEMENT_STYLES = Object.freeze({
	LEFT_CENTER: Object.freeze({ left: '6%', right: 'auto', top: '50%', transform: 'translateY(-50%)' }),
	CENTER: Object.freeze({ left: '50%', right: 'auto', top: '50%', transform: 'translate(-50%, -50%)' }),
	RIGHT_CENTER: Object.freeze({ left: 'auto', right: '6%', top: '50%', transform: 'translateY(-50%)' })
})

const SIZE_STYLES = Object.freeze({ SMALL: '25%', MEDIUM: '40%', LARGE: '55%' })

export const DECORATION_KEYS = Object.freeze(['homeBannerArtText', 'sectionBannerArtText'])
export const PLACEMENT_PRESETS = Object.freeze(Object.keys(PLACEMENT_STYLES))
export const SIZE_PRESETS = Object.freeze(Object.keys(SIZE_STYLES))

export function decorationPresetStyle(decoration = {}) {
	return {
		...(PLACEMENT_STYLES[decoration.placementPreset] || PLACEMENT_STYLES.LEFT_CENTER),
		width: SIZE_STYLES[decoration.sizePreset] || SIZE_STYLES.MEDIUM
	}
}
