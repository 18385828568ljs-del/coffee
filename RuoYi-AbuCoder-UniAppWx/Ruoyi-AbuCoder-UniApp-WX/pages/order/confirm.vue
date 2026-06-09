<template>
	<view class="page">
		<app-nav title="确认订单" fallback-url="/pages/cart/cart" />

		<scroll-view v-if="draftItems.length > 0" class="content" scroll-y>
			<view class="card address-card" @click="chooseAddress">
				<view class="card-header">
					<text class="card-title">收货地址</text>
					<text class="card-link">{{ currentAddress ? '切换地址' : '选择地址' }}</text>
				</view>
				<view v-if="currentAddress">
					<view class="address-top">
						<view class="address-user">
							<text class="address-name">{{ currentAddress.receiverName }}</text>
							<text class="address-phone">{{ currentAddress.receiverPhone }}</text>
						</view>
						<text v-if="currentAddress.isDefault === 1" class="address-tag">默认</text>
					</view>
					<text class="address-detail">{{ fullAddress }}</text>
				</view>
				<view v-else class="address-empty">
					<text class="address-empty-title">请选择收货地址</text>
					<text class="address-empty-desc">用于确认收货人与配送信息</text>
				</view>
			</view>

			<view class="card">
				<view class="card-header">
					<text class="card-title">商品清单</text>
					<text class="card-subtitle">共 {{ totalQuantity }} 件</text>
				</view>
				<view
					v-for="(item, index) in draftItems"
					:key="item.productId + '-' + index"
					class="product-row"
				>
					<image
						v-if="getItemImage(item)"
						class="product-image"
						:src="getItemImage(item)"
						mode="aspectFill"
					></image>
					<view v-else class="product-image product-image-empty"></view>
					<view class="product-info">
						<text class="product-name">{{ item.productName }}</text>
						<text class="product-spec">{{ item.spec || '现烘发货 / 新鲜包装' }}</text>
						<view class="product-meta">
							<text class="product-price">¥{{ formatMoney(item.salePrice || item.price) }}</text>
							<text class="product-quantity">x{{ item.quantity }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="previewInfo.giftItems && previewInfo.giftItems.length > 0" class="card">
				<view class="card-header">
					<text class="card-title">活动赠品</text>
					<text class="card-subtitle">随单附送</text>
				</view>
				<view
					v-for="(item, index) in previewInfo.giftItems"
					:key="'gift-' + index"
					class="product-row"
				>
					<image
						v-if="getItemImage(item)"
						class="product-image"
						:src="getItemImage(item)"
						mode="aspectFill"
					></image>
					<view v-else class="product-image product-image-empty"></view>
					<view class="product-info">
						<view class="gift-name-row">
							<text class="product-name">{{ item.productName }}</text>
							<text class="gift-tag">赠品</text>
						</view>
						<text class="product-spec">{{ item.spec || '活动附送' }}</text>
						<view class="product-meta">
							<text class="product-price summary-green">¥0.00</text>
							<text class="product-quantity">x{{ item.quantity }}</text>
						</view>
					</view>
				</view>
			</view>

			<view class="card">
				<text class="card-title">金额明细</text>
				<view class="summary-row">
					<text class="summary-label">商品金额</text>
					<text class="summary-value">¥{{ formatMoney(previewInfo.totalAmount || subtotal) }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">运费</text>
					<text class="summary-value">¥{{ formatMoney(previewInfo.freightAmount) }}</text>
				</view>
				<view class="summary-row">
					<text class="summary-label">活动优惠</text>
					<text class="summary-value summary-green">-¥{{ formatMoney(previewInfo.discountAmount) }}</text>
				</view>
				<view v-if="Number(previewInfo.memberDiscount || 0) > 0" class="summary-row">
					<text class="summary-label">会员折扣</text>
					<text class="summary-value summary-green">-¥{{ formatMoney(previewInfo.memberDiscount) }}</text>
				</view>
				<text v-if="previewInfo.activitySummary" class="summary-note">{{ previewInfo.activitySummary }}</text>
			</view>

			<!-- 支付方式选择 -->
			<view class="card">
				<text class="card-title">支付方式</text>
				<view class="pay-method-list">
					<view class="pay-method-row" @click="payType = 'balance'">
						<view class="pay-method-info">
							<text class="pay-method-name">余额支付</text>
							<text class="pay-method-desc">可用余额 ¥{{ formatMoney(walletBalance) }}</text>
						</view>
						<view class="pay-radio" :class="{ 'pay-radio-active': payType === 'balance' }"></view>
					</view>
					<view class="pay-method-row" @click="payType = 'wechat'">
						<view class="pay-method-info">
							<text class="pay-method-name">微信支付</text>
							<text class="pay-method-desc">联调占位</text>
						</view>
						<view class="pay-radio" :class="{ 'pay-radio-active': payType === 'wechat' }"></view>
					</view>
				</view>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view v-else class="empty-state">
			<text class="empty-title">暂无待确认商品</text>
			<text class="empty-desc">请先从商品详情页或购物车发起下单。</text>
			<view class="empty-btn" @click="goShopping">
				<text>去逛逛</text>
			</view>
		</view>

		<view class="bottom-bar">
			<view class="bottom-info">
				<text class="bottom-label">实付款</text>
				<text class="bottom-price">¥{{ payAmount }}</text>
			</view>
			<view class="submit-btn" :class="{ disabled: submitting || !canSubmit }" @click="submitOrder">
				<text>{{ submitting ? '提交中...' : '提交订单' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import { activityApi, addressApi, orderApi, walletApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError, showSuccess } from '@/utils/ui-feedback.js'

const ORDER_DRAFT_KEY = 'orderConfirmDraft'
const SELECTED_ADDRESS_KEY = 'selectedOrderAddress'

function buildAddressText(address) {
	if (!address) {
		return ''
	}
	return `${address.province || ''}${address.city || ''}${address.district || ''}${address.detailAddress || ''}`
}

export default {
	data() {
		return {
			draftItems: [],
			cartIds: [],
			currentAddress: null,
			previewInfo: {
				totalAmount: 0,
				payAmount: 0,
				discountAmount: 0,
				memberDiscount: 0,
				freightAmount: 0,
				activitySummary: '',
				giftItems: []
			},
			walletBalance: 0,
			payType: 'balance',
			submitting: false
		}
	},

	computed: {
		fullAddress() {
			return buildAddressText(this.currentAddress)
		},

		totalQuantity() {
			return this.draftItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
		},

		subtotal() {
			return this.draftItems.reduce((sum, item) => {
				const price = Number(item.salePrice || item.price || 0)
				return sum + price * Number(item.quantity || 0)
			}, 0)
		},

		payAmount() {
			const value = this.previewInfo.payAmount || (this.subtotal + Number(this.previewInfo.freightAmount || 0))
			return this.formatMoney(value)
		},

		canSubmit() {
			return !!(this.currentAddress && this.draftItems.length > 0)
		}
	},

	onLoad() {
		if (!ensureLocalLogin()) {
			setTimeout(() => {
				this.goBack()
			}, 300)
			return
		}
		this.loadDraft()
		this.loadWalletBalance()
	},

	onShow() {
		this.consumeSelectedAddress()
	},

	methods: {
		loadDraft() {
			const draft = uni.getStorageSync(ORDER_DRAFT_KEY) || {}
			this.draftItems = draft.items || []
			this.cartIds = draft.cartIds || []
			this.currentAddress = draft.address || null

			if (this.draftItems.length === 0) {
				return
			}

			if (!this.currentAddress) {
				this.loadDefaultAddress()
			}
			this.loadPreview()
		},

		consumeSelectedAddress() {
			const selectedAddress = uni.getStorageSync(SELECTED_ADDRESS_KEY)
			if (!selectedAddress || !selectedAddress.addressId) {
				return
			}
			this.currentAddress = selectedAddress
			uni.removeStorageSync(SELECTED_ADDRESS_KEY)
			this.persistDraft()
		},

		async loadDefaultAddress() {
			try {
				const res = await requestPromise({
					url: addressApi.default,
					method: 'GET'
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.currentAddress = res.data.data
					this.persistDraft()
				}
			} catch (error) {
				// noop
			}
		},

		async loadPreview() {
			if (this.draftItems.length === 0) {
				return
			}

			try {
				const res = await requestPromise({
					url: activityApi.preview,
					method: 'POST',
					data: {
						orderItems: this.draftItems.map((item) => ({
							productId: item.productId,
							quantity: item.quantity,
							spec: item.spec || ''
						}))
					}
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.previewInfo = {
						...this.previewInfo,
						...res.data.data
					}
				}
			} catch (error) {
				// noop
			}
		},

		async loadWalletBalance() {
			try {
				const res = await requestPromise({
					url: walletApi.info,
					method: 'GET'
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.walletBalance = Number(res.data.data.balance || 0)
					// 如果有后端返回的钱包余额也放入 previewInfo
					if (res.data.data.balance != null) {
						this.previewInfo.walletBalance = this.walletBalance
					}
				}
			} catch (error) {
				console.warn('[confirm] 加载钱包余额失败', error)
			}
		},

		persistDraft() {
			uni.setStorageSync(ORDER_DRAFT_KEY, {
				items: this.draftItems,
				cartIds: this.cartIds,
				address: this.currentAddress
			})
		},

		chooseAddress() {
			uni.navigateTo({
				url: `/pages/address/list?from=order&currentAddressId=${this.currentAddress ? this.currentAddress.addressId : ''}`
			})
		},

		getItemImage(item) {
			return resolveImageUrl(item.productImg || item.productImage || item.imageUrl)
		},

		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		submitOrder() {
			if (this.submitting || !this.canSubmit) {
				if (!this.currentAddress) {
					showError('请先选择收货地址')
				}
				return
			}

			this.submitting = true
			this.persistDraft()
			showBusy('提交订单中')

			// 余额支付时检查余额是否充足
			const orderPayAmount = Number(this.payAmount)
			if (this.payType === 'balance' && this.walletBalance < orderPayAmount) {
				showError('余额不足，请先充值或切换支付方式')
				this.submitting = false
				hideBusy()
				return
			}

			requestPromise({
				url: orderApi.create,
				method: 'POST',
				data: {
					receiverName: this.currentAddress.receiverName,
					receiverPhone: this.currentAddress.receiverPhone,
					receiverAddress: this.fullAddress,
					cartIds: this.cartIds,
					payType: this.payType,
					orderItems: this.draftItems.map((item) => ({
						productId: item.productId,
						quantity: item.quantity,
						spec: item.spec || ''
					}))
				}
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						const orderId = res.data.data
						uni.removeStorageSync(ORDER_DRAFT_KEY)
						uni.removeStorageSync(SELECTED_ADDRESS_KEY)
						showSuccess('订单创建成功')
						setTimeout(() => {
							uni.redirectTo({
								url: `/pages/order/detail?id=${orderId}`
							})
						}, 500)
						return
					}

					showError((res.data && res.data.msg) || '提交失败')
				})
				.catch(() => {
					showError('提交失败')
				})
				.finally(() => {
					this.submitting = false
					hideBusy()
				})
		},

		goShopping() {
			uni.switchTab({
				url: '/pages/index/index'
			})
		},

		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/cart/cart'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
	padding-bottom: calc(156rpx + env(safe-area-inset-bottom));
}

.content {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 24rpx;
	box-sizing: border-box;
}

.card {
	@include card(24rpx);
	margin-bottom: 24rpx;
}

.card-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 18rpx;
}

.card-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.card-link,
.card-subtitle {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-primary;
}

.address-top {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
}

.address-user {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 14rpx;
}

.address-name {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.address-phone {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.address-tag {
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

.address-detail,
.address-empty-desc {
	display: block;
	margin-top: 14rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.address-empty-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
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
	width: 144rpx;
	height: 144rpx;
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

.gift-name-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.product-name {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	line-height: 1.4;
	color: $text-primary;
}

.product-spec,
.product-quantity {
	font-family: $font-family;
	font-size: 22rpx;
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
	font-size: 26rpx;
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

.summary-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 16rpx;
}

.summary-label,
.summary-value {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	color: $text-secondary;
}

.summary-value {
	color: $text-primary;
}

.summary-green {
	color: $text-primary;
	font-weight: 600;
}

.summary-note {
	display: block;
	margin-top: 18rpx;
	padding: 16rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.empty-state {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 180rpx 40rpx 0;
}

.empty-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.empty-desc {
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
	text-align: center;
}

.empty-btn {
	margin-top: 28rpx;
	height: 76rpx;
	padding: 0 36rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
}

.empty-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

.bottom-space {
	height: 156rpx;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	padding: 16rpx $space-page calc(24rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	box-sizing: border-box;
}

.bottom-info {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.bottom-label {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.bottom-price {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}

.submit-btn {
	min-width: 236rpx;
	min-height: 88rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.submit-btn.disabled {
	background: $border-strong;
	@include disabled-state;
}

.submit-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

/* 支付方式选择 */
.pay-method-list {
	display: flex;
	flex-direction: column;
}

.pay-method-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	padding: 20rpx 0;
	@include active-press;
}

.pay-method-row + .pay-method-row {
	border-top: 2rpx solid $border-light;
}

.pay-method-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.pay-method-name {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.pay-method-desc {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.pay-radio {
	width: 36rpx;
	height: 36rpx;
	border-radius: 50%;
	border: 4rpx solid $border-strong;
	box-sizing: border-box;
	flex-shrink: 0;
	transition: all 0.15s ease;
}

.pay-radio-active {
	border-color: $accent-primary;
	background: $accent-primary;
	box-shadow: inset 0 0 0 6rpx #FFFFFF;
}
</style>
