<template>
	<view class="page">
		<app-nav title="确认点单" />

		<view v-if="tableNo" class="table-context-card">
			<text class="table-context-label">桌号</text>
			<text class="table-context-value">{{ tableNo }}</text>
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

		<view v-if="cartList.length" class="section-card pay-method-card">
			<text class="section-title">支付方式</text>
			<view class="pay-method-row" @tap="payType = 'shouqianba'">
				<view class="pay-method-main">
					<text class="pay-method-name">微信支付</text>
					<text class="pay-method-desc">使用微信完成付款</text>
				</view>
				<view class="pay-radio" :class="{ 'pay-radio-active': payType === 'shouqianba' }"></view>
			</view>
			<view class="pay-method-row" @tap="payType = 'balance'">
				<view class="pay-method-main">
					<text class="pay-method-name">余额支付</text>
					<text class="pay-method-desc">从会员余额扣款</text>
				</view>
				<view class="pay-radio" :class="{ 'pay-radio-active': payType === 'balance' }"></view>
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
import { getLocalUserInfo, getLocalUserId, ensureLocalLogin } from '@/utils/session.js'
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
			payType: 'shouqianba',
			pickupTemplateId: '',
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
		this.loadSubscribeConfig()
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

		loadSubscribeConfig() {
			requestPromise({
				url: scanOrderApi.subscribeConfig,
				method: 'GET',
				header: this.authHeader()
			}).then(res => {
				if (!isSuccessResponse(res)) return
				const templateId = res.data && res.data.data && res.data.data.pickupTemplateId
				if (templateId) {
					this.pickupTemplateId = templateId
				}
			}).catch(() => {})
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
				if (data.payAmount != null) this.payAmount = toNumber(data.payAmount)
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
				await this.requestPickupSubscribe()
				const userInfo = getLocalUserInfo()
				const createRes = await requestPromise({
					url: scanOrderApi.create,
					method: 'POST',
					header: this.authHeader(),
					data: {
						shopId: this.shopId,
						tableNo: this.tableNo,
						openid: userInfo.openid || '',
						remark: this.remark,
						payType: this.payType
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
					url: scanOrderApi.pay + orderId + '?payType=' + this.payType,
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
		},

		requestPickupSubscribe() {
			return new Promise((resolve, reject) => {
				if (!uni.requestSubscribeMessage) {
					resolve()
					return
				}
				const templateId = this.pickupTemplateId
				if (!templateId) {
					showError('订阅模板配置未加载，请稍后重试')
					reject(new Error('pickup template id missing'))
					return
				}
				uni.requestSubscribeMessage({
					tmplIds: [templateId],
					success: (res) => {
						console.log('订阅授权成功:', res)
						resolve(res)
					},
					fail: (err) => {
						console.log('订阅授权失败:', err)
						resolve(err)
					}
				})
			})
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

.section-card {
	@include card(20rpx 24rpx, $radius-sm);
	margin: 0 $space-page 18rpx;
}

.table-context-card {
	@include card(18rpx 24rpx, $radius-sm);
	margin: 0 $space-page 18rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.table-context-label,
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

.table-context-value,
.section-title,
.cart-name,
.empty-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
	color: $text-primary;
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

.pay-method-card {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}

.pay-method-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
	padding: 18rpx 0;
	border-top: 2rpx solid $border-subtle;
}

.pay-method-row:first-of-type {
	border-top: 0;
}

.pay-method-main {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.pay-method-name {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: $text-primary;
}

.pay-method-desc {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.5;
	color: $text-tertiary;
}

.pay-radio {
	width: 34rpx;
	height: 34rpx;
	border-radius: 50%;
	border: 3rpx solid $border-subtle;
	box-sizing: border-box;
	flex-shrink: 0;
}

.pay-radio-active {
	border: 10rpx solid $accent-primary;
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
