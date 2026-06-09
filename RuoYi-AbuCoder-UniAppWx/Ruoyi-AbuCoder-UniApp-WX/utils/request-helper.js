export function requestPromise(options = {}) {
	return new Promise((resolve, reject) => {
		uni.request({
			...options,
			success: resolve,
			fail: reject
		})
	})
}

export function isSuccessResponse(res) {
	return !!(res && res.data && (res.data.code === 0 || res.data.code === 200))
}
