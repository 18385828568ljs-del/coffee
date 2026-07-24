import { loginApi } from './apiconfig.js'
import { requestPromise, isSuccessResponse } from './request-helper.js'
import { normalizeWxUserInfo, setLocalSession } from './session.js'

export function wxLoginCode() {
	return new Promise((resolve, reject) => {
		uni.login({
			provider: 'weixin',
			success: (res) => {
				if (res && res.code) {
					resolve(res.code)
					return
				}
				reject(new Error('获取登录凭证失败'))
			},
			fail: () => {
				reject(new Error('获取登录凭证失败'))
			}
		})
	})
}

export async function loginByWxAuth() {
	const code = await wxLoginCode()
	const res = await requestPromise({
		url: loginApi.wxLogin,
		method: 'POST',
		data: { code },
		timeout: 10000
	})

	if (!isSuccessResponse(res)) {
		const message = (res && res.data && res.data.msg) || '微信登录失败'
		throw new Error(message)
	}

	const userInfo = normalizeWxUserInfo(res.data.data)
	setLocalSession(res.data.token, userInfo)
	return userInfo
}
