<template>
	<view class="page">
		<app-nav title="购物车" :show-back="false" :right-text="cartTotalQuantity ? `共 ${cartTotalQuantity} 件` : '空购物车'" />

		<scroll-view v-if="hasAnyCartItem" class="cart-list" scroll-y>
			<view v-if="scanCartList.length" class="cart-section cart-section-scan">
				<view class="section-head">
					<view class="section-title-wrap">
						<text class="section-badge">点单</text>
						<view class="section-title-copy">
							<text class="section-title">点单商品</text>
							<text class="section-subtitle">堂食现制，按桌台结算</text>
						</view>
					</view>
					<text class="section-count">{{ scanTotalQuantity }} 件</text>
				</view>

				<view
					v-for="(item, index) in scanCartList"
					:key="item.cartKey"
					class="cart-item"
				>
					<image
						v-if="getCartImage(item)"
						class="item-image"
						:src="getCartImage(item)"
						mode="aspectFill"
					></image>
					<view v-else class="item-image item-image-empty"></view>
					<view class="item-info">
						<view class="item-copy">
							<text class="item-name">{{ item.productName }}</text>
							<text class="item-spec">{{ item.spec || '现制饮品' }}</text>
							<text v-if="item.tableNo" class="item-spec">桌号 {{ item.tableNo }}</text>
						</view>
						<view class="item-bottom">
							<text class="item-price">¥{{ formatMoney(getCartItemPrice(item)) }}</text>
							<view class="item-counter">
								<view class="counter-btn" :class="{ 'counter-btn-disabled': isCartItemPending(item) }" @click="decreaseScanQuantity(index)">
									<view class="counter-icon counter-icon-minus"></view>
								</view>
								<text class="counter-num" :class="{ 'counter-num-busy': isCartItemPending(item) }">
									{{ isCartItemPending(item) ? '...' : item.quantity }}
								</text>
								<view class="counter-btn counter-btn-add" :class="{ 'counter-btn-disabled': isCartItemPending(item) }" @click="increaseScanQuantity(index)">
									<view class="counter-icon counter-icon-plus counter-icon-light"></view>
								</view>
							</view>
						</view>
						<view class="item-delete" :class="{ 'item-delete-disabled': removingCartId === getCartId(item) }" @click="removeScanFromCart(index)">
							<text>{{ removingCartId === getCartId(item) ? '删除中...' : '删除' }}</text>
						</view>
					</view>
				</view>

				<view class="section-checkout compact-checkout">
					<view class="checkout-copy">
						<text class="checkout-label">点单小计</text>
						<text class="checkout-price">¥{{ scanPayPrice }}</text>
					</view>
					<view class="checkout-btn" @click="checkoutScan">
						<text>点单结算</text>
					</view>
				</view>
			</view>

			<view v-if="cartList.length" class="cart-section cart-section-mall">
				<view class="section-head">
					<view class="section-title-wrap">
						<text class="section-badge section-badge-mall">商城</text>
						<view class="section-title-copy">
							<text class="section-title">商城商品</text>
							<text class="section-subtitle">零售配送，按收货地址结算</text>
						</view>
					</view>
					<text class="section-count">{{ mallTotalQuantity }} 件</text>
				</view>

				<view
					v-for="(item, index) in cartList"
					:key="item.cartId || item.id || index"
					class="cart-item"
				>
					<image
						v-if="getCartImage(item)"
						class="item-image"
						:src="getCartImage(item)"
						mode="aspectFill"
					></image>
					<view v-else class="item-image item-image-empty"></view>
					<view class="item-info">
						<view class="item-copy">
							<text class="item-name">{{ item.productName }}</text>
							<text class="item-spec">{{ item.spec || '现烘发货 / 新鲜包装' }}</text>
						</view>
						<view class="item-bottom">
							<text class="item-price">¥{{ formatMoney(getCartItemPrice(item)) }}</text>
							<view class="item-counter">
								<view class="counter-btn" :class="{ 'counter-btn-disabled': isCartItemPending(item) }" @click="decreaseQuantity(index)">
									<view class="counter-icon counter-icon-minus"></view>
								</view>
								<text class="counter-num" :class="{ 'counter-num-busy': isCartItemPending(item) }">
									{{ isCartItemPending(item) ? '...' : item.quantity }}
								</text>
								<view class="counter-btn counter-btn-add" :class="{ 'counter-btn-disabled': isCartItemPending(item) }" @click="increaseQuantity(index)">
									<view class="counter-icon counter-icon-plus counter-icon-light"></view>
								</view>
							</view>
						</view>
						<view class="item-delete" :class="{ 'item-delete-disabled': removingCartId === item.cartId }" @click="removeFromCart(index)">
							<text>{{ removingCartId === item.cartId ? '删除中...' : '删除' }}</text>
						</view>
					</view>
				</view>

				<view class="section-checkout compact-checkout">
					<view class="checkout-copy">
						<text class="checkout-label">商城小计</text>
						<text class="checkout-price">¥{{ payPrice }}</text>
						<text v-if="previewInfo.activitySummary" class="checkout-note">{{ previewInfo.activitySummary }}</text>
					</view>
					<view class="checkout-btn" @click="checkoutMall">
						<text>商城结算</text>
					</view>
				</view>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view v-else class="empty-cart">
			<image class="empty-image" src="/static/empty-cart.svg" mode="aspectFit"></image>
			<text class="empty-title">购物车还是空的</text>
				<text class="empty-text">可以去点单页选择现制饮品，也可以去首页挑选商城商品。</text>
				<view class="empty-btn" @click="goShopping">
					<text>去逛逛</text>
				</view>
		</view>

		<bottom-tab-bar current="cart" />
	</view>
</template>

<script>
import { cartApi, activityApi, scanCartApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { ensureLocalLogin, getLocalUserId } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'
import { getToken } from '@/utils/auth.js'

const ORDER_DRAFT_KEY = 'orderConfirmDraft'

function toNumber(value) {
	const number = Number(value || 0)
	return Number.isFinite(number) ? number : 0
}

export default {
	data() {
		return {
			scanShopId: 1,
			scanShopName: '咖啡门店',
			cartList: [],
			scanCartList: [],
			pendingCartIds: {},
			removingCartId: null,
			previewInfo: {
				totalAmount: 0,
				payAmount: 0,
				discountAmount: 0,
				freightAmount: 0,
				activitySummary: '',
				giftItems: []
			}
		}
	},

	computed: {
		hasAnyCartItem() {
			return this.cartList.length > 0 || this.scanCartList.length > 0
		},

		cartTotalQuantity() {
			return this.mallTotalQuantity + this.scanTotalQuantity
		},

		mallTotalQuantity() {
			return this.cartList.reduce((sum, item) => sum + toNumber(item.quantity), 0)
		},

		scanTotalQuantity() {
			return this.scanCartList.reduce((sum, item) => sum + toNumber(item.quantity), 0)
		},

		scanPayPrice() {
			return this.scanCartList
				.reduce((sum, item) => sum + this.getCartItemPrice(item) * toNumber(item.quantity), 0)
				.toFixed(2)
		},

		scanTableNo() {
			const first = this.scanCartList.find((item) => item && item.tableNo)
			return first ? first.tableNo : ''
		},

		totalQuantity() {
			return this.mallTotalQuantity
		},

		totalPrice() {
			return this.cartList
				.reduce((sum, item) => sum + this.getCartItemPrice(item) * toNumber(item.quantity), 0)
				.toFixed(2)
		},

		payPrice() {
			const value = this.previewInfo.payAmount || this.totalPrice || 0
			return Number(value).toFixed(2)
		}
	},

	onLoad() {
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

		formatMoney(value) {
			return toNumber(value).toFixed(2)
		},

		getCartItemPrice(item) {
			return toNumber(item && (item.salePrice || item.price))
		},

		getCartImage(item) {
			return resolveImageUrl(item.productImg || item.productImage || item.imageUrl)
		},

		getCartId(item) {
			return item && (item.cartId || item.id)
		},

		isCartItemPending(item) {
			const cartId = this.getCartId(item)
			return !!(cartId && this.pendingCartIds[String(cartId)])
		},

		async loadCartList() {
			const userId = getLocalUserId()
			if (!userId) {
				this.cartList = []
				this.scanCartList = []
				this.resetPreview()
				return
			}

			showBusy('加载中...')

			try {
				const [res] = await Promise.all([
					requestPromise({
						url: cartApi.list,
						method: 'GET',
						data: { userId }
					}),
					this.loadScanCartList(false)
				])
				if (isSuccessResponse(res)) {
					this.cartList = res.data.data || []
					await this.loadPreview()
					return
				}

				this.cartList = []
				this.resetPreview()
				showError((res.data && res.data.msg) || '购物车加载失败')
			} catch (error) {
				this.cartList = []
				this.scanCartList = []
				this.resetPreview()
				showError('购物车加载失败')
			} finally {
				hideBusy()
			}
		},

		async loadScanCartList(showLoading = true) {
			const userId = getLocalUserId()
			if (!userId) {
				this.scanCartList = []
				return
			}

			if (showLoading) {
				showBusy('加载中...')
			}
			try {
				const res = await requestPromise({
					url: scanCartApi.list,
					method: 'GET',
					data: { shopId: this.scanShopId },
					header: this.authHeader()
				})
				if (isSuccessResponse(res)) {
					const data = (res.data && res.data.data) || {}
					const list = Array.isArray(data.list) ? data.list : []
					this.scanCartList = list.map((item, index) => ({
						...item,
						cartId: item.id || item.cartId,
						spec: item.specText || item.spec || '',
						cartKey: String(item.id || item.cartId || index)
					}))
					return
				}
				this.scanCartList = []
				showError((res.data && res.data.msg) || '点单购物车加载失败')
			} catch (error) {
				this.scanCartList = []
				showError('点单购物车加载失败')
			} finally {
				if (showLoading) {
					hideBusy()
				}
			}
		},

		async decreaseQuantity(index) {
			const item = this.cartList[index]
			if (!item || this.isCartItemPending(item)) {
				return
			}
			if (toNumber(item.quantity) > 1) {
				const nextQuantity = toNumber(item.quantity) - 1
				item.quantity = nextQuantity
				const success = await this.updateCart(item)
				if (success) {
					this.refreshSummary()
				}
				return
			}
			this.removeFromCart(index)
		},

		async increaseQuantity(index) {
			const item = this.cartList[index]
			if (!item || this.isCartItemPending(item)) {
				return
			}
			item.quantity = toNumber(item.quantity) + 1
			const success = await this.updateCart(item)
			if (success) {
				this.refreshSummary()
			}
		},

		async decreaseScanQuantity(index) {
			const item = this.scanCartList[index]
			if (!item || this.isCartItemPending(item)) return
			if (toNumber(item.quantity) > 1) {
				item.quantity = toNumber(item.quantity) - 1
				const success = await this.updateScanCart(item, this.getCartId(item))
				if (!success) this.loadScanCartList(false)
				return
			}
			this.removeScanFromCart(index)
		},

		async increaseScanQuantity(index) {
			const item = this.scanCartList[index]
			if (!item || this.isCartItemPending(item)) return
			item.quantity = toNumber(item.quantity) + 1
			const success = await this.updateScanCart(item, this.getCartId(item))
			if (!success) this.loadScanCartList(false)
		},

		async updateCart(item) {
			const previousQuantity = toNumber(item && item.quantity)
			const cartId = this.getCartId(item)
			const cartKey = String(cartId || '')
			if (!cartKey || this.pendingCartIds[cartKey]) {
				return false
			}
			this.$set(this.pendingCartIds, cartKey, true)
			try {
				const res = await requestPromise({
					url: cartApi.update,
					method: 'PUT',
					data: {
						cartId: item.cartId,
						userId: getLocalUserId(),
						quantity: item.quantity,
						spec: item.spec
					}
				})
				if (isSuccessResponse(res)) {
					return true
				}
				showError((res.data && res.data.msg) || '更新失败')
			} catch (error) {
				showError('更新失败')
			} finally {
				this.$delete(this.pendingCartIds, cartKey)
			}

			if (item) {
				item.quantity = previousQuantity
			}
			this.loadCartList()
			return false
		},

		async updateScanCart(item, cartId) {
			const cartKey = String(cartId || '')
			if (!cartKey || this.pendingCartIds[cartKey]) return false
			this.$set(this.pendingCartIds, cartKey, true)
			try {
				const res = await requestPromise({
					url: scanCartApi.update,
					method: 'PUT',
					header: this.authHeader(),
					data: { id: cartId, quantity: item.quantity }
				})
				if (isSuccessResponse(res)) return true
				showError((res.data && res.data.msg) || '更新失败')
			} catch (error) {
				showError('更新失败')
			} finally {
				this.$delete(this.pendingCartIds, cartKey)
			}
			return false
		},

		refreshSummary() {
			this.loadPreview()
		},

		async removeFromCart(index) {
			const item = this.cartList[index]
			const cartId = this.getCartId(item)
			if (!item || this.isCartItemPending(item) || this.removingCartId === cartId) {
				return
			}

			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要删除该商品吗？'
			})
			if (!confirmed) {
				return
			}

			this.removingCartId = cartId
			try {
				const deleteRes = await requestPromise({
					url: `${cartApi.delete + item.cartId}?userId=${getLocalUserId()}`,
					method: 'DELETE'
				})
				if (isSuccessResponse(deleteRes)) {
					this.cartList.splice(index, 1)
					if (this.cartList.length === 0) {
						this.resetPreview()
					} else {
						this.loadPreview()
					}
					showSuccess('已删除')
					return
				}

				showError((deleteRes.data && deleteRes.data.msg) || '删除失败')
			} catch (error) {
				showError('删除失败')
			} finally {
				this.removingCartId = null
			}

			this.loadCartList()
		},

		async removeScanFromCart(index) {
			const item = this.scanCartList[index]
			const cartId = this.getCartId(item)
			if (!item || this.isCartItemPending(item) || this.removingCartId === cartId) return

			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要删除该点单商品吗？'
			})
			if (!confirmed) return

			this.removingCartId = cartId
			try {
				const deleteRes = await requestPromise({
					url: scanCartApi.delete + cartId,
					method: 'DELETE',
					header: this.authHeader()
				})
				if (isSuccessResponse(deleteRes)) {
					this.scanCartList.splice(index, 1)
					showSuccess('已删除')
					return
				}
				showError((deleteRes.data && deleteRes.data.msg) || '删除失败')
			} catch (error) {
				showError('删除失败')
			} finally {
				this.removingCartId = null
			}
			this.loadScanCartList(false)
		},

		resetPreview() {
			this.previewInfo = {
				totalAmount: 0,
				payAmount: 0,
				discountAmount: 0,
				freightAmount: 0,
				activitySummary: '',
				giftItems: []
			}
		},

		async loadPreview() {
			if (this.cartList.length === 0) {
				this.resetPreview()
				return
			}

			try {
				const res = await requestPromise({
					url: activityApi.preview,
					method: 'POST',
					data: {
						userId: getLocalUserId(),
						orderItems: this.cartList.map((item) => ({
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
					return
				}
			} catch (error) {
				// noop
			}

			this.resetPreview()
		},

		checkoutMall() {
			if (!ensureLocalLogin()) {
				return
			}

			if (this.cartList.length === 0) {
				showError('购物车为空')
				return
			}

			const draftItems = this.cartList.map((item) => ({
				cartId: item.cartId,
				productId: item.productId,
				productName: item.productName,
				productImage: item.productImage || item.productImg || item.imageUrl || '',
				price: item.price,
				salePrice: item.salePrice || item.price,
				quantity: item.quantity,
				spec: item.spec || ''
			}))

			uni.setStorageSync(ORDER_DRAFT_KEY, {
				source: 'cart',
				cartIds: draftItems.map((item) => item.cartId),
				items: draftItems,
				remark: ''
			})

			uni.navigateTo({
				url: '/pages/order/confirm'
			})
		},

		checkoutScan() {
			if (!ensureLocalLogin()) return
			if (!this.scanCartList.length) {
				showError('点单购物车为空')
				return
			}
			const url = `/pages/scan/confirm?shopId=${this.scanShopId}&tableNo=${encodeURIComponent(this.scanTableNo || '')}&shopName=${encodeURIComponent(this.scanShopName || '')}`
			uni.navigateTo({ url })
		},

		goShopping() {
			uni.switchTab({
				url: '/pages/index/index'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
	padding-bottom: calc(292rpx + env(safe-area-inset-bottom));
}

.cart-list {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 0;
	box-sizing: border-box;
}

.cart-section {
	@include card(0, $radius-md);
	margin-bottom: 22rpx;
	overflow: hidden;
}

.section-head {
	padding: 24rpx;
	background: rgba(255, 255, 255, 0.96);
	border-bottom: 2rpx solid $border-light;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
}

.section-title-wrap {
	display: flex;
	align-items: center;
	gap: 16rpx;
	min-width: 0;
}

.section-title-copy {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.section-badge {
	min-width: 64rpx;
	height: 44rpx;
	padding: 0 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 999rpx;
	background: rgba(111, 78, 55, 0.12);
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 700;
	color: $accent-primary;
	box-sizing: border-box;
}

.section-badge-mall {
	background: rgba(45, 93, 73, 0.11);
	color: #2D5D49;
}

.section-title {
	display: block;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.section-subtitle {
	display: block;
	margin-top: 4rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.section-count {
	flex-shrink: 0;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $accent-primary;
}

.cart-item {
	padding: 22rpx 24rpx;
	display: flex;
	align-items: center;
	gap: 20rpx;
	background: #FFFFFF;
	border-bottom: 2rpx solid $border-light;
}

.cart-item:last-of-type {
	border-bottom: 0;
}

.item-image {
	width: 132rpx;
	height: 132rpx;
	flex-shrink: 0;
	border-radius: $radius-sm;
	background: $accent-surface;
}

.item-image-empty {
	background: $bg-muted;
}

.item-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.item-copy {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.item-name {
	font-family: $font-family;
	font-size: 27rpx;
	font-weight: 600;
	line-height: 1.4;
	color: $text-primary;
}

.item-spec {
	font-family: $font-family;
	font-size: 21rpx;
	font-weight: 500;
	color: $text-secondary;
}

.item-bottom {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.item-price {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.item-counter {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.counter-btn {
	width: 46rpx;
	height: 46rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $bg-muted;
	border: 2rpx solid $border-subtle;
	@include active-press;
}

.counter-btn-add {
	background: $accent-primary;
	border-color: $accent-primary;
}

.counter-btn-disabled {
	@include disabled-state;
}

.counter-icon {
	position: relative;
	width: 22rpx;
	height: 22rpx;
	color: $text-secondary;
	flex-shrink: 0;
}

.counter-icon::before,
.counter-icon::after {
	content: '';
	position: absolute;
	left: 50%;
	top: 50%;
	background: currentColor;
	border-radius: 999rpx;
	transform: translate(-50%, -50%);
}

.counter-icon::before {
	width: 18rpx;
	height: 4rpx;
}

.counter-icon-plus::after {
	width: 4rpx;
	height: 18rpx;
}

.counter-icon-minus::after {
	display: none;
}

.counter-icon-light {
	color: #FFFFFF;
}

.counter-num {
	min-width: 32rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
	text-align: center;
}

.counter-num-busy {
	color: $text-tertiary;
}

.item-delete {
	align-self: flex-end;
	@include active-press;
	padding: 6rpx 2rpx;
}

.item-delete text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $accent-danger;
}

.item-delete-disabled {
	@include disabled-state;
}

.summary-card {
	padding: 16rpx 24rpx;
	background: #FFFFFF;
	border-top: 2rpx solid $border-light;
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.remark-card {
	border-top: 0;
}

.remark-title {
	flex-shrink: 0;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.remark-input {
	flex: 1;
	height: 64rpx;
	min-height: 64rpx;
	padding: 0 18rpx;
	box-sizing: border-box;
	border-radius: $radius-sm;
	background: $bg-muted;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.5;
	color: $text-primary;
}

.remark-placeholder {
	color: $text-tertiary;
}

.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 140rpx 40rpx 0;
}

.empty-image {
	width: 300rpx;
	height: 300rpx;
}

.empty-title {
	margin-top: 20rpx;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.empty-text {
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
	min-height: 88rpx;
	padding: 0 36rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
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

.section-checkout {
	padding: 20rpx 24rpx 24rpx;
	background: #FFFFFF;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	border-top: 2rpx solid $border-light;
}

.checkout-copy {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.checkout-label {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.checkout-note {
	display: block;
	max-width: 400rpx;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	line-height: 1.4;
	color: $text-secondary;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.checkout-price {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}

.checkout-btn {
	min-width: 220rpx;
	min-height: 88rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.checkout-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

.checkout-btn-disabled {
	@include disabled-state;
}
</style>
