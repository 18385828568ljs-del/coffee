<template>
	<view class="page">
		<app-nav title="商品详情" right-text="购物车" fallback-url="/pages/index/index" @right="goCart" />

		<scroll-view class="content" scroll-y>
			<view class="image-card">
				<text class="image-badge">现烘</text>
				<image
					v-if="productImage"
					class="product-image"
					:src="productImage"
					mode="aspectFill"
				></image>
				<view v-else class="product-image image-empty"></view>
			</view>

			<view class="info-card">
				<view class="price-row">
					<text class="price-value">¥{{ productInfo.salePrice || productInfo.price || '0.00' }}</text>
					<text
						v-if="productInfo.salePrice && Number(productInfo.salePrice) !== Number(productInfo.price)"
						class="price-origin"
					>
						¥{{ productInfo.price }}
					</text>
				</view>
				<view v-if="hasPromotion" class="promo-row">
					<text
						v-for="promoTag in promotionTags"
						:key="promoTag"
						class="promo-tag"
					>
						{{ promoTag }}
					</text>
				</view>
				<text class="product-name">{{ productInfo.productName }}</text>
				<text class="product-desc">{{ productInfo.description || '按单现烘发货，适合手冲、意式与日常饮用。' }}</text>
				<view class="tag-row">
					<text class="tag-item">{{ productInfo.origin || '精选产区' }}</text>
					<text class="tag-item">{{ productInfo.roastLevel || '中度烘焙' }}</text>
					<text class="tag-item">{{ productInfo.processingMethod || '风味清晰' }}</text>
				</view>
			</view>

			<view class="spec-card">
				<text class="card-title">风味与参数</text>
				<view class="spec-row">
					<text class="spec-label">风味</text>
					<text class="spec-value">{{ productInfo.flavorNotes || '坚果、可可、果香' }}</text>
				</view>
				<view class="spec-row">
					<text class="spec-label">库存</text>
					<text class="spec-value">{{ productInfo.stock || 0 }}</text>
				</view>
				<view class="spec-row">
					<text class="spec-label">备注</text>
					<text class="spec-value">{{ productInfo.remark || '下单时可备注研磨需求与口味偏好。' }}</text>
				</view>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view class="bottom-bar">
			<view class="action-btn add-btn" :class="{ 'action-btn-disabled': addingToCart }" @click="addToCart">
				<text>{{ addingToCart ? '加入中...' : '加入购物车' }}</text>
			</view>
			<view class="action-btn buy-btn" @click="buyNow">
				<text>立即购买</text>
			</view>
		</view>
	</view>
</template>

<script>
import { productApi, cartApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { ensureLocalLogin, getLocalUserId } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError, showSuccess } from '@/utils/ui-feedback.js'

const ORDER_DRAFT_KEY = 'orderConfirmDraft'
const PRODUCT_PREVIEW_KEY = 'currentProductPreview'

function normalizePreviewProduct(value) {
	if (!value) {
		return {}
	}
	if (typeof value === 'string') {
		try {
			return JSON.parse(value)
		} catch (error) {
			return {}
		}
	}
	return value
}

function safeDecode(value) {
	if (!value) {
		return ''
	}
	try {
		return decodeURIComponent(value)
	} catch (error) {
		return value
	}
}

function compactPreviewData(value) {
	const source = value || {}
	const target = {}
	Object.keys(source).forEach((key) => {
		const current = source[key]
		if (current !== undefined && current !== null && current !== '') {
			target[key] = current
		}
	})
	return target
}

export default {
	data() {
		return {
			productId: null,
			productInfo: {},
			addingToCart: false
		}
	},

	computed: {
		productImage() {
			return resolveImageUrl(this.productInfo.productImg || this.productInfo.imageUrl)
		},

		hasPromotion() {
			return this.promotionTags.length > 0
		},

		promotionTags() {
			const info = this.productInfo || {}
			const tags = []
			if (info.activityTag) {
				tags.push(info.activityTag)
			}
			if (Array.isArray(info.activityTags)) {
				info.activityTags.forEach((tag) => {
					if (tag && !tags.includes(tag)) {
						tags.push(tag)
					}
				})
			}
			return tags
		}
	},

	onLoad(options) {
		const optionPreview = compactPreviewData({
			productName: safeDecode(options.name),
			productImg: safeDecode(options.image),
			imageUrl: safeDecode(options.image),
			price: options.price || '',
			salePrice: options.salePrice || '',
			activityTag: safeDecode(options.activityTag),
			activitySummary: safeDecode(options.activitySummary),
			origin: safeDecode(options.origin),
			remark: safeDecode(options.remark)
		})
		const productId = options.id || options.productId
		if (optionPreview.productName || optionPreview.productImg) {
			this.productInfo = {
				...this.productInfo,
				...optionPreview
			}
		}
		if (productId) {
			this.productId = productId
			this.loadProductDetail()
			return
		}
		const previewProduct = compactPreviewData(normalizePreviewProduct(uni.getStorageSync(PRODUCT_PREVIEW_KEY)))
		const mergedPreview = {
			...previewProduct,
			...optionPreview
		}
		if (mergedPreview && Object.keys(mergedPreview).length) {
			this.productInfo = mergedPreview
			this.productId = this.resolveProductId(mergedPreview)
			if (this.productId) {
				this.loadProductDetail()
				return
			}
			if (mergedPreview.productName) {
				this.resolveProductFromName(mergedPreview)
				return
			}
			return
		}
		showError('缺少商品信息')
	},

	methods: {
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

		async loadProductDetail() {
			showBusy('加载中...')
			const requestData = {}
			const userId = getLocalUserId()
			if (userId) {
				requestData.userId = userId
			}
			try {
				const res = await requestPromise({
					url: productApi.detail + this.productId,
					method: 'GET',
					data: requestData
				})
				if (!isSuccessResponse(res) || !res.data.data) {
					showError((res.data && res.data.msg) || '商品不存在')
					setTimeout(() => {
						uni.navigateBack()
					}, 1200)
					return
				}
				this.productInfo = res.data.data || {}
			} catch (error) {
				showError('加载失败')
			} finally {
				hideBusy()
			}
		},

		async resolveProductFromName(previewProduct) {
			showBusy('加载中...')
			try {
				const res = await requestPromise({
					url: productApi.search,
					method: 'GET',
					data: {
						keyword: previewProduct.productName,
						pageNum: 1,
						pageSize: 20
					}
				})
				const rows = (res && res.data && res.data.rows) || []
				const matchedItem = rows.find((item) => {
					if (!item) {
						return false
					}
					if (item.productName !== previewProduct.productName) {
						return false
					}
					if (!previewProduct.imageUrl && !previewProduct.productImg) {
						return true
					}
					const previewImage = previewProduct.productImg || previewProduct.imageUrl
					const currentImage = item.productImg || item.imageUrl
					return previewImage === currentImage
				}) || rows[0]

				const productId = this.resolveProductId(matchedItem)
				if (productId) {
					this.productId = productId
					hideBusy()
					this.loadProductDetail()
					return
				}
				showError('缺少商品信息')
			} catch (error) {
				showError('缺少商品信息')
			} finally {
				hideBusy()
			}
		},

		addToCart() {
			if (!this.productId) {
				showError('当前商品缺少编号')
				return Promise.resolve(false)
			}
			if (!ensureLocalLogin()) {
				return Promise.resolve(false)
			}
			if (this.addingToCart) {
				return Promise.resolve(false)
			}

			this.addingToCart = true

			return new Promise((resolve) => {
				requestPromise({
					url: cartApi.add,
					method: 'POST',
					data: {
						userId: getLocalUserId(),
						productId: this.productId,
						quantity: 1,
						spec: this.productInfo.remark || ''
					}
				})
					.then((res) => {
						if (isSuccessResponse(res)) {
							showSuccess('已加入购物车')
							resolve(true)
							return
						}

						showError((res.data && res.data.msg) || '加入失败')
						resolve(false)
					})
					.catch(() => {
						showError('加入失败')
						resolve(false)
					})
					.finally(() => {
						this.addingToCart = false
					})
			})
		},

		buyNow() {
			if (!this.productId) {
				showError('当前商品暂无法购买')
				return
			}
			if (!ensureLocalLogin()) {
				return
			}

			uni.setStorageSync(ORDER_DRAFT_KEY, {
				source: 'buyNow',
				cartIds: [],
				items: [{
					productId: this.productId,
					productName: this.productInfo.productName,
					productImage: this.productImage,
					price: this.productInfo.price,
					salePrice: this.productInfo.salePrice || this.productInfo.price,
					quantity: 1,
					spec: this.productInfo.remark || ''
				}],
				remark: ''
			})

			uni.navigateTo({
				url: '/pages/order/confirm'
			})
		},

		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/index/index'
			})
		},

		goCart() {
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

.image-card {
	@include card(24rpx);
	position: relative;
	margin-bottom: 24rpx;
}

.image-badge {
	position: absolute;
	top: 40rpx;
	left: 40rpx;
	z-index: 1;
	height: 44rpx;
	padding: 0 14rpx;
	display: flex;
	align-items: center;
	border-radius: $radius-sm;
	background: $accent-primary-soft;
	border: 2rpx solid rgba(111, 78, 55, 0.12);
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $accent-primary;
}

.product-image {
	width: 100%;
	height: 460rpx;
	background: $accent-surface;
}

.image-empty {
	background: $bg-muted;
}

.info-card,
.spec-card {
	@include card(24rpx);
	margin-bottom: 24rpx;
}

.price-row {
	display: flex;
	align-items: baseline;
	gap: 12rpx;
}

.promo-row {
	margin-top: 16rpx;
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 12rpx;
}

.price-value {
	font-family: $font-family;
	font-size: 48rpx;
	font-weight: 600;
	color: $text-primary;
}

.price-origin {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
	text-decoration: line-through;
}

.promo-tag {
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background: $accent-surface;
	border: 1rpx solid rgba(111, 78, 55, 0.12);
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	line-height: 1.2;
	color: $accent-primary;
}

.product-name {
	display: block;
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 600;
	line-height: 1.3;
	color: $text-primary;
}

.product-desc,
.notice-text {
	display: block;
	margin-top: 16rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.tag-row {
	margin-top: 20rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
}

.tag-item {
	padding: 10rpx 16rpx;
	border-radius: $radius-sm;
	background: $accent-surface;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $accent-primary;
	border: 1rpx solid rgba(111, 78, 55, 0.08);
}

.card-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.spec-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
	margin-top: 20rpx;
	padding-bottom: 18rpx;
	border-bottom: 2rpx solid $border-light;
}

.spec-row:last-child {
	padding-bottom: 0;
	border-bottom: 0;
}

.spec-label,
.spec-value {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.spec-value {
	text-align: right;
	color: $text-primary;
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
	gap: 16rpx;
	padding: 16rpx $space-page calc(24rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	box-sizing: border-box;
}

.action-btn {
	flex: 1;
	min-height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	@include active-press;
}

.action-btn-disabled {
	@include disabled-state;
}

.action-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
}

.add-btn {
	background: $bg-card;
	border: 2rpx solid $border-subtle;
}

.add-btn text {
	color: $text-primary;
}

.buy-btn {
	background: $accent-primary;
}

.buy-btn text {
	color: #FFFFFF;
}
</style>
