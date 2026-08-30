<template>
	<view class="page" :style="themePageStyle">
		<app-nav
			class="scan-nav"
			title="点单"
			:show-back="false"
			:left-text="navTableText"
			:show-toggle="true"
			:toggle-value="layoutMode"
			@toggle="handleLayoutToggle"
		/>

		<view class="immersive-shell">
			<view v-if="!products.length && !loading" class="menu-state">
				<view class="menu-state-card" :class="{ 'menu-state-error': !!loadError }">
					<view class="menu-state-icon">
						<text v-if="loadError">!</text>
						<image v-else class="menu-state-image" src="/static/empty-product.svg" mode="aspectFit"></image>
					</view>
					<text class="menu-state-title">{{ loadError ? '菜单加载失败' : '暂无可点商品' }}</text>
					<text class="menu-state-desc">{{ loadError ? loadError : '门店菜单暂未上架，请稍后再来看看。' }}</text>
				</view>
			</view>

			<scroll-view
				v-if="products.length && layoutMode === 'single'"
				class="product-scroll product-scroll-single"
				scroll-y
				:enhanced="true"
				:bounces="false"
				:show-scrollbar="false"
				:scroll-into-view="singleScrollIntoView"
				:scroll-with-animation="true"
				@scroll="onSingleScroll"
			>
				<view class="product-list product-list-single">
					<view
						v-for="(prod, index) in products"
						:key="index"
						:id="'single-product-' + index"
						class="product-list-item product-list-item-single"
						:class="[`theme-product-card-${themeComponent('productCard').variant || 'vertical'}`, {
							'product-list-item-current': index === singleCurrentIndex,
							'product-list-item-last': index === products.length - 1
						}]"
						:style="[singleCardStyle, themeBackgroundStyle('productCard'), themeSkinAssetStyle('productCard')]"
					>
						<immersive-product-card
							:product="prod"
							:flipped="isFlipped(prod)"
							:active-card="isFlipped(prod)"
							:table-no="tableNo"
							:image-override="themeSkinProductImage(prod.productId || prod.id)"
							:theme-variant="themeComponent('productCard').variant || 'vertical'"
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

			<scroll-view
				v-else-if="products.length"
				class="product-scroll"
				scroll-y
				:show-scrollbar="false"
			>
				<view class="product-list product-list-double">
					<view
						v-for="(prod, index) in products"
						:key="index"
						class="product-list-item"
						:class="`theme-product-card-${themeComponent('productCard').variant || 'vertical'}`"
						:style="[themeBackgroundStyle('productCard'), themeSkinAssetStyle('productCard')]"
					>
						<immersive-product-card
							:product="prod"
							:flipped="false"
							:active-card="isFlipped(prod)"
							:table-no="tableNo"
							:image-override="themeSkinProductImage(prod.productId || prod.id)"
							:compact="true"
							:theme-variant="themeComponent('productCard').variant || 'vertical'"
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

		<view v-if="cartExpanded" class="cart-mask" :style="bottomNavStyle" @tap="toggleCart"></view>

		<view v-if="activeLayerProduct" class="product-flip-mask" @tap="onCardBackClose"></view>
		<view
			v-if="activeLayerProduct"
			class="product-flip-layer"
			:class="{ 'product-flip-layer-open': flipLayerOpen }"
			:style="flipLayerStyle"
		>
			<immersive-product-card
				:product="activeLayerProduct"
				:flipped="flipLayerFlipped"
				:active-card="true"
				:table-no="tableNo"
				:image-override="themeSkinProductImage(activeLayerProduct.productId || activeLayerProduct.id)"
				:theme-variant="themeComponent('productCard').variant || 'vertical'"
				@close="onCardBackClose"
				@play-video="onPlayVideo"
				@added="onProductAdded"
				@checkout="goConfirmAfterAdd"
			/>
		</view>

		<view v-if="cartExpanded" class="cart-drawer" :style="bottomNavStyle">
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

		<view class="cart-bar" :style="bottomNavStyle">
			<view class="cart-icon cart-bag-target" :class="{ 'cart-icon-empty': cartTotalQuantity <= 0 }" @tap="toggleCart">
				<image class="cart-bag-image" src="/static/home/shop.svg" mode="aspectFit"></image>
				<text v-if="cartTotalQuantity > 0" class="cart-badge">{{ cartBadgeText }}</text>
			</view>
			<view class="cart-copy" @tap="toggleCart">
				<text class="cart-amount">¥{{ cartTotalAmount }}</text>
				<text class="cart-count">{{ cartTotalQuantity > 0 ? '共 ' + cartTotalQuantity + ' 件' : '还没有选商品' }}</text>
			</view>
			<view v-if="cartTotalQuantity > 0" class="cart-btn" @tap="goConfirm">
				<text>去结算</text>
			</view>
		</view>

		<!-- #ifdef H5 -->
		<bottom-tab-bar current="scan" />
		<!-- #endif -->

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
import { themeRuntime } from '@/theme/runtime.js'

const SCAN_MENU_CONTEXT_KEY = 'scanMenuEntryContext'
// The mini-program keeps only the native custom TabBar. H5 keeps the embedded
// tab bar and measures its actual height below.
const BOTTOM_NAV_SHELL_RPX = 88
const BOTTOM_NAV_GAP_RPX = 0
const DEFAULT_BOTTOM_NAV_OFFSET_RPX = 88

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
			tableNo: '',
			shopName: '咖啡门店',
			storeCode: '',
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
			flyDotSeq: 0,
			layoutMode: 'single',
			flipLayerFlipped: false,
			flipLayerOpen: false,
			flipLayerOriginStyle: '',
			singleCurrentIndex: 0,
			singleItemStride: 1,
			singleScrollTimer: null,
			singleScrollIntoView: '',
			singleScrollTop: 0,
			singleCardHeight: 0,
			bottomNavOffset: 0,
			decoratorPreview: false
		}
	},
	computed: {
		bottomNavStyle() {
			return {
				'--bottom-nav-offset': this.bottomNavOffset > 0
					? `${this.bottomNavOffset}px`
					: `${DEFAULT_BOTTOM_NAV_OFFSET_RPX}rpx`
			}
		},
		navTableText() {
			return this.tableNo ? this.tableNo + '桌' : ''
		},
		activeLayerProduct() {
			if (this.layoutMode !== 'double' || this.flippedKey === null) {
				return null
			}
			return this.products.find((product) => this.productKey(product) === this.flippedKey) || null
		},
		flipLayerStyle() {
			return this.flipLayerOpen ? '' : this.flipLayerOriginStyle
		},
		cartBadgeText() {
			return this.cartTotalQuantity > 99 ? '99+' : String(this.cartTotalQuantity)
		},
		singleCardStyle() {
			// 测量完成前用空样式，回退到 CSS 占位高度；测量后用实测像素高度
			// 同时写 min-height，否则 class 里的 min-height 会盖过较小的内联 height
			if (this.singleCardHeight > 0) {
				return `height: ${this.singleCardHeight}px; min-height: ${this.singleCardHeight}px;`
			}
			return ''
		}
	},
	async onLoad(options = {}) {
		this.decoratorPreview = this.themePreviewMode || String(options.decoratorPreview || '') === '1'
		if (Object.keys(options || {}).length) {
			this.resolveScanContext(options)
		} else {
			this.applyStoredScanContext()
		}
		if (!this.decoratorPreview) await themeRuntime.loadPublished(this.storeCode)
		this.loadAllProducts()
		this.loadCartList()
		this.initialized = true
	},
	onReady() {
		this.measureBottomNav()
	},
	async onShow() {
		this.measureBottomNav()
		const contextChanged = this.applyStoredScanContext()
		if (contextChanged && !this.decoratorPreview) {
			await themeRuntime.loadPublished(this.storeCode)
		}
		if (this.initialized && (!this.products.length || contextChanged)) {
			this.loadAllProducts()
		}
		if (this.initialized) {
			this.loadCartList()
		}
	},
	onHide() {
		this.clearSingleScrollTimer()
		this.closeVideoIfOpen()
	},
	onUnload() {
		this.clearSingleScrollTimer()
		this.clearFlyDots()
		this.closeVideoIfOpen()
	},
	methods: {
		getBottomNavMinimumOffset() {
			const systemInfo = typeof uni.getWindowInfo === 'function'
				? uni.getWindowInfo()
				: (uni.getSystemInfoSync ? uni.getSystemInfoSync() : {})
			const windowWidth = Number(systemInfo.windowWidth || 375)
			const rpxRatio = windowWidth / 750
			const safeAreaInsets = systemInfo.safeAreaInsets || {}
			let safeBottom = Number(safeAreaInsets.bottom)
			if (!Number.isFinite(safeBottom)) {
				const screenHeight = Number(systemInfo.screenHeight || systemInfo.windowHeight || 0)
				const safeAreaBottom = Number(systemInfo.safeArea && systemInfo.safeArea.bottom)
				if (screenHeight > 0 && Number.isFinite(safeAreaBottom)) {
					safeBottom = Math.max(0, screenHeight - safeAreaBottom)
				}
			}
			let safeAreaOffset = Number.isFinite(safeBottom) ? safeBottom : 0
			// WeChat's native custom TabBar already includes the device safe-area
			// region, so adding it again would lift the checkout bar too far upward.
			// #ifdef MP-WEIXIN
			safeAreaOffset = 0
			// #endif
			return Math.ceil((BOTTOM_NAV_SHELL_RPX + BOTTOM_NAV_GAP_RPX) * rpxRatio + safeAreaOffset)
		},
		measureBottomNav() {
			const minimumOffset = this.getBottomNavMinimumOffset()
			if (this.bottomNavOffset < minimumOffset) {
				this.bottomNavOffset = minimumOffset
			}
			this.$nextTick(() => {
				const query = uni.createSelectorQuery().in(this)
				query.select('.bottom-nav').boundingClientRect((rect) => {
					if (!rect || !Number.isFinite(rect.top)) return
					const systemInfo = uni.getSystemInfoSync ? uni.getSystemInfoSync() : {}
					const viewportHeight = Number(systemInfo.windowHeight || systemInfo.screenHeight || 0)
					if (viewportHeight <= 0) return
					const measuredOffset = Math.max(0, Math.ceil(viewportHeight - rect.top) + 2)
					const offset = Math.max(minimumOffset, measuredOffset)
					if (offset > 0 && offset !== this.bottomNavOffset) this.bottomNavOffset = offset
				}).exec()
			})
		},
		applyStoredScanContext() {
			const context = uni.getStorageSync(SCAN_MENU_CONTEXT_KEY)
			if (!context) {
				return false
			}
			uni.removeStorageSync(SCAN_MENU_CONTEXT_KEY)
			return this.resolveScanContext(context)
		},

		handleLayoutToggle(mode) {
			this.clearSingleScrollTimer()
			this.layoutMode = mode
			this.singleCurrentIndex = 0
			this.singleScrollTop = 0
			this.singleScrollIntoView = ''
			this.flippedKey = null
			this.flipLayerFlipped = false
			this.flipLayerOpen = false
			this.flipLayerOriginStyle = ''
			this.closeVideoIfOpen()
			// 切回单列时 scroll-view 由 v-if 重建，需重新测量卡片步距，否则吸附失效
			if (mode === 'single') {
				this.$nextTick(this.measureSingleStride)
			}
		},

		resolveScanContext(options = {}) {
			const previousTableNo = this.tableNo
			const previousShopName = this.shopName
			const previousStoreCode = this.storeCode
			let tableNo = options.tableNo
			let storeCode = options.storeCode
			const scene = options.scene
			const shopName = options.shopName
			if ((!tableNo || !storeCode) && scene) {
				try {
					const decoded = decodeURIComponent(String(scene))
					const parts = decoded.split('&')
					parts.forEach((part) => {
						const kv = part.split('=')
						if (kv[0] === 'tableNo') tableNo = kv[1]
						if (kv[0] === 'storeCode') storeCode = kv[1]
						if (kv[0] === 'shopName') this.shopName = decodeURIComponent(kv[1] || '') || this.shopName
					})
				} catch (error) {}
			}
			this.tableNo = tableNo ? String(tableNo).trim() : ''
			this.storeCode = storeCode ? String(storeCode).trim() : ''
			if (shopName) {
				this.shopName = decodeURIComponent(String(shopName))
			}
			return (
				this.tableNo !== previousTableNo ||
				this.shopName !== previousShopName ||
				this.storeCode !== previousStoreCode
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
		reloadProducts() {
			this.loadAllProducts()
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
			const productParams = { categoryId }
			try {
				const res = await requestPromise({
					url: scanMenuApi.products,
					method: 'GET',
					data: productParams,
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) {
					return []
				}
				return this.getList((res && res.data) || {}) || []
			} catch (error) {
				return []
			}
		},
		async loadAllProducts() {
			this.loading = true
			this.loadError = ''
			try {
				const catRes = await requestPromise({
					url: scanMenuApi.categories,
					method: 'GET'
				})
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
				if (this.singleCurrentIndex >= all.length) {
					this.singleCurrentIndex = 0
				}
				this.clearSingleScrollTimer()
				this.$nextTick(this.measureSingleStride)
			} catch (error) {
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
		clearSingleScrollTimer() {
			if (this.singleScrollTimer) {
				clearTimeout(this.singleScrollTimer)
				this.singleScrollTimer = null
			}
		},
		measureSingleStride(retry = 1) {
			const sysInfo = uni.getSystemInfoSync()
			// rpx 转 px：1rpx = 屏幕宽度px / 750
			const rpxRatio = sysInfo.windowWidth / 750
			// 卡间距，需与 CSS .product-list-item-single 的 margin-bottom 保持一致
			const marginPx = 28 * rpxRatio
			// 视觉露出量：吸附到卡顶后，下一张卡只从底部露出一点
			const revealPx = 48 * rpxRatio

			const query = uni.createSelectorQuery().in(this)
			query.select('.product-scroll-single').boundingClientRect()
			query.exec((res) => {
				const container = res && res[0]
				if (container && container.height > 1) {
					// 卡片高度 = 容器高度 - 间距 - 露出量，让下一张卡顶部露出
					this.singleCardHeight = Math.floor(container.height - marginPx - revealPx)
					// stride = 卡片高度 + 间距，由几何关系确定，无需二次重排测量
					this.singleItemStride = this.singleCardHeight + marginPx
				} else if (retry > 0) {
					// 布局尚未结算，boundingClientRect 返回 0，稍后重试一次
					setTimeout(() => this.measureSingleStride(retry - 1), 150)
				}
			})
		},
		onSingleScroll(event) {
			const scrollTop = Number(event && event.detail && event.detail.scrollTop) || 0
			this.singleScrollTop = scrollTop
			this.clearSingleScrollTimer()
			// 滚动停止 100ms 后吸附到最近的卡片
			this.singleScrollTimer = setTimeout(() => {
				this.snapToNearestSingle()
			}, 100)
		},
		snapToNearestSingle() {
			if (!this.products.length || this.singleItemStride <= 1) return
			const index = Math.max(0, Math.min(
				this.products.length - 1,
				Math.round(this.singleScrollTop / this.singleItemStride)
			))
			this.singleCurrentIndex = index
			// 已基本对齐则不再触发，避免程序滚动反复回弹
			const offset = Math.abs(this.singleScrollTop - index * this.singleItemStride)
			if (offset < 4) return
			// scroll-into-view 设置相同 id 不会重复触发，需先置空再下一帧设置
			this.singleScrollIntoView = ''
			this.$nextTick(() => {
				this.singleScrollIntoView = 'single-product-' + index
			})
		},
		isFlipped(product) {
			const key = this.productKey(product)
			return key !== null && this.flippedKey === key
		},
		buildFlipLayerOriginStyle(rect) {
			const systemInfo = uni.getSystemInfoSync()
			const screenWidth = Number(systemInfo.windowWidth || 375)
			const screenHeight = Number(systemInfo.windowHeight || 667)
			const layerWidth = Math.max(screenWidth - 32, 1)
			const layerHeight = Math.max(Math.min(screenHeight * 0.76, 490), 360)
			const originX = rect ? (Number(rect.left || 0) + Number(rect.width || 0) / 2) : screenWidth / 2
			const originY = rect ? (Number(rect.top || 0) + Number(rect.height || 0) / 2) : screenHeight / 2
			const centerX = screenWidth / 2
			const centerY = screenHeight / 2
			const scaleX = rect && rect.width ? Number(rect.width) / layerWidth : 0.46
			const scaleY = rect && rect.height ? Number(rect.height) / layerHeight : 0.46
			const scale = Math.max(Math.min(scaleX, scaleY, 0.72), 0.28)
			return 'transform: translate3d('
				+ (originX - centerX) + 'px, '
				+ (originY - centerY) + 'px, 0) translateY(-50%) scale('
				+ scale + '); opacity: 0.2;'
		},
		onCardFlip(payload) {
			const product = payload && payload.product ? payload.product : payload
			const key = this.productKey(product)
			if (key === null) return
			if (this.layoutMode === 'single') {
				this.flippedKey = key
				return
			}
			this.flipLayerFlipped = false
			this.flipLayerOpen = false
			this.flipLayerOriginStyle = this.buildFlipLayerOriginStyle(payload && payload.rect)
			this.flippedKey = key
			this.$nextTick(() => {
				setTimeout(() => {
					this.flipLayerOpen = true
				}, 20)
				setTimeout(() => {
					this.flipLayerFlipped = true
				}, 260)
			})
		},
		onCardBackClose() {
			this.flipLayerOpen = false
			this.flipLayerFlipped = false
			this.flipLayerOriginStyle = ''
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
					y: uni.getSystemInfoSync().windowHeight - 188
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
		async onQuickAdd(payload) {
			const product = payload && payload.product ? payload.product : payload
			const flyFrom = payload && payload.flyFrom ? payload.flyFrom : null
			if (!product) return
			if (!ensureLocalLogin('请先登录后再加入购物车')) return
			try {
				const res = await requestPromise({
					url: scanCartApi.add,
					method: 'POST',
					header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
					data: {
						tableNo: this.tableNo,
						productId: product.productId,
						productName: product.productName,
						productImage: this.themeSkinProductImage(product.productId || product.id)
							|| product.imageUrl || product.productImage || '',
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
				this.playFlyCartAnimation(flyFrom)
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
					data: { tableNo: this.tableNo },
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
			const url = `/pages/scan/confirm?tableNo=${encodeURIComponent(this.tableNo || '')}`
			uni.navigateTo({ url })
		},
		goConfirm() {
			if (this.cartTotalQuantity <= 0) {
				showError('请先选择商品')
				return
			}
			const url = `/pages/scan/confirm?tableNo=${encodeURIComponent(this.tableNo || '')}`
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
	background: var(--theme-page);
	box-sizing: border-box;
	overflow: hidden;
}

.immersive-shell {
	flex: 1;
	min-height: 0;
	position: relative;
	padding-top: 0;
	overflow: hidden;
}

.scan-nav {
	margin-bottom: 0;
}

.product-scroll {
	width: 100%;
	height: 100%;
	box-sizing: border-box;
	position: relative;
	z-index: 2;
}

.product-list {
	padding: 0 24rpx calc(280rpx + env(safe-area-inset-bottom));
	display: flex;
	flex-direction: column;
	gap: 28rpx;
	box-sizing: border-box;
}

.product-scroll-single {
	height: calc(100% - #{$bottom-nav-shell-height} - 112rpx - env(safe-area-inset-bottom));
	-webkit-overflow-scrolling: touch;
}

.product-list-single {
	/* 底部留出 28rpx 卡间距 + 48rpx 露出量，保证最后一张能滚动贴顶，避免吸附目标不可达导致的颤动 */
	padding: 0 24rpx calc(76rpx + env(safe-area-inset-bottom));
	gap: 0;
}

.product-list-item-single {
	/* 略矮于滚动容器，吸附到卡顶后自然露出下一张顶部一截 */
	min-height: calc(100vh - 360rpx);
	margin-bottom: 28rpx;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

.product-list-item-single.product-list-item-last {
	margin-bottom: 0;
}

.product-list-item-single ::v-deep .immersive-card-wrap {
	align-items: stretch;
	justify-content: flex-start;
	width: 100%;
	height: 100%;
}

.product-list-item-single ::v-deep .card-body {
	flex-shrink: 0;
}

.product-list-double {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 14rpx;
	padding: 20rpx 28rpx calc(280rpx + env(safe-area-inset-bottom));
}

.product-list-item {
	width: 100%;
	flex-shrink: 0;
}

.page-progress {
	display: none;
}

.menu-state {
	width: 100%;
	height: 100%;
	min-height: 600rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 32rpx $space-page calc(#{$bottom-nav-shell-height} + 160rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.menu-state-card {
	width: 100%;
	min-height: 520rpx;
	padding: 32rpx 24rpx;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	text-align: center;
	gap: 12rpx;
	border-radius: $radius-sm;
	background: $bg-card;
	border: 2rpx solid $border-subtle;
	box-shadow: $shadow-card;
	box-sizing: border-box;
}

.menu-state-error {
	background: $bg-card;
	border-color: rgba(200, 110, 96, 0.28);
}

.menu-state-icon {
	width: 200rpx;
	height: 200rpx;
	margin-bottom: 12rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.menu-state-icon text {
	font-family: $font-family;
	font-size: 42rpx;
	font-weight: 800;
	color: $accent-primary;
	line-height: 1;
}

.menu-state-image {
	width: 200rpx;
	height: 200rpx;
	display: block;
}

.menu-state-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
	line-height: 1.3;
}

.menu-state-desc {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
	line-height: 1.6;
	word-break: break-all;
}

.cart-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: var(--bottom-nav-offset, 88rpx);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
	height: 112rpx;
	padding: 14rpx $space-page;
	background: #FFFFFF;
	border-top: 2rpx solid rgba(232, 224, 215, 0.72);
	z-index: 25;
	box-sizing: border-box;
}

.cart-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: calc(var(--bottom-nav-offset, 88rpx) + 112rpx);
	background: rgba(32, 26, 23, 0.32);
	z-index: 23;
}

.product-flip-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	background: rgba(32, 26, 23, 0.5);
	z-index: 80;
}

.product-flip-layer {
	position: fixed;
	left: 16rpx;
	right: 16rpx;
	top: 50%;
	height: 76vh;
	max-height: 980rpx;
	min-height: 720rpx;
	z-index: 85;
	perspective: 1500rpx;
	transform-origin: center center;
	transition:
		transform 0.28s cubic-bezier(0.18, 0.78, 0.2, 1),
		opacity 0.2s ease-out;
	will-change: transform, opacity;
}

.product-flip-layer-open {
	transform: translate3d(0, 0, 0) translateY(-50%) scale(1);
	opacity: 1;
}

.product-flip-layer ::v-deep .immersive-card-wrap,
.product-flip-layer ::v-deep .flip-card,
.product-flip-layer ::v-deep .flip-card-inner {
	height: 100%;
}

.product-flip-layer ::v-deep .immersive-card-wrap {
	transform: none;
	filter: none;
}

.product-flip-layer ::v-deep .front-card-measure {
	height: 100%;
	min-height: 100%;
}

.product-flip-layer ::v-deep .card-image-wrap {
	flex: 1;
	min-height: 0;
}

.product-flip-layer ::v-deep .card-image {
	height: 100%;
	object-fit: cover;
}

.product-flip-layer ::v-deep .card-body {
	flex-shrink: 0;
}

.cart-drawer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: calc(var(--bottom-nav-offset, 88rpx) + 112rpx);
	max-height: 560rpx;
	padding: 22rpx $space-page 18rpx;
	border-radius: $radius-md $radius-md 0 0;
	background: #FFFFFF;
	border-top: 2rpx solid rgba(122, 79, 45, 0.12);
	border-bottom: 0;
	box-shadow: 0 -12rpx 28rpx rgba(36, 24, 19, 0.14);
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
	box-shadow: 0 8rpx 18rpx rgba(111, 78, 55, 0.18);
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
	position: relative;
	width: 86rpx;
	height: 86rpx;
	border-radius: 50%;
	background: #FFFFFF;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 6rpx 14rpx rgba(32, 26, 23, 0.1);
	flex-shrink: 0;
	@include active-press;
}

.cart-icon-empty {
	box-shadow: 0 8rpx 18rpx rgba(32, 26, 23, 0.1);
}

.cart-bag-image {
	width: 54rpx;
	height: 54rpx;
	display: block;
}

.cart-badge {
	position: absolute;
	right: -4rpx;
	top: -6rpx;
	min-width: 30rpx;
	height: 30rpx;
	padding: 0 8rpx;
	border-radius: 999rpx;
	background: $accent-danger;
	border: 3rpx solid #ffffff;
	font-family: $font-family;
	font-size: 21rpx;
	font-weight: 800;
	line-height: 30rpx;
	text-align: center;
	color: #ffffff;
	box-sizing: border-box;
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
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.cart-amount {
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 800;
	color: $text-primary;
}

.cart-btn {
	height: 84rpx;
	padding: 0 34rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 22rpx;
	background: $accent-primary;
	box-shadow: 0 10rpx 22rpx rgba(122, 79, 45, 0.2);
	min-width: 220rpx;
	@include active-press;
}

.cart-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 700;
	color: #FFFFFF;
}

</style>
