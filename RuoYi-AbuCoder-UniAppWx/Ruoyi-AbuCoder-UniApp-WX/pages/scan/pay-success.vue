<template>
	<view class="page">
		<app-nav title="付款成功" />
		<view class="result-card">
			<view class="success-mark">
				<text>✓</text>
			</view>
			<text class="result-title">付款成功</text>
			<text class="result-subtitle">订单已提交，商家会尽快为你制作。</text>
			<text v-if="orderNo" class="order-no">订单号：{{ orderNo }}</text>
			<view class="primary-btn" @tap="backToMenu">
				<text>继续点单</text>
			</view>
		</view>
	</view>
</template>

<script>
import { scanOrderApi } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'

export default {
	data() {
		return {
			orderId: null,
			orderNo: ''
		}
	},

	onLoad(options = {}) {
		this.orderId = options.orderId || null
		if (this.orderId) {
			this.loadOrder()
		}
	},

	methods: {
		authHeader() {
			const token = getToken()
			const header = {}
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},

		async loadOrder() {
			try {
				const res = await requestPromise({
					url: scanOrderApi.detail + this.orderId,
					method: 'GET',
					header: this.authHeader()
				})
				if (isSuccessResponse(res)) {
					const order = (res.data && res.data.data) || {}
					this.orderNo = order.orderNo || ''
				}
			} catch (error) {}
		},

		backToMenu() {
			uni.switchTab({ url: '/pages/scan/menu' })
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
}

.result-card {
	@include card(52rpx 32rpx, $radius-md);
	margin: 80rpx $space-page 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
}

.success-mark {
	width: 112rpx;
	height: 112rpx;
	border-radius: 50%;
	background: $accent-primary;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 18rpx 36rpx rgba(111, 78, 55, 0.2);
}

.success-mark text {
	font-size: 58rpx;
	font-weight: 800;
	color: #FFFFFF;
}

.result-title {
	margin-top: 30rpx;
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 800;
	color: $text-primary;
}

.result-subtitle,
.order-no {
	margin-top: 14rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.5;
	color: $text-secondary;
}

.primary-btn {
	margin-top: 42rpx;
	height: 86rpx;
	min-width: 280rpx;
	padding: 0 36rpx;
	border-radius: $radius-sm;
	background: $accent-primary;
	display: flex;
	align-items: center;
	justify-content: center;
	@include active-press;
}

.primary-btn text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: #FFFFFF;
}
</style>
