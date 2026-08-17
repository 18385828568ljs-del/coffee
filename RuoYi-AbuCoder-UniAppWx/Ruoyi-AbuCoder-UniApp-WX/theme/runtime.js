import Vue from 'vue'
import { cloneDefaultTheme, THEME_TEMPLATES, DEFAULT_SKIN_CONFIG, cloneDefaultSkin, cloneLocalSkin, LOCAL_SKINS } from './defaults.js'
import { normalizeThemeConfig, normalizeSkinConfigV1 } from './normalize.js'
import { buildThemeTokenStyle } from './tokens.js'
import { buildBackgroundStyle, buildStructuredBackgroundStyle } from './background-slots.js'
import { themeRpx } from './units.js'
import { SKIN_COMPONENT_MAP } from './skin-registry.js'
import { buildTypographyVariables } from './typography-registry.js'
import { resolveImageUrl } from '@/utils/apiconfig.js'

const STORAGE_KEY = 'coffeeThemeRuntimeV1'
const SKIN_CONFIG_KEY = 'skin_config'
const SKIN_VERSION_KEY = 'skin_version'
const SKIN_STORE_KEY = 'skin_store_code'
const LOCAL_SKIN_KEY = 'coffeeLocalSkin'
const DEFAULT_STORE_ID = '1'
const state = Vue.observable({
	templateKey: 'coffee',
	config: cloneDefaultTheme(),
	skinConfig: cloneDefaultSkin(),
	rawSkinConfig: cloneDefaultSkin(),
	assetUrls: {},
	preview: false,
	source: 'ACTIVE',
	previewId: '',
	storeCode: '',
	versionId: '',
	systemTheme: 'light',
	forcedPreviewTheme: ''
})

function readSystemTheme() {
	try {
		const info = typeof uni !== 'undefined' && uni.getSystemInfoSync ? uni.getSystemInfoSync() : null
		return info && info.theme === 'dark' ? 'dark' : 'light'
	} catch (error) {
		return 'light'
	}
}

state.systemTheme = readSystemTheme()

function apply(config, options = {}) {
	const templateKey = THEME_TEMPLATES[options.templateKey] ? options.templateKey : state.templateKey
	const isSkinConfig = config && Number(config.schemaVersion) === 1
	state.templateKey = templateKey
	state.config = normalizeThemeConfig(config, templateKey)
	state.rawSkinConfig = isSkinConfig ? config : cloneDefaultSkin()
	state.skinConfig = isSkinConfig ? normalizeSkinConfigV1(config) : cloneDefaultSkin()
	state.assetUrls = options.assetUrls && typeof options.assetUrls === 'object' ? { ...options.assetUrls } : {}
	state.preview = !!options.preview
	if (options.source) state.source = options.source
	if (options.previewId !== undefined) state.previewId = options.previewId ? String(options.previewId) : ''
	if (options.versionId !== undefined) state.versionId = options.versionId ? String(options.versionId) : ''
	if (options.persist !== false && typeof uni !== 'undefined') {
		uni.setStorageSync(STORAGE_KEY, { templateKey, config: state.config, assetUrls: state.assetUrls })
		if (isSkinConfig) {
			uni.setStorageSync(SKIN_CONFIG_KEY, state.skinConfig)
			if (options.version != null) uni.setStorageSync(SKIN_VERSION_KEY, String(options.version))
		}
	}
	return state.config
}

function useTemplate(templateKey, persist = true) {
	return apply(cloneDefaultTheme(templateKey), { templateKey, persist })
}

function setPreviewMode(preview) {
	state.preview = !!preview
	if (state.preview) {
		if (state.source === 'ACTIVE') state.source = 'PREVIEW'
		applySystemTheme(effectiveTheme())
	} else if (state.source === 'PREVIEW') {
		state.source = 'ACTIVE'
		state.previewId = ''
		state.forcedPreviewTheme = ''
		applySystemTheme(state.systemTheme)
	}
}

function setPreviewEnvironment(theme) {
	if (state.source !== 'PREVIEW') return false
	if (theme !== 'light' && theme !== 'dark') return false
	state.forcedPreviewTheme = theme
	applySystemTheme(theme)
	return true
}

function effectiveTheme() {
	return state.source === 'PREVIEW' && state.forcedPreviewTheme
		? state.forcedPreviewTheme
		: state.systemTheme
}

function setSystemTheme(theme) {
	state.systemTheme = theme === 'dark' ? 'dark' : 'light'
	if (state.source !== 'PREVIEW' || !state.forcedPreviewTheme) applySystemTheme(state.systemTheme)
}

function applySystemTheme(theme) {
	const normalized = theme === 'dark' ? 'dark' : 'light'
	if (typeof document !== 'undefined' && document.documentElement) {
		document.documentElement.style.colorScheme = normalized
		document.documentElement.dataset.previewTheme = normalized
	}
	if (typeof uni !== 'undefined' && uni.setNavigationBarColor) {
		try {
			uni.setNavigationBarColor({
				frontColor: normalized === 'dark' ? '#FFFFFF' : '#000000',
				backgroundColor: normalized === 'dark' ? '#1D1D1F' : '#FFFFFF'
			})
		} catch (error) {}
	}
}

function useLocalSkin(skinKey = 'vintage', persist = true) {
	const key = LOCAL_SKINS[skinKey] ? skinKey : 'vintage'
	const skin = cloneLocalSkin(key)
	apply(skin, { templateKey: 'coffee', persist: false })
	state.rawSkinConfig = skin
	state.assetUrls = {}
	state.storeCode = ''
	state.versionId = key
	state.source = state.preview ? 'PREVIEW' : 'ACTIVE'
	state.previewId = ''
	state.forcedPreviewTheme = ''
	if (persist && typeof uni !== 'undefined') uni.setStorageSync(LOCAL_SKIN_KEY, key)
	return skin
}

function restoreLocalSkin() {
	if (typeof uni === 'undefined') return null
	const key = String(uni.getStorageSync(LOCAL_SKIN_KEY) || '')
	return LOCAL_SKINS[key] ? useLocalSkin(key, false) : null
}

function restore() {
	if (typeof uni === 'undefined') return state.config
	if (restoreLocalSkin()) return state.config
	const saved = uni.getStorageSync(STORAGE_KEY)
	if (saved && saved.config && Number(saved.config.schemaVersion) !== 1) {
		return apply(saved.config, { templateKey: saved.templateKey, assetUrls: saved.assetUrls, persist: false })
	}
	return state.config
}

async function loadPublished(storeCode) {
	const code = String(storeCode || DEFAULT_STORE_ID).trim()
	let cachedSkin = null
	if (state.storeCode !== code) {
		// 门店切换时先清掉上一家门店的已发布配置，避免请求失败时串用旧主题。
		const cachedStore = typeof uni !== 'undefined' ? String(uni.getStorageSync(SKIN_STORE_KEY) || '') : ''
		cachedSkin = typeof uni !== 'undefined' && cachedStore === code ? uni.getStorageSync(SKIN_CONFIG_KEY) : null
		apply(cachedSkin && cachedSkin.schemaVersion === 1 ? cachedSkin : cloneDefaultSkin(), { templateKey: 'coffee', persist: false })
		state.storeCode = code
		state.versionId = ''
	}
	try {
		const { fetchSkin, fetchPublishedSkinVersion, fetchPublishedTheme } = await import('@/api/theme.js')
		let cachedStore = typeof uni !== 'undefined' ? String(uni.getStorageSync(SKIN_STORE_KEY) || '') : ''
		let cachedVersion = typeof uni !== 'undefined' && cachedStore === code ? String(uni.getStorageSync(SKIN_VERSION_KEY) || '') : ''
		if (typeof uni !== 'undefined') {
			cachedSkin = cachedStore === code ? uni.getStorageSync(SKIN_CONFIG_KEY) : null
			if (cachedSkin && cachedSkin.schemaVersion === 1) apply(cachedSkin, { persist: false })
		}
		const unified = await fetchSkin(code)
		const version = unified && (unified.versionId || unified.versionNo) || await fetchPublishedSkinVersion(code)
		if (version != null && cachedVersion && String(version) === cachedVersion && cachedSkin && cachedSkin.schemaVersion === 1) {
			// 版本未变时也刷新资源地址，避免本地缓存继续使用未编码的旧文件名。
			if (unified && unified.config) {
				apply(unified.config, {
					assetUrls: unified.assetUrls,
					persist: true,
					version: cachedVersion,
					source: 'ACTIVE',
					preview: false,
					previewId: '',
					versionId: cachedVersion
				})
			}
			state.storeCode = code
			state.versionId = cachedVersion
			state.source = 'ACTIVE'
			state.previewId = ''
			return state.config
		}
		const result = unified || await fetchPublishedTheme(code)
		if (!result || !result.config) return state.config
		apply(result.config, { assetUrls: result.assetUrls, persist: true, version: result.versionId || result.version || result.versionNo,
			source: 'ACTIVE', preview: false, previewId: '', versionId: result.versionId || result.versionNo })
		if (typeof uni !== 'undefined') uni.setStorageSync(SKIN_STORE_KEY, code)
		state.storeCode = code
		state.versionId = String(result.versionId || result.version || result.versionNo || '')
		state.source = 'ACTIVE'
		state.previewId = ''
		state.forcedPreviewTheme = ''
		return state.config
	} catch (error) {
		return state.config
	}
}

async function loadPreview(storeCode, previewToken) {
	const code = String(storeCode || '').trim()
	const token = String(previewToken || '').trim()
	if (!token) return null
	try {
		const { fetchSkin } = await import('@/api/theme.js')
		const result = await fetchSkin(code, token)
		if (!result || result.source !== 'PREVIEW' || !result.config) return null
		apply(result.config, {
			assetUrls: result.assetUrls,
			persist: false,
			preview: true,
			source: 'PREVIEW',
			previewId: result.previewId,
			versionId: result.versionId || result.draftRevision
		})
		state.storeCode = code || String(result.storeCode || '').trim()
		state.forcedPreviewTheme = ''
		applySystemTheme(effectiveTheme())
		return result
	} catch (error) {
		return null
	}
}

function component(key) {
	return (state.config.components && state.config.components[key]) || {}
}

function backgroundStyle(key) {
	const background = component(key).background
	return buildBackgroundStyle(background, background && state.assetUrls[background.assetId])
}

function assetUrl(assetId) {
	const value = assetId ? state.assetUrls[String(assetId)] || '' : ''
	return value ? resolveImageUrl(value) : ''
}

function skinSlot(key) {
	return (state.skinConfig && state.skinConfig.slots && state.skinConfig.slots[key]) || {}
}

function skinSlotStyle(key) {
	const slot = skinSlot(key)
	const style = {}
	if (slot.background) Object.assign(style, buildStructuredBackgroundStyle(slot.background, slot.backgroundColor))
	if (!slot.background && /^#[0-9A-Fa-f]{3,8}$/.test(String(slot.backgroundColor || ''))) style.backgroundColor = slot.backgroundColor
	const image = (slot.backgroundImage && resolveImageUrl(slot.backgroundImage)) || assetUrl(slot.assetId)
	if (slot.backgroundType === 'image' && image) {
		style.backgroundImage = `url(${image})`
		style.backgroundRepeat = 'no-repeat'
		style.backgroundPosition = 'center'
		style.backgroundSize = slot.fit === 'contain' ? 'contain' : 'cover'
	}
	if (Number.isFinite(Number(slot.radius))) style.borderRadius = themeRpx(Number(slot.radius) * 2)
	return style
}

const LEGACY_ASSET_SLOTS = Object.freeze({
	homeBanner: 'heroBanner', actionCard: 'orderCard', sectionBanner: 'welcomeBanner', aboutImage: 'aboutSection', tabBar: 'tabBar'
})

function resolveSkinAssetValue(value) {
	if (Number.isInteger(value) && value > 0) return assetUrl(value)
	if (typeof value === 'string' && value) {
		return /^\d+$/.test(value) ? assetUrl(value) : resolveImageUrl(value)
	}
	return ''
}

function skinAssets(key) {
	const value = state.skinConfig && state.skinConfig.assets && state.skinConfig.assets[key]
	const values = Array.isArray(value) ? value : (value == null ? [] : [value])
	return values.map(resolveSkinAssetValue).filter(Boolean)
}

function skinAsset(key) {
	const resolved = skinAssets(key)[0]
	if (resolved) return resolved
	const legacyKey = LEGACY_ASSET_SLOTS[key]
	const legacy = legacyKey ? skinSlot(legacyKey) : null
	if (!legacy) return ''
	if (key === 'aboutImage') return resolveImageUrl(legacy.image) || assetUrl(legacy.assetId)
	return resolveImageUrl(legacy.backgroundImage) || assetUrl(legacy.assetId)
}

function skinProductImage(productId) {
	if (productId === undefined || productId === null || productId === '') return ''
	const images = state.skinConfig && state.skinConfig.productImages
	return resolveSkinAssetValue(images && images[String(productId)])
}

function skinAssetStyle(key) {
	const spec = SKIN_COMPONENT_MAP[key]
	const image = skinAsset(key)
	const style = {}
	if (image) {
		style.backgroundImage = `url(${image})`
		style.backgroundRepeat = 'no-repeat'
		style.backgroundPosition = 'center'
		style.backgroundSize = spec && spec.stretchMode === 'cover' ? 'cover' : (spec && spec.stretchMode === 'contain' ? 'contain' : '100% 100%')
	}
	return style
}

function typographyToken(role) {
	return (state.skinConfig && state.skinConfig.typography && state.skinConfig.typography[role]) || {}
}

function skinContent(key) {
	return (state.skinConfig && state.skinConfig.content && state.skinConfig.content[key]) || {}
}

function serializeStyle(style) {
	return Object.keys(style).map((key) => {
		const value = style[key]
		if (value === undefined || value === null || value === '') return ''
		const property = key.indexOf('--') === 0
			? key
			: key.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)
		return `${property}:${value}`
	}).filter(Boolean).join(';')
}

export const themeRuntime = { state, apply, restore, loadPublished, loadPreview, useTemplate, useLocalSkin, setPreviewMode, setPreviewEnvironment, effectiveTheme, setSystemTheme, applySystemTheme, component, backgroundStyle, assetUrl, skinSlot, skinSlotStyle, skinAsset, skinAssets, skinProductImage, skinAssetStyle, typographyToken, skinContent }

export const themeMixin = {
	computed: {
		themeConfig() { return state.config },
			themePageStyle() {
			const style = { ...buildThemeTokenStyle(state.config), ...buildTypographyVariables(state.skinConfig.typography) }
			style.colorScheme = effectiveTheme()
			const page = state.skinConfig && state.skinConfig.page
			if (page && /^#[0-9A-Fa-f]{3,8}$/.test(String(page.backgroundColor || ''))) {
				style['--theme-page'] = page.backgroundColor
				Object.assign(style, buildStructuredBackgroundStyle(page.background, page.backgroundColor))
			}
			return serializeStyle(style)
		},
		themePreviewMode() { return state.preview }
	},
	methods: {
		themeComponent(key) { return component(key) },
		themeBackgroundStyle(key) { return backgroundStyle(key) },
		themeAssetUrl(assetId) { return assetUrl(assetId) },
		themeSkinSlot(key) { return skinSlot(key) },
		themeSkinSlotStyle(key) { return skinSlotStyle(key) },
		themeSkinAsset(key) { return skinAsset(key) },
		themeSkinAssets(key) { return skinAssets(key) },
		themeSkinProductImage(productId) { return skinProductImage(productId) },
		themeSkinAssetStyle(key) { return skinAssetStyle(key) },
		themeTypographyToken(role) { return typographyToken(role) },
		themeSkinContent(key) { return skinContent(key) }
	}
}
