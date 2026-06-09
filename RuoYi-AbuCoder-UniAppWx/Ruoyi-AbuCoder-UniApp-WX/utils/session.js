const LOGIN_LANDING_URL = '/pages/me/me'

export function getLocalUserInfo() {
	return uni.getStorageSync('userInfo') || {}
}

export function getLocalUserId() {
	const userInfo = getLocalUserInfo()
	return userInfo.userId || userInfo.id || null
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

export function ensureLocalLogin(message = '请先登录', options = {}) {
	if (getLocalUserId()) {
		return true
	}

	uni.showToast({
		title: message,
		icon: 'none'
	})

	if (options.redirect) {
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
