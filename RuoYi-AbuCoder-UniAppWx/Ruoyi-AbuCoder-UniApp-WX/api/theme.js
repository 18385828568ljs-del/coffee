import { baseUrl } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'

function parseConfig(payload) {
	if (!payload) return null
	let config = payload.config || payload.configJson
	if (typeof config === 'string') {
		try { config = JSON.parse(config) } catch (error) { return null }
	}
	return config && typeof config === 'object' ? config : null
}

export async function fetchSkin(storeId, previewToken = '') {
	const code = String(storeId || '').trim()
	const token = String(previewToken || '').trim()
	if (token && !/^(?:[A-Za-z0-9_-]{22}|[A-Za-z0-9_-]{43})$/.test(token)) return null
	if (!code && !token) return null
	if (code && !/^[A-Za-z0-9_-]{1,64}$/.test(code)) return null
	const query = `${code ? `storeId=${encodeURIComponent(code)}` : ''}${token ? `${code ? '&' : ''}previewToken=${encodeURIComponent(token)}` : ''}`
	const response = await requestPromise({
		url: `${baseUrl}/api/mini/skin?${query}`,
		method: 'GET'
	})
	if (!isSuccessResponse(response) || !response.data.data) return null
	const payload = response.data.data
	const config = parseConfig(payload)
	if (!config) return null
	return { ...payload, config }
}

export async function fetchPublishedTheme(storeCode) {
	const code = String(storeCode || '').trim()
	if (!/^[A-Za-z0-9_-]{1,64}$/.test(code)) return null
	const response = await requestPromise({
		url: `${baseUrl}/api/wx/stores/${encodeURIComponent(code)}/skin`,
		method: 'GET'
	})
	if (!isSuccessResponse(response) || !response.data.data) return null
	const payload = response.data.data
	return { config: parseConfig(payload), version: payload.version, versionId: payload.versionId, configHash: payload.configHash, assetUrls: payload.assetUrls || {} }
}

export async function fetchPublishedSkinVersion(storeCode) {
	const code = String(storeCode || '').trim()
	if (!/^[A-Za-z0-9_-]{1,64}$/.test(code)) return null
	const response = await requestPromise({ url: `${baseUrl}/api/wx/stores/${encodeURIComponent(code)}/skin/version`, method: 'GET' })
	if (!isSuccessResponse(response) || !response.data.data) return null
	return response.data.data.versionId || response.data.data.version
}
