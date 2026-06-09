function normalizeTitle(title, fallback) {
	return String(title || fallback || '').trim() || fallback
}

export function showBusy(title = '加载中...', mask = true) {
	uni.showLoading({
		title: normalizeTitle(title, '加载中...'),
		mask
	})
}

export function hideBusy() {
	uni.hideLoading()
}

export function showError(title = '操作失败') {
	uni.showToast({
		title: normalizeTitle(title, '操作失败'),
		icon: 'none'
	})
}

export function showSuccess(title = '操作成功') {
	uni.showToast({
		title: normalizeTitle(title, '操作成功'),
		icon: 'success'
	})
}

export function showInfo(title = '请稍后重试') {
	uni.showToast({
		title: normalizeTitle(title, '请稍后重试'),
		icon: 'none'
	})
}

export function showConfirm(options = {}) {
	return new Promise((resolve) => {
		uni.showModal({
			title: options.title || '提示',
			content: options.content || '',
			confirmText: options.confirmText || '确定',
			cancelText: options.cancelText || '取消',
			success: (res) => {
				resolve(!!res.confirm)
			},
			fail: () => {
				resolve(false)
			}
		})
	})
}
