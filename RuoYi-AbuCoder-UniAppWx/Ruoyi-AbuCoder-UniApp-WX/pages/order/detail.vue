<template>
	<view class="page">
		<app-nav title="订单详情" fallback-url="/pages/me/me" />

		<scroll-view class="content" scroll-y>
			<view class="status-card">
				<text class="status-title">{{ statusText }}</text>
				<text class="status-desc">{{ statusDesc }}</text>
			</view>

			<view class="card">
				<view class="card-header">
					<text class="card-title">收货地址</text>
					<text v-if="orderInfo.receiverName" class="card-tag">订单收货</text>
				</view>
				<view class="address-top">
					<text class="receiver-name">{{ orderInfo.receiverName || '未填写' }}</text>
					<text class="receiver-phone">{{ orderInfo.receiverPhone || '' }}</text>
				</view>
				<text class="address-detail">{{ orderInfo.receiverAddress || '暂无地址信息' }}</text>
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
						<view class="product-name-row">
							<text class="product-name">{{ item.productName }}</text>
							<text v-if="item.giftItem" class="gift-tag">赠品</text>
						</view>
						<text class="product-spec">{{ item.spec || '现烘发货 / 新鲜包装' }}</text>
						<view class="product-meta">
							<text class="product-price">¥{{ formatMoney(item.price) }}</text>
							<text class="product-quantity">x{{ item.quantity }}</text>
						</view>
					</view>
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
				<view class="info-row" v-if="orderInfo.deliveryTime">
					<text class="info-label">发货时间</text>
					<text class="info-value">{{ orderInfo.deliveryTime }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.expressNo">
					<text class="info-label">物流单号</text>
					<view class="info-value-group">
						<text class="info-value">{{ orderInfo.expressNo }}</text>
						<text class="copy-text" @click.stop="copyExpressNo">复制</text>
					</view>
				</view>
				<view class="info-row" v-if="orderInfo.remark">
					<text class="info-label">订单备注</text>
					<text class="info-value">{{ orderInfo.remark }}</text>
				</view>
			</view>

			<view class="card">
				<text class="card-title">价格明细</text>
				<view class="info-row">
					<text class="info-label">商品总价</text>
					<text class="info-value">¥{{ formatMoney(orderInfo.productAmount) }}</text>
				</view>
				<view class="info-row" v-if="orderInfo.freightAmount != null">
					<text class="info-label">运费</text>
					<text class="info-value">¥{{ formatMoney(orderInfo.freightAmount) }}</text>
				</view>
				<view class="info-row" v-if="Number(orderInfo.discountAmount || 0) > 0">
					<text class="info-label">优惠金额</text>
					<text class="info-value info-green">-¥{{ formatMoney(orderInfo.discountAmount) }}</text>
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
					v-if="orderInfo.status === 0"
					class="action-btn"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@click="cancelOrder"
				>
					<text>{{ actionPending === 'cancel' ? '取消中...' : '取消订单' }}</text>
				</view>
				<view
					v-if="orderInfo.status === 0"
					class="action-btn action-btn-primary"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@click="payOrder"
				>
					<text>{{ actionPending === 'pay' ? '支付中...' : '立即支付' }}</text>
				</view>
				<view
					v-if="orderInfo.status === 2"
					class="action-btn action-btn-light"
					:class="{ 'action-btn-disabled': !!actionPending }"
					@click="confirmOrder"
				>
					<text>{{ actionPending === 'confirm' ? '确认中...' : '确认收货' }}</text>
				</view>
			</view>
		</view>

		<result-dialog
			:visible="paySuccessVisible"
			icon="/static/order-status/complete.svg"
			title="支付成功"
			description="订单已完成支付，可在当前页面继续查看发货与物流状态。"
			confirm-text="我知道了"
			@confirm="handlePaySuccessConfirm"
		/>
	</view>
</template>

<script>
import ResultDialog from '@/components/result-dialog.vue'
import { orderApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'

const STATUS_META = {
	0: {
		text: '待付款',
		desc: '订单已创建，支付接口当前保留为联调占位，确认后可继续模拟支付。'
	},
	1: {
		text: '待发货',
		desc: '商家正在安排烘焙与打包，请留意后续发货信息。'
	},
	2: {
		text: '待收货',
		desc: '订单已发出，请留意物流动态并在收货后确认。'
	},
	3: {
		text: '已完成',
		desc: '订单已完成，感谢你的下单与支持。'
	},
	4: {
		text: '已取消',
		desc: '当前订单已取消，如需购买可返回首页重新下单。'
	}
}

export default {
	components: {
		ResultDialog
	},
	data() {
		return {
			orderId: null,
			orderInfo: {
				items: []
			},
			actionPending: '',
			paySuccessVisible: false
		}
	},

	computed: {
		statusText() {
			const meta = STATUS_META[this.orderInfo.status]
			return meta ? meta.text : '未知状态'
		},

		statusDesc() {
			const meta = STATUS_META[this.orderInfo.status]
			return meta ? meta.desc : '请留意订单状态变化。'
		},

		showActionBar() {
			return this.orderInfo.status !== 3 && this.orderInfo.status !== 4
		},

		totalQuantity() {
			return (this.orderInfo.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
		}
	},

	onLoad(options) {
		if (options.id) {
			this.orderId = options.id
			this.loadOrderDetail()
			return
		}
		showError('订单编号缺失')
		setTimeout(() => {
			this.goBack()
		}, 600)
	},

	methods: {
		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		getOrderImage(item) {
			return resolveImageUrl(item.productImg || item.productImage || item.imageUrl)
		},

		async loadOrderDetail(options = {}) {
			const { silent = false } = options
			if (!silent) {
				showBusy('加载中...')
			}

			try {
				const res = await requestPromise({
					url: orderApi.detail + this.orderId,
					method: 'GET'
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.orderInfo = {
						...res.data.data,
						items: res.data.data.items || res.data.data.orderItems || []
					}
					return
				}

				showError((res.data && res.data.msg) || '订单不存在')
				setTimeout(() => {
					this.goBack()
				}, 800)
			} catch (error) {
				showError('加载失败')
			} finally {
				if (!silent) {
					hideBusy()
				}
			}
		},

		async cancelOrder() {
			if (this.actionPending) {
				return
			}
			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要取消该订单吗？'
			})
			if (!confirmed) {
				return
			}
			this.actionPending = 'cancel'
			try {
				const result = await requestPromise({
					url: orderApi.cancel + this.orderId,
					method: 'PUT'
				})
				if (isSuccessResponse(result)) {
					showSuccess('订单已取消')
					this.loadOrderDetail()
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
			if (this.actionPending) {
				return
			}
			const confirmed = await showConfirm({
				title: '确认支付',
				content: `当前订单需支付 ¥${this.formatMoney(this.orderInfo.payAmount || this.orderInfo.totalAmount)}，当前先走业务联调支付接口，确认继续吗？`
			})
			if (!confirmed) {
				return
			}

			this.actionPending = 'pay'
			showBusy('支付中...')
			try {
				const res = await requestPromise({
					url: orderApi.pay + this.orderId,
					method: 'PUT'
				})
				if (isSuccessResponse(res)) {
					await this.loadOrderDetail({ silent: true })
					this.paySuccessVisible = true
					return
				}

				showError((res.data && res.data.msg) || '支付失败')
			} catch (error) {
				showError('支付失败')
			} finally {
				this.actionPending = ''
				hideBusy()
			}
		},

		handlePaySuccessConfirm() {
			this.paySuccessVisible = false
		},

		async confirmOrder() {
			if (this.actionPending) {
				return
			}
			const confirmed = await showConfirm({
				title: '提示',
				content: '确认已收到货物吗？'
			})
			if (!confirmed) {
				return
			}
			this.actionPending = 'confirm'
			try {
				const result = await requestPromise({
					url: orderApi.confirm + this.orderId,
					method: 'PUT'
				})
				if (isSuccessResponse(result)) {
					showSuccess('确认收货成功')
					this.loadOrderDetail()
					return
				}

				showError((result.data && result.data.msg) || '操作失败')
			} catch (error) {
				showError('操作失败')
			} finally {
				this.actionPending = ''
			}
		},

		copyExpressNo() {
			if (!this.orderInfo.expressNo) {
				return
			}
			uni.setClipboardData({
				data: this.orderInfo.expressNo,
				success: () => {
					showSuccess('物流单号已复制')
				}
			})
		},

		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.navigateTo({
				url: '/pages/order/list'
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
	font-size: 32rpx;
	font-weight: 600;
	color: $text-primary;
}

.status-desc {
	display: block;
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
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

.card-tag,
.card-subtitle {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-primary;
}

.address-top {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 12rpx;
}

.receiver-name {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.receiver-phone,
.address-detail {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.address-detail {
	display: block;
	margin-top: 12rpx;
	line-height: 1.6;
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

.product-name-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
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

.gift-tag {
	height: 40rpx;
	padding: 0 12rpx;
	display: flex;
	align-items: center;
	border-radius: $radius-sm;
	background: $accent-surface;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-primary;
	flex-shrink: 0;
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

.info-value-group {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.copy-text {
	height: 40rpx;
	padding: 0 12rpx;
	display: flex;
	align-items: center;
	border-radius: $radius-sm;
	background: $accent-surface;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-primary;
}

.info-green {
	color: $text-primary;
}

.total-row {
	padding-top: 16rpx;
	border-top: 2rpx solid $border-light;
}

.total-label,
.total-value {
	font-weight: 600;
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
}

.bar-actions {
	display: flex;
	justify-content: flex-end;
	gap: 16rpx;
}

.action-btn {
	min-width: 176rpx;
	height: 84rpx;
	padding: 0 30rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $bg-card;
	border: 2rpx solid $border-subtle;
}

.action-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-secondary;
}

.action-btn-primary {
	background: $accent-primary;
	border-color: $accent-primary;
}

.action-btn-primary text {
	color: #FFFFFF;
}

.action-btn-disabled {
	@include disabled-state;
}

.action-btn-light {
	background: $accent-primary-soft;
	border-color: $accent-primary-soft;
}

.action-btn-light text {
	color: $text-primary;
}
</style>
