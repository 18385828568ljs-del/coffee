<template>
	<view class="page">
		<app-nav title="确认点单" />

		<view class="scan-context-card">
			<view class="scan-context-item">
				<text class="scan-context-label">门店</text>
				<text class="scan-context-value">{{ shopName }}</text>
			</view>
			<view class="scan-context-item scan-context-item-right">
				<text class="scan-context-label">桌号</text>
				<text class="scan-context-value">{{ tableNo || '未识别' }}</text>
			</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">已选商品</text>
				<text class="section-subtitle">共 {{ totalQuantity }} 件</text>
			</view>

			<view v-if="!cartList.length && !loading" class="empty-box">
				<text class="empty-title">还没有选择商品</text>
				<text class="empty-subtitle">返回点单页选择现制饮品。</text>
			</view>

			<view v-for="item in cartList" :key="item.id" class="cart-item">
				<image
					v-if="itemImage(item)"
					class="cart-image"
					:src="itemImage(item)"
					mode="aspectFill"
				/>
				<view v-else class="cart-image-empty">
					<text>{{ shortName(item.productName) }}</text>
				</view>
				<view class="cart-main">
					<text class="cart-name">{{ item.productName }}</text>
					<text v-if="item.specText" class="cart-spec">{{ item.specText }}</text>
					<view class="cart-meta">
						<text class="cart-price">¥{{ money(item.price) }}</text>
						<text class="cart-qty">x{{ item.quantity || 0 }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="section-card remark-card">
			<text class="section-title">备注</text>
			<textarea
				class="remark-input"
				v-model="remark"
				maxlength="80"
				placeholder="如少冰、分开打包等，可不填"
				placeholder-class="remark-placeholder"
			/>
		</view>

		<view v-if="cartList.length" class="section-card amount-card">
			<view class="amount-row">
				<text class="amount-label">商品原价</text>
				<text class="amount-value">¥{{ money(totalAmount) }}</text>
			</view>
			<view v-if="discountAmount > 0" class="amount-row amount-row-saving">
				<text class="amount-label">活动优惠</text>
				<text class="amount-value">-¥{{ money(discountAmount) }}</text>
			</view>
			<view v-if="memberDiscount > 0" class="amount-row amount-row-saving">
				<text class="amount-label">会员折扣</text>
				<text class="amount-value">-¥{{ money(memberDiscount) }}</text>
			</view>
			<view v-if="activitySummary" class="amount-row activity-summary-row">
				<text class="amount-summary">{{ activitySummary }}</text>
			</view>
		</view>

		<view class="bottom-bar">
			<view class="total-box">
				<text class="total-label">应付</text>
				<view class="total-price-row">
					<text class="total-symbol">¥</text>
					<text class="total-price">{{ money(payAmount) }}</text>
				</view>
			</view>
			<view class="pay-btn" :class="{ 'pay-btn-disabled': !cartList.length || submitting }" @tap="submitAndPay">
				<text>{{ submitting ? '支付中...' : '立即付款' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import { scanCartApi, scanOrderApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getLocalUserId, ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'
import { showError, showSuccess, showBusy, hideBusy } from '@/utils/ui-feedback.js'

function toNumber(value) {
	const numberValue = Number(value || 0)
	return Number.isFinite(numberValue) ? numberValue : 0
}

export default {
	data() {
		return {
			shopId: 1,
			tableNo: '',
			shopName: '咖啡门店',
			cartList: [],
			totalQuantity: 0,
			totalAmount: 0,
			payAmount: 0,
			discountAmount: 0,
			memberDiscount: 0,
			activitySummary: '',
			remark: '',
			loading: false,
			submitting: false
		}
	},

	onLoad(options = {}) {
		this.shopId = toNumber(options.shopId) || 1
		this.tableNo = options.tableNo ? decodeURIComponent(String(options.tableNo)).trim() : ''
		if (options.shopName) {
			this.shopName = decodeURIComponent(String(options.shopName)) || this.shopName
		}
		this.loadCartList()
	},

	onShow() {
		this.loadCartList()
	},

	methods: {
		authHeader() {
			const token = getToken()
			const header = { 'Content-Type': 'application/json' }
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},

		money(value) {
			return toNumber(value).toFixed(2)
		},

		shortName(name) {
			return String(name || '咖啡').slice(0, 2)
		},

		itemImage(item) {
			return resolveImageUrl((item && item.productImage) || '')
		},

		async loadCartList() {
			const userId = getLocalUserId()
			if (!userId) {
				this.cartList = []
				this.totalQuantity = 0
				this.totalAmount = 0
				this.payAmount = 0
				this.discountAmount = 0
				this.memberDiscount = 0
				this.activitySummary = ''
				return
			}
			this.loading = true
			try {
				const res = await requestPromise({
					url: scanCartApi.list,
					method: 'GET',
					data: {
						shopId: this.shopId,
						tableNo: this.tableNo
					},
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '购物车加载失败')
					return
				}
				const data = (res.data && res.data.data) || {}
				this.cartList = Array.isArray(data.list) ? data.list : []
				this.totalQuantity = toNumber(data.totalQuantity)
				this.totalAmount = toNumber(data.totalAmount)
				// 默认 payAmount 等于 totalAmount, 紧接着 loadPreview 用真实金额覆盖
				this.payAmount = this.totalAmount
				this.discountAmount = 0
				this.memberDiscount = 0
				this.activitySummary = ''
				if (this.cartList.length) {
					await this.loadPreview()
				}
			} catch (error) {
				showError('购物车加载失败')
			} finally {
				this.loading = false
			}
		},

		async loadPreview() {
			try {
				const res = await requestPromise({
					url: scanOrderApi.preview,
					method: 'GET',
					data: {
						shopId: this.shopId,
						tableNo: this.tableNo
					},
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) return
				const data = (res.data && res.data.data) || {}
				// 后端返回的 totalAmount 来自服务端商品当前价,优先用它,避免被前端汇总污染
				if (data.totalAmount != null) this.totalAmount = toNumber(data.totalAmount)
				this.payAmount = toNumber(data.payAmount)
				this.discountAmount = toNumber(data.discountAmount)
				this.memberDiscount = toNumber(data.memberDiscount)
				this.activitySummary = data.activitySummary || ''
			} catch (error) {
				// 预览失败不影响下单, 沿用原价显示
			}
		},

		async submitAndPay() {
			if (!this.cartList.length || this.submitting) return
			if (!ensureLocalLogin('请先登录后再付款')) return
			this.submitting = true
			showBusy('正在下单...')
			try {
				const createRes = await requestPromise({
					url: scanOrderApi.create,
					method: 'POST',
					header: this.authHeader(),
					data: {
						shopId: this.shopId,
						tableNo: this.tableNo,
						remark: this.remark,
						payType: 'wechat'
					}
				})
				if (!isSuccessResponse(createRes)) {
					showError((createRes && createRes.data && createRes.data.msg) || '下单失败')
					return
				}
				const order = (createRes.data && createRes.data.data) || {}
				const orderId = order.orderId
				if (!orderId) {
					showError('订单信息异常')
					return
				}
				const payRes = await requestPromise({
					url: scanOrderApi.pay + orderId + '?payType=wechat',
					method: 'PUT',
					header: this.authHeader()
				})
				if (!isSuccessResponse(payRes)) {
					showError((payRes && payRes.data && payRes.data.msg) || '支付失败')
					return
				}
				showSuccess('付款成功')
				setTimeout(() => {
					uni.redirectTo({ url: `/pages/scan/pay-success?orderId=${orderId}` })
				}, 600)
			} catch (error) {
				showError('付款失败，请稍后重试')
			} finally {
				hideBusy()
				this.submitting = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
	padding-bottom: 176rpx;
}

.scan-context-card,
.section-card {
	@include card(20rpx 24rpx, $radius-sm);
	margin: 0 $space-page 18rpx;
}

.scan-context-card {
	display: flex;
	justify-content: space-between;
	gap: 20rpx;
}

.scan-context-item {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.scan-context-item-right {
	align-items: flex-end;
	flex-shrink: 0;
}

.scan-context-label,
.section-subtitle,
.cart-spec,
.cart-qty,
.empty-subtitle,
.total-label {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.scan-context-value,
.section-title,
.cart-name,
.empty-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
	color: $text-primary;
}

.scan-context-value {
	margin-top: 6rpx;
}

.section-head,
.cart-item,
.cart-meta,
.bottom-bar,
.total-price-row {
	display: flex;
	align-items: center;
}

.section-head {
	justify-content: space-between;
	margin-bottom: 18rpx;
}

.cart-item {
	padding: 18rpx 0;
	border-top: 2rpx solid $border-subtle;
}

.cart-item:first-of-type {
	border-top: 0;
}

.cart-image,
.cart-image-empty {
	width: 120rpx;
	height: 120rpx;
	border-radius: $radius-sm;
	background: $accent-surface;
	flex-shrink: 0;
}

.cart-image-empty {
	display: flex;
	align-items: center;
	justify-content: center;
}

.cart-image-empty text {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 700;
	color: rgba(111, 78, 55, 0.72);
}

.cart-main {
	flex: 1;
	min-width: 0;
	margin-left: 18rpx;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.cart-name,
.cart-spec {
	display: -webkit-box;
	-webkit-box-orient: vertical;
	overflow: hidden;
	text-overflow: ellipsis;
}

.cart-name {
	-webkit-line-clamp: 1;
}

.cart-spec {
	-webkit-line-clamp: 1;
}

.cart-meta {
	justify-content: space-between;
}

.cart-price {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.empty-box {
	min-height: 260rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-direction: column;
	gap: 12rpx;
}

.remark-card {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}

.remark-input {
	min-height: 124rpx;
	padding: 18rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-primary;
	box-sizing: border-box;
}

.remark-placeholder {
	color: $text-tertiary;
}

.amount-card {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.amount-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.amount-label {
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-secondary;
}

.amount-value {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.amount-row-saving .amount-value {
	color: $accent-danger;
}

.activity-summary-row {
	margin-top: 4rpx;
}

.amount-summary {
	font-family: $font-family;
	font-size: 22rpx;
	color: $accent-primary;
	background: $accent-primary-soft;
	padding: 6rpx 12rpx;
	border-radius: $radius-xs;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	justify-content: space-between;
	gap: 20rpx;
	padding: 18rpx $space-page calc(18rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	box-sizing: border-box;
	z-index: 40;
}

.total-box {
	flex: 1;
	min-width: 0;
}

.total-price-row {
	margin-top: 6rpx;
	align-items: flex-end;
}

.total-symbol {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: $text-primary;
	line-height: 1.2;
}

.total-price {
	font-family: $font-family;
	font-size: 42rpx;
	font-weight: 800;
	color: $text-primary;
	line-height: 1;
}

.pay-btn {
	height: 88rpx;
	min-width: 248rpx;
	padding: 0 34rpx;
	border-radius: $radius-sm;
	background: $accent-primary;
	display: flex;
	align-items: center;
	justify-content: center;
	@include active-press;
}

.pay-btn-disabled {
	background: rgba(111, 78, 55, 0.28);
}

.pay-btn text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: #FFFFFF;
}
</style>
