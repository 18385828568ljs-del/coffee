import { themeRuntime } from '@/theme/runtime.js'

// Components consume resolved values from the shared runtime instead of
// interpreting SkinConfig fields themselves.
export function resolvePageStyle() {
	return themeRuntime.state.config ? themeRuntime.state.config : {}
}

export function resolveSlotStyle(slotKey) {
	return themeRuntime.skinSlotStyle(slotKey)
}

export function resolveAssetStyle(componentKey) {
	return themeRuntime.skinAssetStyle(componentKey)
}

export function resolveEffectiveTheme() {
	return themeRuntime.effectiveTheme()
}

export const skinResolver = Object.freeze({
	page: resolvePageStyle,
	slot: resolveSlotStyle,
	asset: resolveAssetStyle,
	effectiveTheme: resolveEffectiveTheme
})

export const SkinResolver = skinResolver
