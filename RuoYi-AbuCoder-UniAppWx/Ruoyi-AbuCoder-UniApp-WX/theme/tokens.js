import { themeRpx } from './units.js'

export function buildThemeTokenStyle(config) {
	const tokens = config.tokens
	const colors = tokens.colors
	const radius = tokens.radius
	const shadow = tokens.shadow.card === 'none'
		? 'none'
		: tokens.shadow.card === 'medium'
			? `0 ${themeRpx(14)} ${themeRpx(34)} rgba(20, 28, 24, 0.16)`
			: `0 ${themeRpx(10)} ${themeRpx(26)} rgba(20, 28, 24, 0.09)`
	return {
		'--theme-primary': colors.primary,
		'--theme-page': colors.pageBackground,
		'--theme-surface': colors.surface,
		'--theme-text': colors.textPrimary,
		'--theme-text-secondary': colors.textSecondary,
		'--theme-button': colors.buttonBackground,
		'--theme-button-text': colors.buttonText,
		'--theme-border': colors.border,
		'--theme-card-radius': themeRpx(radius.card * 2),
		'--theme-button-radius': themeRpx(radius.button * 2),
		'--theme-image-radius': themeRpx(radius.image * 2),
		'--theme-card-shadow': shadow,
		backgroundColor: colors.pageBackground,
		color: colors.textPrimary
	}
}
