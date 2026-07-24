import { sessionApi } from './session-api.js'
import { getToken } from './auth.js'

const LOGIN_LANDING_URL = '/pages/me/me'

function normalizeAvatar(value) {
	const avatar = String(value || '').trim()
	if (!avatar) {
		return ''
	}
	const normalized = avatar.replace(/\\/g, '/')
	if (/^\/?static\//i.test(normalized)) {
		return ''
	}
	return avatar
}

export function normalizeWxUserInfo(userData = {}, fallback = {}) {
	return {
		userId: userData.userId || userData.id || fallback.userId || fallback.id || '',
		openid: userData.openid || fallback.openid || '',
		nickName: userData.nickName || userData.nickname || fallback.nickName || fallback.nickname || '微信用户',
		avatar: normalizeAvatar(userData.avatar || userData.avatarUrl || fallback.avatar || fallback.avatarUrl),
		phone: userData.phone || fallback.phone || ''
	}
}

export function getLocalUserInfo() {
	return uni.getStorageSync('userInfo') || {}
}

export function getLocalUserId() {
	const userInfo = getLocalUserInfo()
	return userInfo ? (userInfo.userId || userInfo.id || null) : null
}

export function hasCompleteLocalLogin() {
	const userInfo = getLocalUserInfo()
	return !!(userInfo && (userInfo.userId || userInfo.id))
}

export function clearLocalSession(extraKeys = []) {
	const keys = [
		'token',
		'userInfo',
		'userinfo',
		'orderConfirmDraft',
		'selectedOrderAddress',
		...extraKeys
	]

	keys.forEach((key) => {
		if (key) {
			uni.removeStorageSync(key)
		}
	})
}

export function setLocalSession(token, userInfo) {
	if (token) {
		uni.setStorageSync('token', token)
	}
	if (userInfo) {
		uni.setStorageSync('userInfo', normalizeWxUserInfo(userInfo, getLocalUserInfo()))
	}
}

export function restoreLocalSession() {
	const token = getToken()
	if (!token) {
		if (uni.getStorageSync('userInfo') || uni.getStorageSync('userinfo')) {
			clearLocalSession()
		}
		return Promise.resolve(false)
	}

	return new Promise((resolve) => {
		uni.request({
			url: sessionApi.me,
			method: 'GET',
			header: {
				Authorization: `Bearer ${token}`,
				'X-Wx-Token': token
			},
			success: (res) => {
				if (res && res.statusCode >= 200 && res.statusCode < 300 && res.data && res.data.code === 0 && res.data.data) {
					setLocalSession(res.data.token || token, res.data.data)
					resolve(true)
					return
				}
				clearLocalSession()
				resolve(false)
			},
			fail: () => {
				resolve(!!getLocalUserId())
			}
		})
	})
}

export function ensureLocalLogin(message = '请先登录', options = {}) {
	if (hasCompleteLocalLogin()) {
		return true
	}

	uni.showToast({
		title: message,
		icon: 'none'
	})

	const shouldRedirect = options.redirect !== false
	if (shouldRedirect) {
		const redirectUrl = options.redirectUrl || LOGIN_LANDING_URL
		const redirectMode = options.redirectMode || 'switchTab'
		setTimeout(() => {
			if (redirectMode === 'navigateTo') {
				uni.navigateTo({ url: redirectUrl })
				return
			}
			if (redirectMode === 'redirectTo') {
				uni.redirectTo({ url: redirectUrl })
				return
			}
			uni.switchTab({ url: redirectUrl })
		}, options.redirectDelay || 300)
	}

	return false
}
