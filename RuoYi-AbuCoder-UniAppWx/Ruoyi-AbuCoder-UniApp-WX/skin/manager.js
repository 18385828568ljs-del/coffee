import { themeRuntime } from '@/theme/runtime.js'

// Public SkinManager facade. The existing runtime remains the single source
// of truth so ACTIVE and PREVIEW use exactly the same renderer.
export const skinManager = Object.freeze({
	state: themeRuntime.state,
	loadActive: themeRuntime.loadPublished,
	loadPreview: themeRuntime.loadPreview,
	setPreviewEnvironment: themeRuntime.setPreviewEnvironment,
	effectiveTheme: themeRuntime.effectiveTheme,
	setSystemTheme: themeRuntime.setSystemTheme,
	apply: themeRuntime.apply
})

export const SkinManager = skinManager
