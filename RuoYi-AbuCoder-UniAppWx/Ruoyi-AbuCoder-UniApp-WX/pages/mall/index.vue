<template>
	<view class="page">
		<app-nav title="咖啡豆与周边" />

		<view class="content">
			<view class="content-wrap">
				<view class="category-shell" :style="{ top: categoryStickyTop + 'px' }">
					<view class="category-inline">
						<scroll-view
							v-if="floorSections.length"
							class="category-scroll"
							scroll-x
							scroll-with-animation
							:scroll-left="categoryScrollLeft"
							:scroll-into-view="categoryScrollIntoView"
							:show-scrollbar="false"
						>
							<view class="category-row">
								<view
									v-for="(item, index) in floorSections"
									:id="getCategoryChipDomId(item)"
									:key="getCategoryRenderKey(item, index)"
									class="category-chip"
									:class="{ 'category-chip-active': isCategoryActive(item) }"
									@click="handleCategoryTabByIndex(index)"
								>
									<text class="category-chip-text">{{ item.categoryName }}</text>
								</view>
							</view>
						</scroll-view>
					</view>
				</view>

				<view
					v-if="floorSections.length"
					class="floor-list"
				>
					<view
						v-for="(section, sectionIndex) in floorSections"
						:id="section.anchorId"
						:key="getSectionRenderKey(section, sectionIndex)"
						class="floor-section"
					>
						<view class="floor-header">
							<view class="floor-copy">
								<text class="floor-title">{{ section.categoryName }}</text>
								<text class="floor-subtitle">{{ getSectionSubtitle(section) }}</text>
							</view>
						</view>

						<view v-if="section.products.length" class="product-list">
							<view
								v-for="(item, index) in section.products"
								:key="getProductRenderKey(section, item, index)"
								class="product-card"
								@tap="goDetailByIndex(sectionIndex, index)"
							>
								<image
									v-if="getProductImage(item, index)"
									class="product-image"
									:src="getProductImage(item, index)"
									mode="aspectFill"
									@error="handleProductImageErrorByIndex(sectionIndex, index)"
								></image>
								<view v-else class="product-image product-image-empty">
									<text>暂无图片</text>
								</view>
								<view class="product-info">
									<view class="product-top">
										<text class="product-name">{{ item.productName }}</text>
										<text class="product-meta">{{ getProductMetaText(item) }}</text>
									</view>
									<view v-if="hasProductPromotion(item)" class="product-promo">
										<text
											v-for="promoTag in getProductPromotionTags(item)"
											:key="`${resolveProductId(item) || index}-${promoTag}`"
											class="product-promo-tag"
										>
											{{ promoTag }}
										</text>
									</view>
									<view class="product-bottom">
										<view class="price-block">
											<text class="product-price">¥{{ formatMoney(getProductSalePrice(item)) }}</text>
											<text
												v-if="showOriginPrice(item)"
												class="product-origin-price"
											>
												¥{{ formatMoney(item.price) }}
											</text>
										</view>

										<view class="cart-stepper">
											<view
												v-if="getCartQuantity(item) > 0"
												class="stepper-wrap"
											>
												<view
													class="stepper-btn"
													:class="{ 'stepper-btn-disabled': isProductMutating(item) }"
													@tap.stop="decreaseCartQuantityByIndex(sectionIndex, index)"
												>
													<view class="stepper-icon stepper-icon-minus"></view>
												</view>
												<text class="stepper-count" :class="{ 'stepper-count-busy': isProductMutating(item) }">
													{{ isProductMutating(item) ? '...' : getCartQuantity(item) }}
												</text>
												<view
													class="stepper-btn stepper-btn-primary"
													:class="{ 'stepper-btn-disabled': isProductMutating(item) }"
													@tap.stop="increaseCartQuantityByIndex(sectionIndex, index)"
												>
													<view class="stepper-icon stepper-icon-plus stepper-icon-light"></view>
												</view>
											</view>
											<view
												v-else
												class="stepper-add"
												:class="{ 'stepper-btn-disabled': isProductMutating(item) }"
												@tap.stop="increaseCartQuantityByIndex(sectionIndex, index)"
											>
												<view class="stepper-icon stepper-icon-plus stepper-icon-light"></view>
											</view>
										</view>
									</view>
								</view>
							</view>
						</view>
						<view v-else class="floor-empty">
							<text class="floor-empty-text">当前分类暂无商品</text>
						</view>
					</view>
				</view>

				<view v-else class="product-empty-state">
					<view class="empty-card">
						<image class="empty-image" src="/static/empty-product.svg" mode="aspectFit"></image>
						<text class="empty-title">暂无可展示的商品</text>
						<text class="empty-desc">
							商品上架后会在这里按分类展示。
						</text>
					</view>
				</view>

				<view class="content-bottom-space"></view>
			</view>
		</view>

	</view>
</template>

<script>
import {
	baseUrl,
	productApi,
	cartApi,
	resolveImageUrl,
	isLoopbackBaseUrl,
	getApiBaseUrlHint
} from '@/utils/apiconfig.js'
import { ensureLocalLogin, getLocalUserId } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError } from '@/utils/ui-feedback.js'

const PRODUCT_PREVIEW_KEY = 'currentProductPreview'
const FALLBACK_CATEGORY_ID = 'uncategorized'

function sortCategoryList(list = []) {
	return [...list].sort((a, b) => {
		const sortA = Number(a && a.sortOrder !== undefined && a.sortOrder !== null ? a.sortOrder : Number.MAX_SAFE_INTEGER)
		const sortB = Number(b && b.sortOrder !== undefined && b.sortOrder !== null ? b.sortOrder : Number.MAX_SAFE_INTEGER)
		if (sortA !== sortB) {
			return sortA - sortB
		}
		return Number((a && a.categoryId) || 0) - Number((b && b.categoryId) || 0)
	})
}

function clonePlainData(value) {
	try {
		return JSON.parse(JSON.stringify(value || {}))
	} catch (error) {
		return value || {}
	}
}

function toNumber(value) {
	const number = Number(value || 0)
	return Number.isFinite(number) ? number : 0
}

export default {
	data() {
		return {
			categoryList: [],
			allProductList: [],
			cartList: [],
			currentCategoryId: '',
			productImageErrorMap: {},
			pendingProductMap: {},
			apiConfigHintShown: false,
			sectionMetrics: [],
			sectionMeasureTimer: null,
			categoryTapLockTimer: null,
			navOffsetTop: 0,
			categoryFixedHeight: 0,
			categoryStickyTop: 0,
			categoryScrollLeft: 0,
			categoryScrollIntoView: '',
			pendingCategoryId: ''
		}
	},

	computed: {
		floorSections() {
			const categoryMap = {}
			const sortedCategories = sortCategoryList(this.categoryList)

			sortedCategories.forEach((item) => {
				if (!item || item.categoryId === undefined || item.categoryId === null || !item.categoryName) {
					return
				}
				categoryMap[String(item.categoryId)] = {
					categoryId: item.categoryId,
					categoryName: item.categoryName,
					categoryKey: String(item.categoryId),
					anchorId: `floor-${item.categoryId}`,
					chipId: `category-chip-${item.categoryId}`,
					products: []
				}
			})

			this.allProductList.forEach((item) => {
				const rawCategoryId = this.getProductCategoryId(item)
				const normalizedCategoryId =
					rawCategoryId === null || rawCategoryId === undefined || rawCategoryId === ''
						? FALLBACK_CATEGORY_ID
						: String(rawCategoryId)

				if (!categoryMap[normalizedCategoryId]) {
					categoryMap[normalizedCategoryId] = {
						categoryId: normalizedCategoryId,
						categoryName: item.categoryName || '其他分类',
						categoryKey: normalizedCategoryId,
						anchorId: `floor-${normalizedCategoryId}`,
						chipId: `category-chip-${normalizedCategoryId}`,
						products: []
					}
				}

				categoryMap[normalizedCategoryId].products.push(item)
			})

			const orderedSections = sortedCategories
				.map((item) => categoryMap[String(item.categoryId)])
				.filter(Boolean)

			Object.keys(categoryMap).forEach((key) => {
				const current = categoryMap[key]
				if (!current || current.products.length === 0) {
					return
				}
				if (!orderedSections.find((item) => item && item.categoryKey === current.categoryKey)) {
					orderedSections.push(current)
				}
			})

			return orderedSections.filter(Boolean)
		},

	},

	onLoad() {
		this.navOffsetTop = this.resolveNavOffsetTop()
		this.categoryStickyTop = this.navOffsetTop
		this.loadHomeData()
	},

	onShow() {
		this.loadCartList({ silent: true, noLoading: true })
		this.$nextTick(() => {
			this.scheduleSectionMeasure()
		})
	},

	onPullDownRefresh() {
		this.loadHomeData({
			stopRefresh: true
		})
	},

	onPageScroll(event) {
		const scrollTop = event && event.scrollTop ? event.scrollTop : 0
		this.syncActiveCategoryByScroll(scrollTop)
	},

	beforeDestroy() {
		if (this.sectionMeasureTimer) {
			clearTimeout(this.sectionMeasureTimer)
		}
		if (this.categoryTapLockTimer) {
			clearTimeout(this.categoryTapLockTimer)
		}
	},

	methods: {
		resolveNavOffsetTop() {
			let statusBarHeight = 0
			if (typeof uni.getWindowInfo === 'function') {
				statusBarHeight = Number(uni.getWindowInfo().statusBarHeight || 0)
			} else {
				statusBarHeight = Number(uni.getSystemInfoSync().statusBarHeight || 0)
			}
			return statusBarHeight + uni.upx2px(60)
		},

		formatMoney(value) {
			return toNumber(value).toFixed(2)
		},

		getCategoryDomKey(item) {
			if (!item) {
				return ''
			}
			return String(item.categoryKey || item.categoryId || item.anchorId || item.chipId || '')
		},

		getCategoryChipDomId(item, layer = 'inline') {
			return `${layer}-category-chip-${this.getCategoryDomKey(item)}`
		},

		getCategoryRenderKey(item, index, layer = 'inline') {
			const domKey = this.getCategoryDomKey(item)
			return `${layer}-${domKey || index}`
		},

		getSectionRenderKey(section, index) {
			const domKey = this.getCategoryDomKey(section)
			return `section-${domKey || index}`
		},

		getSectionSubtitle(section) {
			const productCount = Array.isArray(section && section.products) ? section.products.length : 0
			return productCount ? `共 ${productCount} 件商品` : '暂无商品'
		},

		getProductMetaParts(item) {
			if (!item) {
				return []
			}

			const origin = item.origin || item.productOrigin || item.area || item.region || ''
			const processingMethod =
				item.processingMethod ||
				item.processMethod ||
				item.processing ||
				item.process ||
				item.handlingMethod ||
				''
			const flavorNotes =
				item.flavorNotes ||
				item.flavor ||
				item.flavourNotes ||
				item.taste ||
				item.tastingNotes ||
				''

			return [origin, processingMethod, flavorNotes]
				.map((part) => String(part || '').trim())
				.filter(Boolean)
		},

		getProductMetaText(item) {
			const parts = this.getProductMetaParts(item)
			return parts.length ? parts.join(' / ') : '精选产区 / 处理法清晰 / 风味干净'
		},

		getProductRenderKey(section, item, index) {
			const sectionKey = this.getCategoryDomKey(section) || 'section'
			const productId = this.resolveProductId(item)
			return productId ? `${sectionKey}-${productId}` : `${sectionKey}-${index}`
		},

		getProductCategoryId(item) {
			if (!item) {
				return null
			}
			return (
				item.categoryId ||
				item.category_id ||
				item.categoryID ||
				item.categoryid ||
				item.CategoryId ||
				null
			)
		},

		handleRequestFailure(scene, payload, options = {}) {
			const fallbackMessage = options.fallbackMessage || `${scene}失败`
			const responseMessage = payload && payload.data && payload.data.msg ? payload.data.msg : ''
			const networkMessage = payload && payload.errMsg ? payload.errMsg : ''
			const message = responseMessage || fallbackMessage

			console.error(`[index] ${scene} failed`, {
				baseUrl,
				payload
			})

			if (!options.silent) {
				showError(message)
			}

			if (!this.apiConfigHintShown && isLoopbackBaseUrl() && /request:fail|connect|timeout|socket/i.test(networkMessage)) {
				this.apiConfigHintShown = true
				uni.showModal({
					title: '接口地址需要调整',
					content: getApiBaseUrlHint(baseUrl),
					showCancel: false
				})
			}
		},

		async loadHomeData(options = {}) {
			if (!options.noLoading) {
				showBusy('加载中...')
			}

			try {
				await Promise.all([
					this.loadCategories({ silent: true }),
					this.loadProducts({ silent: true }),
					this.loadCartList({ silent: true, noLoading: true })
				])
				this.$nextTick(() => {
					this.scheduleSectionMeasure(true)
				})
			} finally {
				if (!options.noLoading) {
					hideBusy()
				}
				if (options.stopRefresh) {
					uni.stopPullDownRefresh()
				}
			}
		},

		async loadCategories(options = {}) {
			try {
				const res = await requestPromise({
					url: productApi.categories,
					method: 'GET'
				})
				if (isSuccessResponse(res)) {
					const categoryList = res.data.data || []
					this.categoryList = sortCategoryList(categoryList.filter((item) => item && item.categoryName))
					return
				}
				this.categoryList = []
				this.handleRequestFailure('加载分类', res, {
					fallbackMessage: '分类加载失败',
					silent: options.silent
				})
			} catch (error) {
				this.categoryList = []
				this.handleRequestFailure('加载分类', error, {
					fallbackMessage: '分类加载失败',
					silent: options.silent
				})
			}
		},

		async loadProducts(options = {}) {
			const requestData = {
				pageNum: 1,
				pageSize: 1000
			}
			const userId = getLocalUserId()
			if (userId) {
				requestData.userId = userId
			}

			try {
				const res = await requestPromise({
					url: productApi.list,
					method: 'GET',
					data: requestData
				})
				if (isSuccessResponse(res)) {
					this.allProductList = res.data.rows || []
					return
				}
				this.allProductList = []
				this.handleRequestFailure('加载商品', res, {
					fallbackMessage: '商品加载失败',
					silent: options.silent
				})
			} catch (error) {
				this.allProductList = []
				this.handleRequestFailure('加载商品', error, {
					fallbackMessage: '商品加载失败',
					silent: options.silent
				})
			}
		},

		async loadCartList(options = {}) {
			const userId = getLocalUserId()
			if (!userId) {
				this.cartList = []
				return
			}

			try {
				const res = await requestPromise({
					url: cartApi.list,
					method: 'GET',
					data: {
						userId
					}
				})
				if (isSuccessResponse(res)) {
					this.cartList = res.data.data || []
					return
				}
				this.cartList = []
				this.handleRequestFailure('加载购物车', res, {
					fallbackMessage: '购物车加载失败',
					silent: options.silent
				})
			} catch (error) {
				this.cartList = []
				this.handleRequestFailure('加载购物车', error, {
					fallbackMessage: '购物车加载失败',
					silent: options.silent
				})
			}
		},

		resolveProductId(item) {
			if (!item) {
				return null
			}
			return (
				item.productId ||
				item.product_id ||
				item.productID ||
				item.productid ||
				item.ProductId ||
				item.id ||
				item.goodsId ||
				item.goods_id ||
				null
			)
		},

		getProductKey(item, index) {
			if (!item) {
				return `product-${index}`
			}
			return this.resolveProductId(item) || item.productName || item.imageUrl || `product-${index}`
		},

		getProductImage(item, index = 0) {
			const productKey = this.getProductKey(item, index)
			if (this.productImageErrorMap[productKey]) {
				return ''
			}
			return resolveImageUrl(item.productImg || item.imageUrl)
		},

		getFloorSectionByIndex(sectionIndex) {
			return this.floorSections[sectionIndex] || null
		},

		getFloorProduct(sectionIndex, productIndex) {
			const section = this.getFloorSectionByIndex(sectionIndex)
			if (!section || !Array.isArray(section.products)) {
				return null
			}
			return section.products[productIndex] || null
		},

		handleCategoryTabByIndex(index) {
			this.handleCategoryTab(this.floorSections[index] || null)
		},

		handleProductImageErrorByIndex(sectionIndex, productIndex) {
			this.handleProductImageError(this.getFloorProduct(sectionIndex, productIndex), productIndex)
		},

		goDetailByIndex(sectionIndex, productIndex) {
			this.goDetail(this.getFloorProduct(sectionIndex, productIndex))
		},

		handleProductImageError(item, index) {
			const productKey = this.getProductKey(item, index)
			if (!this.productImageErrorMap[productKey]) {
				this.$set(this.productImageErrorMap, productKey, true)
			}
		},

		scheduleSectionMeasure(resetCurrent = false) {
			if (this.sectionMeasureTimer) {
				clearTimeout(this.sectionMeasureTimer)
			}
			this.sectionMeasureTimer = setTimeout(() => {
				this.measureSections(resetCurrent)
			}, 80)
		},

		measureSections(resetCurrent = false) {
			if (!this.floorSections.length) {
				this.sectionMetrics = []
				this.currentCategoryId = ''
				return
			}

			const query = uni.createSelectorQuery().in(this)
			query.select('.category-inline').boundingClientRect()
			query.selectViewport().scrollOffset()
			query.selectAll('.floor-section').boundingClientRect()
			query.exec((result) => {
				const fixedRect = result && result[0] ? result[0] : null
				const viewport = result && result[1] ? result[1] : {}
				const sectionRects = result && Array.isArray(result[2]) ? result[2] : []

				if (fixedRect && fixedRect.height) {
					this.categoryFixedHeight = Number(fixedRect.height)
				}

				const scrollTop = Number(viewport.scrollTop || 0)
				this.sectionMetrics = sectionRects
					.map((rect, index) => {
						const current = this.floorSections[index]
						if (!rect || !current) {
							return null
						}
						return {
							categoryId: String(current.categoryId),
							top: Number(rect.top || 0) + scrollTop
						}
					})
					.filter(Boolean)

				this.syncActiveCategoryByScroll(scrollTop, resetCurrent)
			})
		},

		syncActiveCategoryByScroll(scrollTop = 0, force = false) {
			if (!this.floorSections.length) {
				this.currentCategoryId = ''
				return
			}

			if (!this.sectionMetrics.length) {
				this.setCurrentCategory(this.floorSections[0].categoryId, true)
				return
			}

			const anchorLine = Number(scrollTop || 0) + this.navOffsetTop + this.categoryFixedHeight + uni.upx2px(24)
			let activeMetric = this.sectionMetrics[0]

			this.sectionMetrics.forEach((item) => {
				if (anchorLine >= item.top) {
					activeMetric = item
				}
			})

			if (this.pendingCategoryId) {
				const pendingMetric = this.sectionMetrics.find(
					(item) => String(item.categoryId) === String(this.pendingCategoryId)
				)
				const hasReachedPendingSection = pendingMetric && anchorLine >= pendingMetric.top - uni.upx2px(12)

				if (!hasReachedPendingSection) {
					this.setCurrentCategory(this.pendingCategoryId, true)
					return
				}

				this.pendingCategoryId = ''
			}

			if (activeMetric) {
				this.setCurrentCategory(activeMetric.categoryId, force)
			}
		},

		setCurrentCategory(categoryId, force = false) {
			const nextCategoryId = String(categoryId || '')
			if (!nextCategoryId) {
				return
			}
			if (!force && this.currentCategoryId === nextCategoryId) {
				return
			}
			this.currentCategoryId = nextCategoryId
			this.updateCategoryScrollIntoView()
		},

		resetCategoryScrollPosition(force = false) {
			this.categoryScrollIntoView = ''
			if (!force) {
				this.categoryScrollLeft = 0
				return
			}

			// Force the scroll-view to flush back to the start when the first chip becomes active again.
			this.categoryScrollLeft = 1
			this.$nextTick(() => {
				this.categoryScrollLeft = 0
			})
		},

		updateCategoryScrollIntoView() {
			if (!this.currentCategoryId) {
				this.resetCategoryScrollPosition()
				return
			}

			const firstSection = this.floorSections[0]
			if (firstSection && String(firstSection.categoryId) === String(this.currentCategoryId)) {
				this.resetCategoryScrollPosition(true)
				return
			}

			this.categoryScrollLeft = 0
			this.categoryScrollIntoView = this.getCategoryChipDomId({
				categoryKey: this.currentCategoryId
			})
		},

		isCategoryActive(item) {
			return !!(item && String(item.categoryId) === String(this.currentCategoryId))
		},

		handleCategoryTab(item) {
			if (!item) {
				return
			}
			this.pendingCategoryId = String(item.categoryId || '')
			this.setCurrentCategory(item.categoryId, true)
			if (this.categoryTapLockTimer) {
				clearTimeout(this.categoryTapLockTimer)
			}
			this.categoryTapLockTimer = setTimeout(() => {
				this.pendingCategoryId = ''
			}, 450)
			uni.pageScrollTo({
				selector: `#${item.anchorId}`,
				duration: 250,
				offsetTop: -(this.categoryStickyTop + this.categoryFixedHeight + uni.upx2px(18))
			})
		},

		getProductPromotionTags(item) {
			if (!item) {
				return []
			}
			const tags = []
			if (item.activityTag) {
				tags.push(item.activityTag)
			}
			if (Array.isArray(item.activityTags)) {
				item.activityTags.forEach((tag) => {
					if (tag && !tags.includes(tag)) {
						tags.push(tag)
					}
				})
			}
			return tags.slice(0, 1)
		},

		hasProductPromotion(item) {
			return this.getProductPromotionTags(item).length > 0
		},

		getProductSalePrice(item) {
			return toNumber(item && (item.salePrice || item.price))
		},

		showOriginPrice(item) {
			return toNumber(item && item.salePrice) > 0 && toNumber(item && item.salePrice) !== toNumber(item && item.price)
		},

		getCartItemPrice(item) {
			return toNumber(item && (item.salePrice || item.price))
		},

		getCartItemByProduct(item) {
			const productId = this.resolveProductId(item)
			if (!productId) {
				return null
			}
			return (
				this.cartList.find((cartItem) => String(cartItem.productId) === String(productId)) ||
				null
			)
		},

		getCartQuantity(item) {
			const cartItem = this.getCartItemByProduct(item)
			return cartItem ? toNumber(cartItem.quantity) : 0
		},

		isProductMutating(item) {
			const productId = this.resolveProductId(item)
			return !!(productId && this.pendingProductMap[String(productId)])
		},

		async increaseCartQuantity(item) {
			await this.changeCartQuantity(item, 1)
		},

		async decreaseCartQuantity(item) {
			await this.changeCartQuantity(item, -1)
		},

		async increaseCartQuantityByIndex(sectionIndex, productIndex) {
			await this.increaseCartQuantity(this.getFloorProduct(sectionIndex, productIndex))
		},

		async decreaseCartQuantityByIndex(sectionIndex, productIndex) {
			await this.decreaseCartQuantity(this.getFloorProduct(sectionIndex, productIndex))
		},

		async changeCartQuantity(item, delta) {
			const productId = this.resolveProductId(item)
			if (!productId) {
				showError('当前商品缺少编号')
				return
			}

			if (!ensureLocalLogin()) {
				return
			}

			const productKey = String(productId)
			if (this.pendingProductMap[productKey]) {
				return
			}

			this.$set(this.pendingProductMap, productKey, true)

			try {
				const cartItem = this.getCartItemByProduct(item)
				let success = false

				if (!cartItem && delta > 0) {
					success = await this.requestAddToCart(item)
				} else if (cartItem) {
					const nextQuantity = toNumber(cartItem.quantity) + delta
					if (nextQuantity <= 0) {
						success = await this.requestDeleteCartItem(cartItem.cartId)
					} else {
						success = await this.requestUpdateCartItem(cartItem, nextQuantity)
					}
				}

				if (success) {
					await this.loadCartList({ silent: true, noLoading: true })
				}
			} finally {
				this.$delete(this.pendingProductMap, productKey)
			}
		},

		async requestAddToCart(item) {
			try {
				const res = await requestPromise({
					url: cartApi.add,
					method: 'POST',
					data: {
						userId: getLocalUserId(),
						productId: this.resolveProductId(item),
						quantity: 1,
						spec: item.remark || ''
					}
				})
				if (isSuccessResponse(res)) {
					return true
				}
				showError((res.data && res.data.msg) || '加入购物车失败')
				return false
			} catch (error) {
				showError('加入购物车失败')
				return false
			}
		},

		async requestUpdateCartItem(cartItem, quantity) {
			try {
				const res = await requestPromise({
					url: cartApi.update,
					method: 'PUT',
					data: {
						cartId: cartItem.cartId,
						userId: getLocalUserId(),
						quantity,
						spec: cartItem.spec || ''
					}
				})
				if (isSuccessResponse(res)) {
					return true
				}
				showError((res.data && res.data.msg) || '更新购物车失败')
				return false
			} catch (error) {
				showError('更新购物车失败')
				return false
			}
		},

		async requestDeleteCartItem(cartId) {
			try {
				const res = await requestPromise({
					url: `${cartApi.delete + cartId}?userId=${getLocalUserId()}`,
					method: 'DELETE'
				})
				if (isSuccessResponse(res)) {
					return true
				}
				showError((res.data && res.data.msg) || '移出购物车失败')
				return false
			} catch (error) {
				showError('移出购物车失败')
				return false
			}
		},

		buildDetailUrl(item) {
			const productId = this.resolveProductId(item)
			const query = []
			if (productId) {
				query.push(`id=${encodeURIComponent(productId)}`)
			}
			if (item && item.productName) {
				query.push(`name=${encodeURIComponent(item.productName)}`)
			}
			if (item && (item.productImg || item.imageUrl)) {
				query.push(`image=${encodeURIComponent(item.productImg || item.imageUrl)}`)
			}
			if (item && item.price !== undefined && item.price !== null) {
				query.push(`price=${encodeURIComponent(item.price)}`)
			}
			if (item && item.salePrice !== undefined && item.salePrice !== null) {
				query.push(`salePrice=${encodeURIComponent(item.salePrice)}`)
			}
			if (item && item.origin) {
				query.push(`origin=${encodeURIComponent(item.origin)}`)
			}
			if (item && item.remark) {
				query.push(`remark=${encodeURIComponent(item.remark)}`)
			}
			if (item && item.activityTag) {
				query.push(`activityTag=${encodeURIComponent(item.activityTag)}`)
			}
			if (item && item.activitySummary) {
				query.push(`activitySummary=${encodeURIComponent(item.activitySummary)}`)
			}
			return `/pages/product/detail${query.length ? `?${query.join('&')}` : ''}`
		},

		goDetail(item) {
			if (!item) {
				return
			}
			const previewItem = clonePlainData(item)
			uni.setStorageSync(PRODUCT_PREVIEW_KEY, previewItem)
			uni.navigateTo({
				url: this.buildDetailUrl(previewItem)
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
}

.category-shell {
	position: sticky;
	z-index: 40;
	background: $bg-bottom;
	backdrop-filter: blur(20rpx);
	-webkit-backdrop-filter: blur(20rpx);
	margin-left: -$space-page;
	margin-right: -$space-page;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.72);
}

.category-inline {
	padding: 14rpx 0 16rpx;
	border-bottom: 0;
	box-sizing: border-box;
}

.category-inline {
	position: relative;
}

.category-scroll {
	width: 100%;
	white-space: nowrap;
}

.category-row {
	display: inline-flex;
	align-items: center;
	gap: 16rpx;
	padding: 0 $space-page;
	box-sizing: border-box;
}

.category-chip {
	min-width: 132rpx;
	min-height: 72rpx;
	padding: 0 28rpx;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border-radius: 12rpx;
	border: 2rpx solid rgba(111, 78, 55, 0.08);
	background: rgba(255, 255, 255, 0.94);
	box-shadow: 0 6rpx 18rpx rgba(32, 26, 23, 0.05);
	box-sizing: border-box;
	flex-shrink: 0;
	transition: background-color 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
	@include active-press;
}

.category-chip-active {
	background: $accent-primary-soft;
	border-color: rgba(111, 78, 55, 0.12);
	box-shadow: 0 10rpx 24rpx rgba(32, 26, 23, 0.08);
}

.category-chip-text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	line-height: 1;
	color: $text-secondary;
	white-space: nowrap;
	transition: color 0.24s ease;
}

.category-chip-active .category-chip-text {
	color: $text-primary;
}

.content {
	flex: 1;
	display: flex;
	flex-direction: column;
}

.content-wrap {
	flex: 1;
	padding: 10rpx $space-page 0;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.search-box {
	@include card(0 20rpx);
	min-height: 88rpx;
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.search-icon {
	width: 26rpx;
	height: 26rpx;
	position: relative;
	flex-shrink: 0;
}

.search-icon-circle {
	width: 18rpx;
	height: 18rpx;
	border: 3rpx solid $text-tertiary;
	border-radius: 50%;
}

.search-icon-line {
	position: absolute;
	right: 0;
	bottom: 2rpx;
	width: 10rpx;
	height: 3rpx;
	background: $text-tertiary;
	transform: rotate(45deg);
	transform-origin: center;
}

.search-input {
	flex: 1;
	height: 88rpx;
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 500;
	color: $text-primary;
}

.search-placeholder {
	color: $text-tertiary;
}

.search-clear {
	min-height: 88rpx;
	padding: 0 18rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $bg-muted;
	@include active-press;
}

.search-clear text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.floor-list {
	display: flex;
	flex-direction: column;
	gap: 28rpx;
}

.floor-section {
	display: flex;
	flex-direction: column;
	gap: 18rpx;
}

.floor-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 64rpx;
}

.floor-copy {
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.floor-title {
	font-family: $font-family;
	font-size: 36rpx;
	font-weight: 700;
	line-height: 1.15;
	letter-spacing: 0.5rpx;
	color: $text-primary;
}

.floor-subtitle {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 400;
	line-height: 1.3;
	color: $text-primary;
}

.floor-empty {
	@include card(28rpx 24rpx);
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 120rpx;
}

.floor-empty-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.product-list {
	display: flex;
	flex-wrap: wrap;
	align-items: stretch;
	justify-content: space-between;
	gap: 16rpx;
}

.product-card {
	@include card(8rpx);
	width: calc((100% - 16rpx) / 2);
	display: flex;
	flex-direction: column;
	gap: 6rpx;
	box-sizing: border-box;
	@include active-press;
}

.product-image {
	width: 100%;
	height: 256rpx;
	flex-shrink: 0;
	background: $accent-surface;
	display: block;
}

.product-image-empty {
	display: flex;
	align-items: center;
	justify-content: center;
	background: $bg-muted;
}

.product-image-empty text {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.product-info {
	display: flex;
	flex-direction: column;
	min-width: 0;
	gap: 4rpx;
	flex: 1;
}

.product-top {
	display: flex;
	flex-direction: column;
	gap: 2rpx;
}

.product-name {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 1.35;
	color: $text-primary;
	display: -webkit-box;
	overflow: hidden;
	text-overflow: ellipsis;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}

.product-meta {
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 500;
	line-height: 1.4;
	color: $text-secondary;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
	text-overflow: ellipsis;
}

.product-promo {
	display: flex;
	flex-wrap: wrap;
	gap: 4rpx;
}

.product-promo-tag {
	display: inline-block;
	padding: 2rpx 8rpx;
	border-radius: 999rpx;
	background: $accent-surface;
	border: 1rpx solid rgba(111, 78, 55, 0.1);
	font-family: $font-family;
	font-size: 16rpx;
	font-weight: 600;
	line-height: 1.2;
	color: $text-primary;
	max-width: 100%;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.product-bottom {
	margin-top: 0;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 6rpx;
	min-height: 52rpx;
}

.price-block {
	display: flex;
	align-items: baseline;
	flex-wrap: nowrap;
	gap: 6rpx;
	min-width: 0;
}

.product-price {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	line-height: 1.1;
	color: $text-primary;
	white-space: nowrap;
}

.product-origin-price {
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 500;
	line-height: 1.2;
	color: $text-tertiary;
	text-decoration: line-through;
	white-space: nowrap;
}

.cart-stepper {
	flex-shrink: 0;
}

.stepper-wrap {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.stepper-btn,
.stepper-add {
	width: 48rpx;
	height: 48rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	border: 2rpx solid $border-subtle;
	background: $bg-card;
	box-sizing: border-box;
	@include active-press;
}

.stepper-btn-primary,
.stepper-add {
	background: $accent-primary;
	border-color: $accent-primary;
}

.stepper-btn-disabled {
	@include disabled-state;
}

.stepper-icon {
	position: relative;
	width: 22rpx;
	height: 22rpx;
	color: $text-secondary;
	flex-shrink: 0;
}

.stepper-icon::before,
.stepper-icon::after {
	content: '';
	position: absolute;
	left: 50%;
	top: 50%;
	background: currentColor;
	border-radius: 999rpx;
	transform: translate(-50%, -50%);
}

.stepper-icon::before {
	width: 18rpx;
	height: 4rpx;
}

.stepper-icon-plus::after {
	width: 4rpx;
	height: 18rpx;
}

.stepper-icon-minus::after {
	display: none;
}

.stepper-icon-light {
	color: #FFFFFF;
}

.stepper-count {
	min-width: 28rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	text-align: center;
	color: $text-primary;
}

.stepper-count-busy {
	color: $text-tertiary;
}

.product-empty-state {
	flex: 1;
	display: flex;
}

.empty-card {
	@include card(32rpx 24rpx);
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: center;
	gap: 12rpx;
	align-items: center;
	text-align: center;
	min-height: 520rpx;
}

.empty-image {
	width: 200rpx;
	height: 200rpx;
	margin-bottom: 12rpx;
}

.empty-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.empty-desc {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.content-bottom-space {
	height: 32rpx;
}
</style>
