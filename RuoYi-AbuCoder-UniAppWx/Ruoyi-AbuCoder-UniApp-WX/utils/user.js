export function getUserInfo() {
	return uni.getStorageSync('userInfo') || {}
}

export function getUserId() {
	const userInfo = getUserInfo()
	return userInfo.userId || userInfo.id || null
}

export function isLoggedIn() {
	return !!getUserId()
}

export function ensureLogin(message = '请先登录') {
	if (isLoggedIn()) {
		return true
	}

	uni.showToast({
		title: message,
		icon: 'none'
	})
	return false
}
