import { baseUrl } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'

export async function fetchPreviewTheme(previewToken) {
	const token = String(previewToken || '').trim()
	if (!/^(?:[A-Za-z0-9_-]{22}|[A-Za-z0-9_-]{43})$/.test(token)) return null
	const response = await requestPromise({
		url: `${baseUrl}/api/wx/skin/preview?token=${encodeURIComponent(token)}`,
		method: 'GET'
	})
	if (!isSuccessResponse(response) || !response.data.data) return null
	const payload = response.data.data
	let config = payload.configJson
	if (typeof config === 'string') {
		try { config = JSON.parse(config) } catch (error) { return null }
	}
	return { ...payload, config }
}
