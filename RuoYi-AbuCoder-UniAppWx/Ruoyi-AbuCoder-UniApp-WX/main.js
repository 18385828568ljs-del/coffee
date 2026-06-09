import Vue from 'vue'
import App from './App'
import request from 'utils/request.js'
import share from '@/utils/share.js'
import uView from '@/uni_modules/uview-ui'
import AppNav from '@/components/app-nav.vue'

Vue.mixin(share)

const LOGIN_LANDING_URL = '/pages/me/me'
const AUTH_REQUIRED_PATTERNS = [
	'/api/cart/',
	'/api/order/',
	'/api/address/',
	'/api/activity/preview',
	'/api/wallet/',
	'/api/member/'
]

let loginRedirectPending = false

function clearWxLoginState(showToast = false, redirectToLogin = false) {
	uni.removeStorageSync('token')
	uni.removeStorageSync('userInfo')
	uni.removeStorageSync('userinfo')
	uni.removeStorageSync('orderConfirmDraft')
	uni.removeStorageSync('selectedOrderAddress')
	if (showToast) {
		uni.showToast({
			title: '登录已失效，请重新登录',
			icon: 'none'
		})
	}
	if (redirectToLogin && !loginRedirectPending) {
		loginRedirectPending = true
		setTimeout(() => {
			uni.switchTab({
				url: LOGIN_LANDING_URL,
				complete: () => {
					loginRedirectPending = false
				}
			})
		}, showToast ? 300 : 0)
	}
}

function sanitizePayload(payload) {
	if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
		return payload
	}
	const nextPayload = {}
	Object.keys(payload).forEach((key) => {
		const value = payload[key]
		if (value !== undefined && value !== null && value !== 'undefined') {
			nextPayload[key] = value
		}
	})
	return nextPayload
}

function isProtectedRequest(url) {
	return AUTH_REQUIRED_PATTERNS.some((pattern) => String(url || '').indexOf(pattern) >= 0)
}

function sanitizeRequestUrl(url, protectedRequest = false) {
	if (!url || url.indexOf('?') === -1) {
		return url
	}
	const [base, queryString] = url.split('?')
	const parts = queryString
		.split('&')
		.filter(Boolean)
		.filter((part) => {
			const [key = '', value = ''] = part.split('=')
			if (protectedRequest && key === 'userId') {
				return false
			}
			const decodedValue = decodeURIComponent(value || '')
			return decodedValue !== 'undefined' && decodedValue !== 'null' && decodedValue !== ''
		})
	return parts.length ? `${base}?${parts.join('&')}` : base
}

function stripProtectedFields(payload, protectedRequest = false) {
	if (!protectedRequest || !payload || typeof payload !== 'object' || Array.isArray(payload)) {
		return payload
	}
	const nextPayload = { ...payload }
	delete nextPayload.userId
	return nextPayload
}

// #ifndef VUE3
uni.addInterceptor('request', {
	invoke(args) {
		const token = uni.getStorageSync('token')
		const protectedRequest = isProtectedRequest(args.url)
		args.url = sanitizeRequestUrl(args.url, protectedRequest)
		args.data = sanitizePayload(stripProtectedFields(args.data, protectedRequest))
		args.header = args.header || {}
		if (protectedRequest && token) {
			args.header.Authorization = `Bearer ${token}`
			args.header['X-Wx-Token'] = token
		}
	},
	success(res) {
		if ((res && res.statusCode === 401) || (res && res.data && res.data.code === 401)) {
			clearWxLoginState(true, true)
		}
	}
})

if (!uni.getStorageSync('token') && (uni.getStorageSync('userInfo') || uni.getStorageSync('userinfo'))) {
	clearWxLoginState()
}

Vue.use(uView)
Vue.component('app-nav', AppNav)
Vue.prototype.request = request.request
Vue.config.productionTip = false
App.mpType = 'app'

const app = new Vue({
	...App
})

const adlist = [
	{
		bannerAd: 'adunit-3187490d80ba92ca',
		gridAd: 'adunit-26da28df208c002c',
		videoAd: 'adunit-e3777ebcaaad92af',
		awardAd: 'adunit-c6b420834b40ddbb',
		plaqueAd: 'adunit-67e3b026600b6582'
	}
]

function myad() {
	return adlist
}

function jump(url) {
	uni.navigateTo({
		url
	})
}

function Godetails(v, url) {
	console.log(JSON.stringify(v))
	uni.navigateTo({
		url: `${url}?params=${JSON.stringify(v)}`
	})
}

function ZH(json) {
	return JSON.parse(json)
}

function mes(str) {
	uni.showToast({
		title: str,
		icon: 'none'
	})
}

Vue.prototype.jump = jump
Vue.prototype.myad = myad
Vue.prototype.mes = mes
Vue.prototype.Godetails = Godetails
Vue.prototype.ZH = ZH

app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
	const app = createSSRApp(App)
	return {
		app
	}
}
// #endif
