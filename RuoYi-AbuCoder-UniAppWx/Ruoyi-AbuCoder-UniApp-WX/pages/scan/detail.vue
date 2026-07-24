<template>
	<view class="page">
		<app-nav title="点单详情" fallback-url="/pages/order/list?type=scan" />

		<scroll-view class="content" scroll-y>
			<view class="status-card">
				<text class="status-title">{{ statusText }}</text>
				<text class="status-desc">{{ statusDesc }}</text>
				<view class="progress-line" v-if="showProgressLine">
					<view
						v-for="(step, index) in progressSteps"
						:key="index"
						class="progress-step"
						:class="{ active: isStepActive(step.minStatus), current: isStepCurrent(step.currentStatus) }"
					>
						<view class="step-dot"></view>
						<text>{{ step.label }}</text>
					</view>
				</view>
			</view>

			<view class="pickup-card" v-if="showPickupSummary">
				<view class="pickup-item">
					<text class="pickup-label">取餐号</text>
					<text class="pickup-value">{{ orderInfo.pickupNo || '-' }}</text>
				</view>
				<view class="pickup-item">
					<text class="pickup-label">预计等待</text>
					<text class="pickup-value">{{ waitText }}</text>
				</view>
			</view>

			<view class="card">
				<view class="card-header">
					<text class="card-title">商品信息</text>
					<text class="card-subtitle">共 {{ totalQuantity }} 件</text>
				</view>
				<view
					v-for="(item, index) in orderInfo.items"
					:key="index"
					class="product-row"
				>
					<image
						v-if="getOrderImage(item)"
						class="product-image"
						:src="getOrderImage(item)"
						mode="aspectFill"
					></image>
					<view v-else class="product-image product-image-empty"></view>
					<view class="product-info">
						<text class="product-name">{{ item.productName }}</text>
						<text class="product-spec">{{ item.spec || '默认规格' }}</text>
						<view class="product-meta">
							<text class="product-price">¥{{ formatMoney(item.price) }}</text>
							<text class="product-quantity">x{{ item.quantity }}</text>
						</view>
					</view>
				</view>
			</view>

			<view class="card">
				<text class="card-title">取餐信息</text>
				<view class="info-row">
					<text class="info-label">取餐号</text>
					<text class="info-value">{{ orderInfo.pickupNo || '-' }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.tableNo">
					<text class="info-label">桌号</text>
					<text class="info-value">{{ orderInfo.tableNo }}</text>
				</view>
			</view>

			<view class="card">
				<text class="card-title">订单信息</text>
				<view class="info-row">
					<text class="info-label">订单编号</text>
					<text class="info-value">{{ orderInfo.orderNo || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">下单时间</text>
					<text class="info-value">{{ orderInfo.createTime || '--' }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.payTime">
					<text class="info-label">支付时间</text>
					<text class="info-value">{{ orderInfo.payTime }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.makingTime">
					<text class="info-label">开始制作</text>
					<text class="info-value">{{ orderInfo.makingTime }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.callTime">
					<text class="info-label">叫号时间</text>
					<text class="info-value">{{ orderInfo.callTime }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.finishTime">
					<text class="info-label">完成时间</text>
					<text class="info-value">{{ orderInfo.finishTime }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.cancelTime">
					<text class="info-label">取消时间</text>
					<text class="info-value">{{ orderInfo.cancelTime }}</text>
				</view>
				<view class="info-row" v-if="hasUrged">
					<text class="info-label">催单</text>
					<text class="info-value">已催 {{ orderInfo.urgeCount }} 次</text>
				</view>
				<view class="info-row" v-if="refundStatus > 0">
					<text class="info-label">退款状态</text>
					<text class="info-value" :class="refundStatusClass">{{ refundStatusText }}</text>
				</view>
				<view class="info-row" v-if="refundStatus > 0 && orderInfo.refundReason">
					<text class="info-label">退款原因</text>
					<text class="info-value">{{ orderInfo.refundReason }}</text>
				</view>
				<view class="info-row" v-if="refundStatus === 3 && orderInfo.refundRejectReason">
					<text class="info-label">拒绝原因</text>
					<text class="info-value warn-text">{{ orderInfo.refundRejectReason }}</text>
				</view>
				<view class="info-row total-row">
					<text class="info-label total-label">实付款</text>
					<text class="info-value total-value">¥{{ formatMoney(orderInfo.payAmount || orderInfo.totalAmount) }}</text>
				</view>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view class="bottom-bar" v-if="showActionBar">
			<view class="bar-actions">
				<view
					v-if="orderStatus === 0"
					class="action-btn"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@tap="cancelOrder"
				>
					<text>{{ actionPending === 'cancel' ? '取消中...' : '取消订单' }}</text>
				</view>
				<view
					v-if="orderStatus === 0"
					class="action-btn action-btn-primary"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@tap="payOrder"
				>
					<text>{{ actionPending === 'pay' ? '支付中...' : '立即支付' }}</text>
				</view>
				<view
					v-if="canUrge"
					class="action-btn action-btn-primary"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@tap="urgeOrder"
				>
					<text>{{ actionPending === 'urge' ? '催单中...' : '催单' }}</text>
				</view>
				<view
					v-if="canApplyRefund"
					class="action-btn action-btn-warn"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@tap="showRefundDialog"
				>
					<text>{{ actionPending === 'refund' ? '申请中...' : '申请退款' }}</text>
				</view>
			</view>
		</view>

		<refund-dialog
			:visible="refundDialogVisible"
			:amount="orderInfo.payAmount || orderInfo.totalAmount"
			:reasons="refundReasons"
			:selected-reason="refundReason"
			:pending="actionPending === 'refund'"
			@close="refundDialogVisible = false"
			@reason-change="refundReason = $event"
			@confirm="confirmRefund"
		/>
	</view>
</template>

<script>
import RefundDialog from '@/components/refund-dialog.vue'
import { scanOrderApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'

const STATUS_META = {
	0: { text: '待付款', desc: '订单已创建，支付后商家会开始制作。' },
	2: { text: '制作中', desc: '订单已支付，正在为你制作。' },
	3: { text: '待取餐', desc: '已叫号，请凭取餐号取餐。' },
	4: { text: '已完成', desc: '订单已完成，感谢你的支持。' },
	5: { text: '已取消', desc: '订单已取消，如需购买可重新点单。' }
}

const PROGRESS_STEPS = [
	{ minStatus: 2, currentStatus: 2, label: '制作中' },
	{ minStatus: 3, currentStatus: 3, label: '请取餐' },
	{ minStatus: 4, currentStatus: 4, label: '已完成' }
]

export default {
	components: {
		RefundDialog
	},
	data() {
		return {
			orderId: null,
			orderInfo: {
				items: []
			},
			actionPending: '',
			pollTimer: null,
			pickupNotified: false,
			refundDialogVisible: false,
			refundReason: '不想要了',
			refundReasons: [
				'不想要了',
				'下错单了',
				'等待时间太长',
				'其他原因'
			]
		}
	},

	computed: {
		orderStatus() {
			const raw = this.orderInfo.orderStatus === undefined ? this.orderInfo.status : this.orderInfo.orderStatus
			return Number(raw)
		},

		statusText() {
			const refundMeta = this.refundStatusMeta
			if (refundMeta && refundMeta.overridesOrderStatus) {
				return refundMeta.text
			}
			const meta = STATUS_META[this.orderStatus]
			return meta ? meta.text : '未知状态'
		},

		statusDesc() {
			const refundMeta = this.refundStatusMeta
			if (refundMeta && refundMeta.overridesOrderStatus) {
				return refundMeta.desc
			}
			const meta = STATUS_META[this.orderStatus]
			return meta ? meta.desc : '请留意订单状态变化。'
		},

		progressSteps() {
			return PROGRESS_STEPS
		},

		waitText() {
			return this.orderInfo.estimatedWaitMinutes ? `${this.orderInfo.estimatedWaitMinutes}分钟` : '-'
		},

		totalQuantity() {
			return (this.orderInfo.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
		},

		canUrge() {
			return (this.orderStatus === 2 || this.orderStatus === 3)
				&& !this.refundBlocksOrderActions
		},

		hasUrged() {
			return Number(this.orderInfo.urgeCount || 0) > 0
		},

		showActionBar() {
			return !this.refundBlocksOrderActions
				&& (this.orderStatus === 0 || this.canUrge || this.canApplyRefund)
		},

		refundStatus() {
			return Number(this.orderInfo.refundStatus || 0)
		},

		refundStatusText() {
			const map = {
				0: '未退款',
				1: '退款中',
				2: '已退款',
				3: '退款失败'
			}
			return map[this.refundStatus] || '未知'
		},

		refundStatusClass() {
			const map = {
				1: 'status-pending',
				2: 'status-success',
				3: 'status-error'
			}
			return map[this.refundStatus] || ''
		},

		refundStatusMeta() {
			const map = {
				1: {
					text: '退款处理中',
					desc: '退款申请已提交，商家正在处理，请留意退款结果。',
					overridesOrderStatus: true
				},
				2: {
					text: '已退款',
					desc: '退款已完成，金额已按原支付方式退回。',
					overridesOrderStatus: true
				},
				3: {
					text: '退款未通过',
					desc: this.orderInfo.refundRejectReason || '退款申请未通过，可根据订单状态重新发起申请。',
					overridesOrderStatus: false
				}
			}
			return map[this.refundStatus] || null
		},

		refundBlocksOrderActions() {
			return this.refundStatus === 1 || this.refundStatus === 2
		},

		showProgressLine() {
			return !this.refundBlocksOrderActions
		},

		showPickupSummary() {
			return !this.refundBlocksOrderActions
		},

		canApplyRefund() {
			return (this.orderStatus >= 2 && this.orderStatus <= 4)
				&& (this.refundStatus === 0 || this.refundStatus === 3)
		}
	},

	onLoad(options = {}) {
		this.orderId = options.orderId || options.id || null
		if (this.orderId) {
			this.loadOrderDetail()
			this.startPolling()
			return
		}
		showError('订单编号缺失')
		setTimeout(() => {
			this.goBack()
		}, 600)
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

		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		getOrderImage(item) {
			return resolveImageUrl(item.productImg || item.productImage || item.imageUrl)
		},

		isStepActive(stepValue) {
			if (this.orderStatus === 5) return false
			return this.orderStatus >= stepValue
		},

		isStepCurrent(stepStatus) {
			if (stepStatus == null) return false
			return this.orderStatus === stepStatus
		},

		async loadOrderDetail(options = {}) {
			const { silent = false } = options
			const previousStatus = this.orderStatus
			if (!silent) {
				showBusy('加载中...')
			}
			try {
				const res = await requestPromise({
					url: scanOrderApi.detail + this.orderId,
					method: 'GET',
					header: this.authHeader()
				})
				if (isSuccessResponse(res) && res.data.data) {
					const order = res.data.data
					this.orderInfo = {
						...order,
						items: order.items || order.orderItems || []
					}
					// 只有在状态发生变化时才通知（从非3状态变为3状态）
					if (previousStatus !== undefined && previousStatus !== 3 && this.orderStatus === 3) {
						this.notifyPickupReady()
					}
					return
				}
				showError((res.data && res.data.msg) || '订单不存在')
				setTimeout(() => {
					this.goBack()
				}, 800)
			} catch (error) {
				showError('鍔犺浇澶辫触')
			} finally {
				if (!silent) {
					hideBusy()
				}
			}
		},

		startPolling() {
			this.stopPolling()
			this.pollTimer = setInterval(() => {
				this.loadOrderDetail({ silent: true })
			}, 5000)
		},

		stopPolling() {
			if (this.pollTimer) {
				clearInterval(this.pollTimer)
				this.pollTimer = null
			}
		},

		notifyPickupReady() {
			if (this.pickupNotified) {
				return
			}
			this.pickupNotified = true
			uni.showModal({
				title: '\u53d6\u9910\u63d0\u9192',
				content: `\u60a8\u7684\u9910\u54c1\u5df2\u5b8c\u6210\uff0c\u8bf7\u51ed\u53d6\u9910\u53f7 ${this.orderInfo.pickupNo || '-'} \u53d6\u9910\u3002`,
				showCancel: false,
				confirmText: '\u77e5\u9053\u4e86'
			})
		},

		async cancelOrder() {
			if (this.actionPending) return
			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要取消该订单吗？'
			})
			if (!confirmed) return

			this.actionPending = 'cancel'
			try {
				const result = await requestPromise({
					url: scanOrderApi.cancel + this.orderId,
					method: 'PUT',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('订单已取消')
					this.loadOrderDetail({ silent: true })
					return
				}
				showError((result.data && result.data.msg) || '取消失败')
			} catch (error) {
				showError('取消失败')
			} finally {
				this.actionPending = ''
			}
		},

		async payOrder() {
			if (this.actionPending) return
			const payType = 'balance'
			const confirmed = await showConfirm({
				title: '确认支付',
				content: `当前订单需支付 ¥${this.formatMoney(this.orderInfo.payAmount || this.orderInfo.totalAmount)}，确认使用余额支付吗？`
			})
			if (!confirmed) return

			this.actionPending = 'pay'
			try {
				const result = await requestPromise({
					url: scanOrderApi.pay + this.orderId + '?payType=' + payType,
					method: 'PUT',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('支付成功')
					this.loadOrderDetail({ silent: true })
					return
				}
				showError((result.data && result.data.msg) || '支付失败')
			} catch (error) {
				showError('支付失败')
			} finally {
				this.actionPending = ''
			}
		},

		async urgeOrder() {
			if (this.actionPending) return
			this.actionPending = 'urge'
			try {
				const result = await requestPromise({
					url: scanOrderApi.urge + this.orderId,
					method: 'POST',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('已催单')
					this.loadOrderDetail({ silent: true })
					return
				}
				showError((result.data && result.data.msg) || '催单失败')
			} catch (error) {
				showError('催单失败')
			} finally {
				this.actionPending = ''
			}
		},

		showRefundDialog() {
			if (this.actionPending) return
			this.refundDialogVisible = true
		},

		async confirmRefund(reason) {
			if (this.actionPending) return

			this.refundReason = reason || this.refundReason
			if (!this.refundReason || this.refundReason.trim() === '') {
				showError('请选择退款原因')
				return
			}

			this.actionPending = 'refund'
			showBusy('提交中...')

			try {
				const res = await requestPromise({
					url: scanOrderApi.refundApply,
					method: 'POST',
					data: {
						orderId: this.orderId,
						refundReason: this.refundReason
					},
					header: this.authHeader()
				})

				hideBusy()

				if (isSuccessResponse(res)) {
					this.refundDialogVisible = false
					showSuccess('退款申请已提交')

					setTimeout(() => {
						this.loadOrderDetail({ silent: true })
					}, 1500)
				} else {
					showError(res.data?.msg || '退款申请失败')
				}
			} catch (error) {
				hideBusy()
				showError(error.message || '退款申请失败')
			} finally {
				this.actionPending = ''
			}
		},

		goBack() {
			this.stopPolling()
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.navigateTo({
				url: '/pages/order/list?type=scan'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
	padding-bottom: calc(128rpx + env(safe-area-inset-bottom));
}

.content {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 24rpx;
	box-sizing: border-box;
}

.status-card {
	@include hero-card(24rpx);
	margin-bottom: 20rpx;
}

.status-title {
	display: block;
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 700;
	color: $text-primary;
}

.status-desc {
	display: block;
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 23rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.progress-line {
	margin-top: 26rpx;
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 10rpx;
}

.progress-step {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10rpx;
	position: relative;
}

.progress-step::before {
	content: '';
	position: absolute;
	top: 11rpx;
	left: -50%;
	width: 100%;
	height: 4rpx;
	background: $border-subtle;
}

.progress-step:first-child::before {
	display: none;
}

.progress-step.active::before {
	background: $accent-primary;
}

.step-dot {
	width: 26rpx;
	height: 26rpx;
	border-radius: 50%;
	background: $bg-card;
	border: 4rpx solid $border-strong;
	box-sizing: border-box;
	z-index: 1;
}

.progress-step.active .step-dot {
	background: $accent-primary;
	border-color: $accent-primary;
	box-shadow: 0 0 0 8rpx rgba(122, 79, 45, 0.12);
}

.progress-step.current .step-dot {
	background: $accent-warm;
	border-color: $accent-warm;
	box-shadow: 0 0 0 10rpx rgba(183, 121, 69, 0.16);
}

.progress-step text {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-tertiary;
}

.progress-step.active text {
	color: $accent-primary;
}

.progress-step.current text {
	color: $text-primary;
	font-weight: 700;
}

.pickup-card {
	display: flex;
	gap: 18rpx;
	margin-bottom: 20rpx;
}

.pickup-item {
	@include card(24rpx);
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

.pickup-label {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.pickup-value {
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 800;
	color: $text-primary;
}

.card {
	@include card(24rpx);
	margin-bottom: 20rpx;
}

.card-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	margin-bottom: 16rpx;
}

.card-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.card-subtitle {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-secondary;
}

.product-row {
	display: flex;
	gap: 18rpx;
}

.product-row + .product-row {
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 2rpx solid $border-light;
}

.product-image {
	width: 132rpx;
	height: 132rpx;
	flex-shrink: 0;
	border-radius: $radius-sm;
	background: $accent-surface;
}

.product-image-empty {
	background: $bg-muted;
}

.product-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.product-name {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 1.4;
	color: $text-primary;
}

.product-spec,
.product-quantity {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.product-meta {
	margin-top: auto;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.product-price {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.info-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
	margin-top: 16rpx;
}

.info-label,
.info-value {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
}

.info-label {
	color: $text-secondary;
	flex-shrink: 0;
}

.info-value {
	color: $text-primary;
	text-align: right;
	word-break: break-all;
}

.total-row {
	padding-top: 16rpx;
	border-top: 2rpx solid $border-light;
}

.total-label,
.total-value {
	font-weight: 700;
}

.bottom-space {
	height: 120rpx;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 18rpx $space-page calc(28rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	box-sizing: border-box;
	z-index: 20;
}

.bar-actions {
	display: flex;
	gap: 16rpx;
}

.action-btn {
	flex: 1;
	height: 84rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	border: 2rpx solid $border-subtle;
	background: $bg-card;
}

.action-btn text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: $text-secondary;
}

.action-btn-primary {
	background: $accent-primary;
	border-color: $accent-primary;
}

.action-btn-primary text {
	color: #FFFFFF;
}

.action-btn-warn {
	background: $accent-surface;
	border-color: $accent-primary-soft;
}

.action-btn-warn text {
	color: $accent-primary;
}

.action-btn-disabled {
	opacity: 0.58;
}

/* 退款状态样式 */
.status-pending {
	color: $accent-warm;
}

.status-success {
	color: $accent-success;
}

.status-error,
.warn-text {
	color: $accent-danger;
}
</style>
