<template>
	<view class="page">
		<app-nav title="点单" :show-back="false" :left-text="navTableText" />

		<view v-if="loadError" class="error-bar">
			<text class="error-text">{{ loadError }}</text>
		</view>

		<view class="immersive-shell">
			<view v-if="!products.length && !loading && !loadError" class="empty-main">
				<text class="empty-main-title">暂无可点商品</text>
				<text class="empty-main-subtitle">稍后再来看看，或刷新一下页面。</text>
			</view>

			<scroll-view
				v-else
				class="product-scroll"
				scroll-y
				:show-scrollbar="false"
			>
				<view class="product-list">
					<view
						v-for="(prod, index) in products"
						:key="prod.productId || index"
						class="product-list-item"
					>
						<immersive-product-card
							:product="prod"
							:flipped="isFlipped(prod)"
							:active-card="isFlipped(prod)"
							:shop-id="shopId"
							:table-no="tableNo"
							@flip="onCardFlip"
							@close="onCardBackClose"
							@quick-add="onQuickAdd"
							@play-video="onPlayVideo"
							@added="onProductAdded"
							@checkout="goConfirmAfterAdd"
						/>
					</view>
				</view>
			</scroll-view>

			<view v-if="products.length" class="page-progress">
				<text>共 {{ products.length }} 款</text>
			</view>

		</view>

		<view v-if="cartExpanded" class="cart-mask" @tap="toggleCart"></view>

		<view v-if="cartExpanded" class="cart-drawer">
			<view class="cart-drawer-head">
				<view class="cart-drawer-title-wrap">
					<text class="cart-drawer-title">已选商品</text>
					<text class="cart-drawer-subtitle">{{ cartTotalQuantity }} 件 · 桌号 {{ tableNo || '未识别' }}</text>
				</view>
				<text class="cart-drawer-close" @tap="toggleCart">收起</text>
			</view>

			<scroll-view class="cart-drawer-list" scroll-y>
				<view v-if="!cartList.length" class="cart-drawer-empty">
					<text>还没有加入点单商品</text>
				</view>
				<view v-for="(item, index) in cartList" :key="item.cartKey" class="cart-drawer-item">
					<image
						v-if="item.productImageUrl"
						class="cart-drawer-image"
						:src="item.productImageUrl"
						mode="aspectFill"
					/>
					<view v-else class="cart-drawer-image cart-drawer-image-empty">
						<text>{{ shortName(item.productName) }}</text>
					</view>
					<view class="cart-drawer-copy">
						<text class="cart-drawer-name">{{ item.productName }}</text>
						<text v-if="item.specText || item.spec" class="cart-drawer-spec">{{ item.specText || item.spec }}</text>
						<text class="cart-drawer-price">¥{{ money(item.price) }}</text>
					</view>
					<view class="cart-drawer-actions">
						<view class="cart-step-btn" @tap="decreaseCartItem(item, index)"><text>-</text></view>
						<text class="cart-step-num">{{ item.quantity || 0 }}</text>
						<view class="cart-step-btn cart-step-btn-add" @tap="increaseCartItem(item)"><text>+</text></view>
					</view>
				</view>
			</scroll-view>
		</view>

		<view
			v-for="dot in flyDots"
			:key="dot.id"
			class="fly-cart-dot"
			:style="dot.style"
		>
			<text>+</text>
		</view>

		<view class="cart-bar">
			<view class="cart-icon cart-bag-target" :class="{ 'cart-icon-empty': cartTotalQuantity <= 0 }" @tap="toggleCart">
				<text>袋</text>
			</view>
			<view class="cart-copy" @tap="toggleCart">
				<text class="cart-amount">¥{{ cartTotalAmount }}</text>
				<text class="cart-count">{{ cartTotalQuantity > 0 ? '共 ' + cartTotalQuantity + ' 件' : '还没有选商品' }}</text>
			</view>
			<view v-if="cartTotalQuantity > 0" class="cart-btn" @tap="goConfirm">
				<text>去结算</text>
			</view>
		</view>

		<bottom-tab-bar current="scan" />

		<product-video-player ref="videoPlayer" />
	</view>
</template>

<script>
import { scanMenuApi, scanCartApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getLocalUserId, ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'
import { showError, showSuccess } from '@/utils/ui-feedback.js'
import ImmersiveProductCard from '@/components/immersive-product-card.vue'
import ProductVideoPlayer from '@/components/product-video-player.vue'

const SCAN_MENU_CONTEXT_KEY = 'scanMenuEntryContext'

function toNumber(value) {
	const n = Number(value || 0)
	return Number.isFinite(n) ? n : 0
}

export default {
	components: {
		ImmersiveProductCard,
		ProductVideoPlayer
	},
	data() {
		return {
			shopId: 1,
			tableNo: '',
			shopName: '咖啡门店',
			products: [],
			loading: false,
			loadError: '',
			initialized: false,
			flippedKey: null,
			cartList: [],
			cartTotalQuantity: 0,
			cartTotalAmount: '0.00',
			cartExpanded: false,
			updatingCartId: null,
			flyDots: [],
			flyDotSeq: 0
		}
	},
	computed: {
		navTableText() {
			return this.tableNo ? this.tableNo + '桌' : ''
		}
	},
	onLoad(options = {}) {
		if (Object.keys(options || {}).length) {
			this.resolveScanContext(options)
		} else {
			this.applyStoredScanContext()
		}
		this.loadAllProducts()
		this.loadCartList()
		this.initialized = true
	},
	onShow() {
		const contextChanged = this.applyStoredScanContext()
		if (this.initialized && (!this.products.length || contextChanged)) {
			this.loadAllProducts()
		}
		if (this.initialized) {
			this.loadCartList()
		}
	},
	onHide() {
		this.closeVideoIfOpen()
	},
	onUnload() {
		this.clearFlyDots()
		this.closeVideoIfOpen()
	},
	methods: {
		applyStoredScanContext() {
			const context = uni.getStorageSync(SCAN_MENU_CONTEXT_KEY)
			if (!context) {
				return false
			}
			uni.removeStorageSync(SCAN_MENU_CONTEXT_KEY)
			return this.resolveScanContext(context)
		},

		resolveScanContext(options = {}) {
			const previousShopId = this.shopId
			const previousTableNo = this.tableNo
			const previousShopName = this.shopName
			let shopId = options.shopId
			let tableNo = options.tableNo
			const scene = options.scene
			const shopName = options.shopName
			if (!shopId && !tableNo && scene) {
				try {
					const decoded = decodeURIComponent(String(scene))
					const parts = decoded.split('&')
					parts.forEach((part) => {
						const kv = part.split('=')
						if (kv[0] === 'shopId') shopId = kv[1]
						if (kv[0] === 'tableNo') tableNo = kv[1]
						if (kv[0] === 'shopName') this.shopName = decodeURIComponent(kv[1] || '') || this.shopName
					})
				} catch (error) {}
			}
			this.shopId = toNumber(shopId) || 1
			this.tableNo = tableNo ? String(tableNo).trim() : ''
			if (shopName) {
				this.shopName = decodeURIComponent(String(shopName))
			}
			return (
				this.shopId !== previousShopId ||
				this.tableNo !== previousTableNo ||
				this.shopName !== previousShopName
			)
		},
		authHeader() {
			const token = getToken()
			const header = { 'Content-Type': 'application/json' }
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},
		getList(body) {
			if (!body) return []
			if (Array.isArray(body)) return body
			if (body.data && Array.isArray(body.data)) return body.data
			if (body.rows && Array.isArray(body.rows)) return body.rows
			if (body.list && Array.isArray(body.list)) return body.list
			if (body.result && Array.isArray(body.result)) return body.result
			if (body.data && body.data.list && Array.isArray(body.data.list)) return body.data.list
			if (body.data && body.data.rows && Array.isArray(body.data.rows)) return body.data.rows
			if (body.data && body.data.result && Array.isArray(body.data.result)) return body.data.result
			return []
		},
		async loadProductsByCategory(categoryId) {
			const productParams = { categoryId, shopId: this.shopId }
			try {
				console.log('[scan/menu] 请求商品:', scanMenuApi.products, productParams)
				const res = await requestPromise({
					url: scanMenuApi.products,
					method: 'GET',
					data: productParams,
					header: this.authHeader()
				})
				console.log('[scan/menu] 商品响应 categoryId=' + categoryId + ':', res && res.data)
				if (!isSuccessResponse(res)) {
					console.warn('[scan/menu] 分类 ' + categoryId + ' 商品请求未成功:', res && res.data && res.data.msg)
					return []
				}
				return this.getList((res && res.data) || {}) || []
			} catch (error) {
				console.error('[scan/menu] 分类 ' + categoryId + ' 商品请求异常:', error)
				return []
			}
		},
		async loadAllProducts() {
			this.loading = true
			this.loadError = ''
			try {
				console.log('[scan/menu] 请求分类:', scanMenuApi.categories)
				const catRes = await requestPromise({
					url: scanMenuApi.categories,
					method: 'GET'
				})
				console.log('[scan/menu] 分类响应:', catRes && catRes.data)
				if (!isSuccessResponse(catRes)) {
					this.products = []
					this.loadError = (catRes && catRes.data && catRes.data.msg) || '分类加载失败'
					return
				}
				const categories = this.getList((catRes && catRes.data) || {})
				if (!categories.length) {
					this.products = []
					this.loadError = '暂无商品分类'
					return
				}
				const all = []
				const seen = {}
				for (let i = 0; i < categories.length; i++) {
					const cat = categories[i] || {}
					const categoryId = cat.categoryId || cat.id
					if (!categoryId && categoryId !== 0) continue
					const list = await this.loadProductsByCategory(categoryId)
					list.forEach((item) => {
						if (!item) return
						const key = item.productId || item.id || item.product_id
						if (key === undefined || key === null || key === '') {
							all.push(item)
							return
						}
						const seenKey = String(key)
						if (!seen[seenKey]) {
							seen[seenKey] = true
							all.push(item)
						}
					})
				}
				this.products = all
				if (!all.length) {
					this.loadError = '暂无可点商品'
				}
				console.log('[scan/menu] 合并后商品总数:', all.length)
			} catch (error) {
				console.error('[scan/menu] 商品加载异常:', error)
				this.products = []
				this.loadError = '商品加载失败：' + ((error && error.errMsg) || (error && error.message) || '网络异常')
			} finally {
				this.loading = false
			}
		},
		money(value) {
			return toNumber(value).toFixed(2)
		},
		shortName(name) {
			return String(name || '咖啡').slice(0, 2)
		},
		closeVideoIfOpen() {
			if (this.$refs.videoPlayer && this.$refs.videoPlayer.visible) {
				this.$refs.videoPlayer.close()
			}
		},
		productKey(product) {
			if (!product) return null
			return product.productId || product.id || product.product_id || null
		},
		isFlipped(product) {
			const key = this.productKey(product)
			return key !== null && this.flippedKey === key
		},
		onCardFlip(product) {
			const key = this.productKey(product)
			if (key === null) return
			this.flippedKey = key
		},
		onCardBackClose() {
			this.flippedKey = null
			this.closeVideoIfOpen()
		},
		onPlayVideo(payload) {
			const url = payload && payload.videoUrl
			if (!url) return
			this.$refs.videoPlayer && this.$refs.videoPlayer.open(url)
		},
		onProductAdded(payload) {
			if (!payload || payload.action !== 'decrease') {
				this.playFlyCartAnimation(payload && payload.flyFrom)
			}
			this.loadCartList()
		},
		getCartBagPoint() {
			return new Promise((resolve) => {
				uni.createSelectorQuery()
					.in(this)
					.select('.cart-bag-target')
					.boundingClientRect(function (rect) {
						if (!rect) {
							resolve(null)
							return
						}
						resolve({
							x: rect.left + rect.width / 2,
							y: rect.top + rect.height / 2
						})
					})
					.exec()
			})
		},
		buildFlyDotStyle(dot) {
			return 'left:' + Number(dot.left || 0) + 'px;'
				+ 'top:' + Number(dot.top || 0) + 'px;'
				+ 'opacity:' + Number(dot.opacity || 0) + ';'
				+ 'transform:translate3d(' + Number(dot.moveX || 0) + 'px, '
				+ Number(dot.moveY || 0) + 'px, 0) scale(' + Number(dot.scale || 0.72) + ');'
		},
		clearFlyDots() {
			this.flyDots.forEach(function (dot) {
				;(dot.timers || []).forEach(function (timer) {
					clearTimeout(timer)
				})
			})
			this.flyDots = []
		},
		updateFlyDot(id, patch) {
			this.flyDots = this.flyDots.map((dot) => {
				if (dot.id !== id) return dot
				const next = Object.assign({}, dot, patch)
				next.style = this.buildFlyDotStyle(next)
				return next
			})
		},
		playFlyCartAnimation(flyFrom) {
			this.getCartBagPoint().then((bagPoint) => {
				const fallbackFrom = {
					x: uni.getSystemInfoSync().windowWidth * 0.72,
					y: uni.getSystemInfoSync().windowHeight * 0.55
				}
				const fallbackTo = {
					x: 74,
					y: uni.getSystemInfoSync().windowHeight - 112
				}
				const from = flyFrom || fallbackFrom
				const to = bagPoint || fallbackTo
				const dotHalf = uni.getSystemInfoSync().windowWidth * 27 / 750
				const startX = from.x - dotHalf
				const startY = from.y - dotHalf
				const jumpY = -34
				const flyX = to.x - from.x
				const flyY = to.y - from.y - 14
				const id = ++this.flyDotSeq
				const dot = {
					id: id,
					left: startX,
					top: startY,
					moveX: 0,
					moveY: 0,
					opacity: 0,
					scale: 0.72,
					timers: [],
					style: ''
				}
				dot.style = this.buildFlyDotStyle(dot)
				this.flyDots.push(dot)
				this.$nextTick(() => {
					const timers = [
						setTimeout(() => {
							this.updateFlyDot(id, {
								moveX: 0,
								moveY: jumpY,
								opacity: 1,
								scale: 1.08
							})
						}, 40),
						setTimeout(() => {
							this.updateFlyDot(id, {
								moveX: flyX,
								moveY: flyY,
								opacity: 1,
								scale: 0.46
							})
						}, 260),
						setTimeout(() => {
							this.updateFlyDot(id, {
								opacity: 0,
								scale: 0.24
							})
						}, 1280),
						setTimeout(() => {
							this.flyDots = this.flyDots.filter(function (item) {
								return item.id !== id
							})
						}, 1500)
					]
					this.updateFlyDot(id, { timers: timers })
				})
			})
		},
		async onQuickAdd(product) {
			if (!product) return
			if (!ensureLocalLogin('请先登录后再加入购物车')) return
			try {
				const res = await requestPromise({
					url: scanCartApi.add,
					method: 'POST',
					header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
					data: {
						shopId: this.shopId,
						tableNo: this.tableNo,
						productId: product.productId,
						productName: product.productName,
						productImage: product.imageUrl || product.productImage || '',
						price: product.price || 0,
						quantity: 1,
						specText: product.remark || product.description || '',
						specJson: '',
						selected: 1,
						status: 1
					}
				})
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '加入购物车失败')
					return
				}
				showSuccess('已加入购物车')
				this.loadCartList()
			} catch (error) {
				showError('加入购物车失败')
			}
		},
		getCartId(item) {
			return item && (item.id || item.cartId)
		},
		async loadCartList() {
			const userId = getLocalUserId()
			if (!userId) {
				this.cartList = []
				this.cartTotalQuantity = 0
				this.cartTotalAmount = '0.00'
				this.cartExpanded = false
				return
			}
			try {
				const res = await requestPromise({
					url: scanCartApi.list,
					method: 'GET',
					data: { shopId: this.shopId, tableNo: this.tableNo },
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) {
					this.cartList = []
					this.cartTotalQuantity = 0
					this.cartTotalAmount = '0.00'
					return
				}
				const data = (res.data && res.data.data) || res.data || {}
				let list = []
				let quantity = 0
				let amount = 0
				if (Array.isArray(data.list)) list = data.list
				else if (Array.isArray(data.items)) list = data.items
				else if (Array.isArray(data)) list = data
				if (data.totalQuantity !== undefined) {
					quantity = toNumber(data.totalQuantity)
				} else {
					list.forEach((item) => { quantity += toNumber(item.quantity) })
				}
				if (data.totalAmount !== undefined) {
					amount = toNumber(data.totalAmount)
				} else {
					list.forEach((item) => { amount += toNumber(item.price) * toNumber(item.quantity) })
				}
				this.cartList = list.map((item, index) => ({
					...item,
					cartKey: String(item.id || item.cartId || index),
					productImageUrl: resolveImageUrl(item.productImage || item.imageUrl || item.productImg || '')
				}))
				this.cartTotalQuantity = quantity
				this.cartTotalAmount = amount.toFixed(2)
				if (quantity <= 0) {
					this.cartExpanded = false
				}
			} catch (error) {
				this.cartList = []
				this.cartTotalQuantity = 0
				this.cartTotalAmount = '0.00'
				this.cartExpanded = false
			}
		},
		toggleCart() {
			if (this.cartTotalQuantity <= 0) {
				showError('请先选择商品')
				return
			}
			this.cartExpanded = !this.cartExpanded
		},
		async updateCartQuantity(item, quantity) {
			const cartId = this.getCartId(item)
			if (!cartId || this.updatingCartId) return
			this.updatingCartId = cartId
			try {
				const res = await requestPromise({
					url: scanCartApi.update,
					method: 'PUT',
					header: this.authHeader(),
					data: { id: cartId, quantity }
				})
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '更新失败')
				}
			} catch (error) {
				showError('更新失败')
			} finally {
				this.updatingCartId = null
				this.loadCartList()
			}
		},
		async removeCartItem(item) {
			const cartId = this.getCartId(item)
			if (!cartId || this.updatingCartId) return
			this.updatingCartId = cartId
			try {
				const res = await requestPromise({
					url: scanCartApi.delete + cartId,
					method: 'DELETE',
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '删除失败')
				}
			} catch (error) {
				showError('删除失败')
			} finally {
				this.updatingCartId = null
				this.loadCartList()
			}
		},
		decreaseCartItem(item) {
			const quantity = toNumber(item && item.quantity)
			if (quantity <= 1) {
				this.removeCartItem(item)
				return
			}
			this.updateCartQuantity(item, quantity - 1)
		},
		increaseCartItem(item) {
			this.updateCartQuantity(item, toNumber(item && item.quantity) + 1)
		},
		goConfirmAfterAdd() {
			const url = `/pages/scan/confirm?shopId=${this.shopId}&tableNo=${encodeURIComponent(this.tableNo || '')}&shopName=${encodeURIComponent(this.shopName || '')}`
			uni.navigateTo({ url })
		},
		goConfirm() {
			if (this.cartTotalQuantity <= 0) {
				showError('请先选择商品')
				return
			}
			const url = `/pages/scan/confirm?shopId=${this.shopId}&tableNo=${encodeURIComponent(this.tableNo || '')}&shopName=${encodeURIComponent(this.shopName || '')}`
			uni.navigateTo({ url })
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background:
		radial-gradient(circle at 18% 8%, rgba(244, 210, 139, 0.28), transparent 34%),
		radial-gradient(circle at 88% 22%, rgba(111, 78, 55, 0.22), transparent 30%),
		linear-gradient(180deg, #2b211c 0%, #6f4e37 42%, #f4eee8 100%);
	box-sizing: border-box;
	overflow: hidden;
}

.error-bar {
	margin: 0 $space-page 16rpx;
	padding: 18rpx 20rpx;
	border-radius: $radius-sm;
	background: rgba(200, 110, 96, 0.1);
	border: 2rpx solid rgba(200, 110, 96, 0.16);
}

.error-text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	color: #b45548;
}

.immersive-shell {
	flex: 1;
	min-height: 0;
	position: relative;
	padding-top: 0;
}

.product-scroll {
	width: 100%;
	height: 100%;
	box-sizing: border-box;
	position: relative;
	z-index: 2;
}

.product-list {
	padding: 0 24rpx 224rpx;
	display: flex;
	flex-direction: column;
	gap: 28rpx;
	box-sizing: border-box;
}

.product-list-item {
	width: 100%;
	flex-shrink: 0;
}

.page-progress {
	display: none;
}

.empty-main {
	width: 100%;
	height: 100%;
	min-height: 600rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	color: $text-tertiary;
	gap: 12rpx;
}

.empty-main-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.empty-main-subtitle {
	font-family: $font-family;
	font-size: 22rpx;
	color: $text-tertiary;
}

.cart-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: $bottom-nav-shell-height;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
	height: 112rpx;
	padding: 14rpx $space-page;
	background: rgba(255, 255, 255, 0.92);
	border-top: 2rpx solid rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	z-index: 25;
	box-sizing: border-box;
}

.cart-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: calc(#{$bottom-nav-shell-height} + 112rpx);
	background: rgba(32, 26, 23, 0.32);
	backdrop-filter: blur(6rpx);
	-webkit-backdrop-filter: blur(6rpx);
	z-index: 23;
}

.cart-drawer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: calc(#{$bottom-nav-shell-height} + 112rpx);
	max-height: 560rpx;
	padding: 22rpx $space-page;
	border-radius: $radius-md $radius-md 0 0;
	background: rgba(255, 255, 255, 0.96);
	border-top: 2rpx solid rgba(255, 255, 255, 0.82);
	box-shadow: 0 -22rpx 56rpx rgba(32, 26, 23, 0.18);
	backdrop-filter: blur(22rpx);
	-webkit-backdrop-filter: blur(22rpx);
	z-index: 24;
	box-sizing: border-box;
}

.fly-cart-dot {
	position: fixed;
	width: 54rpx;
	height: 54rpx;
	border-radius: 50%;
	background: $accent-primary;
	color: #ffffff;
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 40;
	pointer-events: none;
	box-shadow: 0 12rpx 28rpx rgba(111, 78, 55, 0.24);
	will-change: transform, opacity;
	transition: transform 1.08s cubic-bezier(0.2, 0.82, 0.18, 1),
		opacity 0.22s ease-out;
}

.fly-cart-dot text {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 800;
	line-height: 1;
}

.cart-drawer-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
	padding-bottom: 16rpx;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.72);
}

.cart-drawer-title-wrap {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
	min-width: 0;
}

.cart-drawer-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.cart-drawer-subtitle,
.cart-drawer-spec {
	font-family: $font-family;
	font-size: 21rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.cart-drawer-close {
	flex-shrink: 0;
	// 扩大点击热区,避免只有 "收起" 两个字可点
	display: inline-block;
	padding: 12rpx 18rpx;
	font-family: $font-family;
	font-size: 23rpx;
	font-weight: 600;
	color: $accent-primary;
}

.cart-drawer-list {
	max-height: 440rpx;
	margin-top: 8rpx;
}

.cart-drawer-empty {
	height: 180rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-tertiary;
}

.cart-drawer-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	padding: 18rpx 0;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.52);
}

.cart-drawer-image {
	width: 86rpx;
	height: 86rpx;
	border-radius: $radius-sm;
	background: $accent-surface;
	flex-shrink: 0;
}

.cart-drawer-image-empty {
	display: flex;
	align-items: center;
	justify-content: center;
}

.cart-drawer-image-empty text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: rgba(111, 78, 55, 0.72);
}

.cart-drawer-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.cart-drawer-name {
	font-family: $font-family;
	font-size: 25rpx;
	font-weight: 700;
	color: $text-primary;
	// 微信小程序里 <text> 默认是 inline,需要显式设为 block 才能让 ellipsis 生效
	display: block;
	width: 100%;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.cart-drawer-price {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 700;
	color: $text-primary;
}

.cart-drawer-actions {
	display: flex;
	align-items: center;
	gap: 14rpx;
	flex-shrink: 0;
}

.cart-step-btn {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	border: 2rpx solid rgba(111, 78, 55, 0.18);
	background: $bg-card;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.cart-step-btn-add {
	background: $accent-primary;
	border-color: $accent-primary;
}

.cart-step-btn text {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
	line-height: 1;
	color: $accent-primary;
}

.cart-step-btn-add text {
	color: #ffffff;
}

.cart-step-num {
	min-width: 30rpx;
	font-family: $font-family;
	font-size: 25rpx;
	font-weight: 700;
	text-align: center;
	color: $text-primary;
}

.cart-icon {
	width: 84rpx;
	height: 84rpx;
	border-radius: 50%;
	background: $accent-primary;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 14rpx 30rpx rgba(111, 78, 55, 0.2);
	flex-shrink: 0;
	@include active-press;
}

.cart-icon-empty {
	background: rgba(111, 78, 55, 0.28);
	box-shadow: none;
}

.cart-icon text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: #ffffff;
}

.cart-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.cart-count {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.cart-amount {
	font-family: $font-family;
	font-size: 36rpx;
	font-weight: 700;
	color: $text-primary;
}

.cart-btn {
	height: 84rpx;
	padding: 0 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	min-width: 220rpx;
	@include active-press;
}

.cart-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #ffffff;
}
</style>
