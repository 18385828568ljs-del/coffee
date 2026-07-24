/**
 * 统一错误处理工具
 */

/**
 * 显示错误提示
 * @param {String|Object} error 错误信息或错误对象
 * @param {String} defaultMessage 默认错误消息
 */
export function showError(error, defaultMessage = '操作失败，请稍后重试') {
	let message = defaultMessage

	if (typeof error === 'string') {
		message = error
	} else if (error && error.msg) {
		message = error.msg
	} else if (error && error.message) {
		message = error.message
	} else if (error && error.data && error.data.msg) {
		message = error.data.msg
	}

	uni.showToast({
		title: message,
		icon: 'none',
		duration: 2000
	})
}

/**
 * 显示成功提示
 * @param {String} message 成功消息
 */
export function showSuccess(message = '操作成功') {
	uni.showToast({
		title: message,
		icon: 'success',
		duration: 1500
	})
}

/**
 * 显示加载中
 * @param {String} title 加载提示文字
 */
export function showLoading(title = '加载中...') {
	uni.showLoading({
		title: title,
		mask: true
	})
}

/**
 * 隐藏加载中
 */
export function hideLoading() {
	uni.hideLoading()
}

/**
 * 处理网络请求错误
 * @param {Object} error 错误对象
 * @param {String} defaultMessage 默认错误消息
 */
export function handleRequestError(error, defaultMessage = '网络请求失败') {
	console.error('Request error:', error)

	if (!error) {
		showError(defaultMessage)
		return
	}

	// 网络错误
	if (error.errMsg && error.errMsg.includes('timeout')) {
		showError('请求超时，请检查网络连接')
		return
	}

	if (error.errMsg && error.errMsg.includes('fail')) {
		showError('网络连接失败，请检查网络设置')
		return
	}

	// 业务错误
	if (error.data) {
		if (error.data.code === 401) {
			showError('登录已过期，请重新登录')
			// 可以在这里跳转到登录页
			setTimeout(() => {
				uni.switchTab({
					url: '/pages/me/me'
				})
			}, 1500)
			return
		}

		if (error.data.msg) {
			showError(error.data.msg)
			return
		}
	}

	showError(defaultMessage)
}

/**
 * 确认对话框
 * @param {String} content 提示内容
 * @param {String} title 标题
 * @returns {Promise<Boolean>} 用户是否确认
 */
export function confirm(content, title = '提示') {
	return new Promise((resolve) => {
		uni.showModal({
			title: title,
			content: content,
			success: (res) => {
				resolve(res.confirm)
			},
			fail: () => {
				resolve(false)
			}
		})
	})
}
