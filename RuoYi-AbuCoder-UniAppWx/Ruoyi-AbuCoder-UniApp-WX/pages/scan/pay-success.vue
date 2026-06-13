<template>
	<view class="page">
		<app-nav title="付款成功" />
		<view class="result-card">
			<view class="success-mark">
				<text>✓</text>
			</view>
			<text class="result-title">付款成功</text>
			<text class="result-subtitle">{{ statusDesc }}</text>
			<view v-if="pickupNo || estimatedWaitMinutes" class="pickup-panel">
				<view v-if="pickupNo" class="pickup-item">
					<text class="pickup-label">取餐号</text>
					<text class="pickup-value">{{ pickupNo }}</text>
				</view>
				<view v-if="estimatedWaitMinutes" class="pickup-item">
					<text class="pickup-label">预计等待</text>
					<text class="pickup-value">{{ estimatedWaitMinutes }}分钟</text>
				</view>
			</view>
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
			orderNo: '',
			orderStatus: null,
			pickupNo: '',
			estimatedWaitMinutes: null,
			pollTimer: null,
			pickupNotified: false
		}
	},

	computed: {
		statusDesc() {
			if (this.orderStatus === 2) return '订单已支付，正在为你制作。'
			if (this.orderStatus === 3) return '已叫号，请凭取餐号取餐。'
			if (this.orderStatus === 4) return '订单已完成，感谢你的支持。'
			return '订单已提交，商家会尽快处理。'
		}
	},

	onLoad(options = {}) {
		this.orderId = options.orderId || null
		if (this.orderId) {
			this.loadOrder()
			this.startPolling()
		}
	},

	onUnload() {
		this.stopPolling()
	},

	onHide() {
		this.stopPolling()
	},

	onShow() {
		if (this.orderId) {
			this.startPolling()
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

		async loadOrder(options = {}) {
			const previousStatus = this.orderStatus
			try {
				const res = await requestPromise({
					url: scanOrderApi.detail + this.orderId,
					method: 'GET',
					header: this.authHeader()
				})
				if (isSuccessResponse(res)) {
					const order = (res.data && res.data.data) || {}
					this.orderNo = order.orderNo || ''
					this.orderStatus = Number(order.orderStatus === undefined ? order.status : order.orderStatus)
					this.pickupNo = order.pickupNo || ''
					this.estimatedWaitMinutes = order.estimatedWaitMinutes || null
					this.notifyPickupReady(previousStatus)
				}
			} catch (error) {}
		},

		startPolling() {
			this.stopPolling()
			this.pollTimer = setInterval(() => {
				this.loadOrder({ silent: true })
			}, 5000)
		},

		stopPolling() {
			if (this.pollTimer) {
				clearInterval(this.pollTimer)
				this.pollTimer = null
			}
		},

		notifyPickupReady(previousStatus) {
			if (this.pickupNotified || this.orderStatus !== 3 || previousStatus === 3) {
				return
			}
			this.pickupNotified = true
			uni.showModal({
				title: '\u53d6\u9910\u63d0\u9192',
				content: `\u60a8\u7684\u9910\u54c1\u5df2\u5b8c\u6210\uff0c\u8bf7\u51ed\u53d6\u9910\u53f7 ${this.pickupNo || '-'} \u53d6\u9910\u3002`,
				showCancel: false,
				confirmText: '\u77e5\u9053\u4e86'
			})
		},

		backToMenu() {
			this.stopPolling()
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

.pickup-panel {
	margin-top: 28rpx;
	width: 100%;
	display: flex;
	gap: 18rpx;
}

.pickup-item {
	flex: 1;
	padding: 22rpx 16rpx;
	border-radius: $radius-sm;
	background: $accent-surface;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 8rpx;
}

.pickup-label {
	font-family: $font-family;
	font-size: 22rpx;
	color: $text-secondary;
}

.pickup-value {
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 800;
	color: $accent-primary;
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
