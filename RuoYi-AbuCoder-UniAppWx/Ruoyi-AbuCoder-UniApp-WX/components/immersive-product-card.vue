<template>
	<view class="immersive-card-wrap" :class="[`theme-${themeVariant}`, { 'is-compact': compact, 'is-active-card': visualActive, 'is-fill': fillHeight }]">
		<view class="flip-card" :class="{ 'is-flipped-card': keepFlipFrame }" :style="activeCardHeightStyle">
			<view class="flip-card-inner" :class="{ 'is-flipped': flipped }" :style="activeCardHeightStyle">
				<view class="flip-face flip-face-front" @tap="handleTap">
					<view class="immersive-card front-card-measure" data-skin-component="productCard" :style="[activeCardHeightStyle, themeSkinAssetStyle('productCard')]">
						<view class="card-image-wrap" data-skin-component="productImages" :data-product-id="product.productId || product.id">
							<image
								v-if="productImage"
								class="card-image"
								:src="productImage"
								:mode="fillHeight || themeVariant === 'horizontal' ? 'aspectFill' : 'widthFix'"
								@load="handleImageLoad"
							/>
							<view v-else class="card-image-empty">
								<text class="card-image-empty-text">{{ shortName }}</text>
							</view>
						</view>

						<view class="card-body">
							<view class="card-info-col">
								<view class="card-price-wrap">
									<text class="card-price-symbol" data-text-role="price">¥</text>
									<text class="card-price" data-text-role="price">{{ displayPrice }}</text>
									<text v-if="showMonthlySales" class="card-sales" data-text-role="metaText">月售 {{ product.monthSales }}</text>
								</view>
								<text class="card-title" data-text-role="productTitle">{{ product.productName }}</text>
								<text v-if="subtitleText" class="card-subtitle" data-text-role="bodyText">{{ subtitleText }}</text>
							</view>
							<view class="card-cart-btn" @tap.stop="handleQuickAdd">
								<image class="card-cart-icon-image" src="/static/tabbar/cart-active.svg" mode="aspectFit"></image>
							</view>
						</view>
					</view>
				</view>

				<view class="flip-face flip-face-back">
					<product-card-back
						v-if="backMounted"
						:product="product"
						:table-no="tableNo"
						:image-override="imageOverride"
						:active="flipped"
						@close="onBackClose"
						@play-video="onBackPlayVideo"
						@added="onBackAdded"
						@checkout="onBackCheckout"
					/>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { resolveImageUrl } from '@/utils/apiconfig.js'
import ProductCardBack from '@/components/product-card-back.vue'

export default {
	name: 'ImmersiveProductCard',
	components: { ProductCardBack },
	props: {
		product: {
			type: Object,
			default() { return {} }
		},
		flipped: { type: Boolean, default: false },
		compact: { type: Boolean, default: false },
		fillHeight: { type: Boolean, default: false },
		themeVariant: { type: String, default: 'vertical' },
		activeCard: { type: Boolean, default: false },
		tableNo: { type: String, default: '' },
		imageOverride: { type: String, default: '' }
	},
	data: function () {
		return {
			backMounted: false,
			frontCardHeight: 0,
			returning: false,
			returnTimer: null
		}
	},
	watch: {
		flipped: function (val) {
			if (val) {
				this.clearReturnTimer()
				this.returning = false
				this.measureFrontCard()
				this.backMounted = true
			} else if (this.backMounted) {
				this.returning = true
				this.clearReturnTimer()
				this.returnTimer = setTimeout(() => {
					this.returning = false
					this.returnTimer = null
				}, 580)
			}
		},
		product: function () {
			this.frontCardHeight = 0
			this.$nextTick(this.measureFrontCard)
		}
	},
	mounted: function () {
		if (this.flipped) {
			this.backMounted = true
		}
		this.$nextTick(this.measureFrontCard)
	},
	beforeDestroy: function () {
		this.clearReturnTimer()
	},
	computed: {
		productImage() {
			return resolveImageUrl(this.imageOverride || this.product.imageUrl || this.product.productImage || '')
		},
		subtitleText() {
			return this.product.subTitle || this.product.flavorNotes || this.product.description || this.product.remark || '点击查看商品详情'
		},
		displayPrice() {
			const amount = Number(this.product.price || 0)
			return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
		},
		shortName() {
			return String(this.product.productName || '咖啡').slice(0, 2)
		},
		showMonthlySales() {
			const value = this.product.monthSales
			return value !== undefined && value !== null && value !== ''
		},
		cardHeightStyle() {
			return this.frontCardHeight ? ('height:' + this.frontCardHeight + 'px;min-height:' + this.frontCardHeight + 'px;') : ''
		},
		activeCardHeightStyle() {
			return this.keepFlipFrame ? this.cardHeightStyle : ''
		},
		keepFlipFrame() {
			return this.flipped || this.returning
		},
		visualActive() {
			return this.activeCard || this.returning
		}
	},
	methods: {
		clearReturnTimer() {
			if (this.returnTimer) {
				clearTimeout(this.returnTimer)
				this.returnTimer = null
			}
		},
		measureFrontCard(done) {
			uni.createSelectorQuery()
				.in(this)
				.select('.front-card-measure')
				.boundingClientRect((rect) => {
					let measured = false
					if (rect && rect.height > 200) {
						this.frontCardHeight = rect.height
						measured = true
					}
					if (typeof done === 'function') done(measured)
				})
				.exec()
		},
		handleImageLoad() {
			this.measureFrontCard()
			this.$emit('image-load')
		},
		handleTap() {
			if (this.flipped) return
			uni.createSelectorQuery()
				.in(this)
				.select('.front-card-measure')
				.boundingClientRect((rect) => {
					this.measureFrontCard(() => {
						this.$emit('flip', { product: this.product, rect: rect || null })
					})
				})
				.exec()
		},
		handleQuickAdd() {
			uni.createSelectorQuery()
				.in(this)
				.select('.card-cart-btn')
				.boundingClientRect((rect) => {
					const flyFrom = rect
						? {
							x: rect.left + rect.width / 2,
							y: rect.top + rect.height / 2
						}
						: null
					this.$emit('quick-add', { product: this.product, flyFrom })
				})
				.exec()
		},
		onBackClose() {
			this.$emit('close', this.product)
		},
		onBackPlayVideo(payload) {
			this.$emit('play-video', payload)
		},
		onBackAdded(payload) {
			this.$emit('added', payload || { product: this.product })
		},
		onBackCheckout() {
			this.$emit('checkout', this.product)
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.immersive-card-wrap {
	width: 100%;
	min-height: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0;
	box-sizing: border-box;
	perspective: 1500rpx;
	transform-origin: center center;
	transition:
		transform 0.28s cubic-bezier(0.2, 0.78, 0.18, 1),
		filter 0.28s ease-out;
}

.immersive-card-wrap.is-active-card {
	transform: translateY(0) scale(1);
	filter: drop-shadow(0 18rpx 30rpx rgba(36, 24, 19, 0.2));
}

.immersive-card-wrap.is-compact {
	height: auto;
	align-items: stretch;
	padding: 0;
	perspective: 1500rpx;
}

.immersive-card-wrap.is-compact.is-active-card {
	transform: none;
	filter: none;
}

.immersive-card-wrap.is-compact .immersive-card {
	height: auto;
}

.immersive-card-wrap.is-compact .card-image-wrap {
	height: auto;
	padding-bottom: 0;
	position: relative;
	background: $accent-surface;
}

.immersive-card-wrap.is-compact .card-image {
	position: relative;
	display: block;
	width: 100%;
	height: auto;
}

.immersive-card-wrap.is-compact .card-image-empty {
	width: 100%;
	min-height: 360rpx;
}

.immersive-card-wrap.is-compact .card-body {
	padding: 18rpx 18rpx 20rpx;
	min-height: 164rpx;
	gap: 12rpx;
}

.immersive-card-wrap.is-compact .card-title {
	font-size: 28rpx;
	font-weight: 700;
	-webkit-line-clamp: 1;
}

.immersive-card-wrap.is-compact .card-subtitle {
	font-size: 21rpx;
	-webkit-line-clamp: 1;
}

.immersive-card-wrap.is-compact .card-price-symbol {
	font-size: 22rpx;
	margin-bottom: 5rpx;
}

.immersive-card-wrap.is-compact .card-price {
	font-size: 36rpx;
}

.immersive-card-wrap.is-compact .card-sales {
	font-size: 21rpx;
}

.immersive-card-wrap.is-compact .card-cart-btn {
	width: 64rpx;
	height: 64rpx;
	box-shadow: 0 8rpx 16rpx rgba(122, 79, 45, 0.16);
}

.immersive-card-wrap.is-compact .card-cart-icon-image {
	width: 38rpx;
	height: 38rpx;
}

.immersive-card-wrap.is-compact ::v-deep .card-back-shell {
	border-radius: 30rpx;
	box-shadow: none;
}

.immersive-card-wrap.is-compact ::v-deep .back-head {
	padding: 18rpx 18rpx 14rpx;
	gap: 10rpx;
}

.immersive-card-wrap.is-compact ::v-deep .back-thumb-wrap,
.immersive-card-wrap.is-compact ::v-deep .back-subtitle,
.immersive-card-wrap.is-compact ::v-deep .head-video-btn,
.immersive-card-wrap.is-compact ::v-deep .selected-spec-box,
.immersive-card-wrap.is-compact ::v-deep .description-text {
	display: none;
}

.immersive-card-wrap.is-compact ::v-deep .back-title {
	font-size: 24rpx;
	line-height: 1.25;
	-webkit-line-clamp: 1;
}

.immersive-card-wrap.is-compact ::v-deep .back-close {
	width: 48rpx;
	height: 48rpx;
}

.immersive-card-wrap.is-compact ::v-deep .back-body {
	padding: 0 16rpx;
}

.immersive-card-wrap.is-compact ::v-deep .back-section {
	margin-top: 14rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-block {
	gap: 10rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-head {
	gap: 8rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-title {
	font-size: 22rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-rule {
	font-size: 21rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-options {
	gap: 8rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-chip {
	padding: 8rpx 12rpx;
	border-radius: 14rpx;
}

.immersive-card-wrap.is-compact ::v-deep .spec-chip-name,
.immersive-card-wrap.is-compact ::v-deep .spec-chip-price {
	font-size: 21rpx;
}

.immersive-card-wrap.is-compact ::v-deep .back-bottom {
	padding: 14rpx 16rpx 16rpx;
	gap: 10rpx;
	flex-direction: column;
	align-items: stretch;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-total {
	justify-content: center;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-total-symbol {
	font-size: 22rpx;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-total-value {
	font-size: 30rpx;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-actions {
	gap: 8rpx;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-btn {
	height: 56rpx;
	min-width: 0;
	padding: 0 12rpx;
	border-radius: 14rpx;
}

.immersive-card-wrap.is-compact ::v-deep .bottom-btn-text {
	font-size: 22rpx;
}

.flip-card {
	position: relative;
	width: 100%;
	height: auto;
	min-height: 0;
	transform-style: preserve-3d;
}

.flip-card.is-flipped-card {
	min-height: 0;
}

.flip-card-inner {
	position: relative;
	width: 100%;
	height: auto;
	min-height: 0;
	transform-style: preserve-3d;
	transform-origin: center center;
	transition: transform 0.54s cubic-bezier(0.2, 0.72, 0.2, 1);
	will-change: transform;
}

.flip-card.is-flipped-card .flip-card-inner {
	min-height: 0;
}

.flip-card-inner.is-flipped {
	transform: rotateY(-180deg);
}

.flip-face {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	-webkit-backface-visibility: hidden;
	backface-visibility: hidden;
	border-radius: 30rpx;
	overflow: hidden;
	transform-style: preserve-3d;
}

.flip-face-front {
	position: relative;
	height: auto;
}

.flip-card:not(.is-flipped-card) .flip-face-front {
	position: relative;
	height: auto;
}

.flip-card.is-flipped-card .flip-face-front {
	position: absolute;
	height: 100%;
}

.flip-card:not(.is-flipped-card) .flip-face-back {
	display: none;
}

.flip-face-back {
	transform: rotateY(180deg);
	background: $bg-card;
	pointer-events: none;
}

.flip-card-inner.is-flipped .flip-face-front {
	pointer-events: none;
}

.flip-card-inner.is-flipped .flip-face-back {
	pointer-events: auto;
}

.immersive-card {
	position: relative;
	width: 100%;
	height: auto;
	display: flex;
	flex-direction: column;
	background: var(--theme-surface, #FFFFFF);
	border-radius: var(--theme-card-radius, 30rpx);
	border: 2rpx solid var(--theme-border, rgba(122, 79, 45, 0.12));
	box-shadow: var(--theme-card-shadow, 0 14rpx 28rpx rgba(36, 24, 19, 0.16));
	overflow: hidden;
	box-sizing: border-box;
}

.flip-card .immersive-card {
	height: auto;
	min-height: 0;
}

.flip-card.is-flipped-card .immersive-card {
	min-height: 100%;
}

.card-image-wrap {
	position: relative;
	width: 100%;
	height: auto;
	background: $accent-surface;
	box-sizing: border-box;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
}

.immersive-card-wrap.theme-horizontal:not(.is-compact) .immersive-card {
	min-height: 420rpx;
	flex-direction: row;
}

.immersive-card-wrap.theme-horizontal:not(.is-compact) .card-image-wrap {
	width: 44%;
	min-height: 420rpx;
	flex: 0 0 44%;
}

.immersive-card-wrap.theme-horizontal:not(.is-compact) .card-image {
	width: 100%;
	height: 100%;
}

.immersive-card-wrap.theme-horizontal:not(.is-compact) .card-body {
	min-width: 0;
	flex: 1;
	border-top: 0;
	border-left: 2rpx solid var(--theme-border, rgba(122, 79, 45, 0.1));
}

.card-image {
	display: block;
	width: 100%;
	height: auto;
}

.card-image-empty {
	width: 100%;
	min-height: 560rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background:
		radial-gradient(circle at 24% 18%, rgba(255, 205, 139, 0.24), transparent 32%),
		linear-gradient(145deg, $accent-primary-deep 0%, $accent-primary 100%);
}

.card-image-empty-text {
	font-size: 96rpx;
	font-weight: 700;
	color: rgba(255, 255, 255, 0.62);
}

.card-body {
	position: relative;
	z-index: 2;
	padding: 30rpx 30rpx 32rpx;
	background: transparent;
	border-top: 2rpx solid var(--theme-border, rgba(122, 79, 45, 0.1));
	border-bottom-left-radius: 30rpx;
	border-bottom-right-radius: 30rpx;
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	gap: 22rpx;
	box-sizing: border-box;
}

.card-info-col {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 8rpx;
	min-width: 0;
	flex: 1;
}

.card-title {
	max-width: 100%;
	overflow-wrap: anywhere;
	word-break: break-word;
	font-family: $font-family;
	font-size: var(--skin-product-title-size, 38rpx);
	font-weight: var(--skin-product-title-weight, 800);
	line-height: var(--skin-product-title-line-height, 1.2);
	letter-spacing: var(--skin-product-title-letter-spacing, 0);
	color: var(--skin-product-title-color, var(--theme-text, #{$text-primary}));
	text-align: left;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.card-subtitle {
	max-width: 100%;
	font-family: $font-family;
	font-size: var(--skin-body-text-size, 24rpx);
	font-weight: var(--skin-body-text-weight, 500);
	line-height: var(--skin-body-text-line-height, 1.45);
	color: var(--skin-body-text-color, var(--theme-text-secondary, #{$text-secondary}));
	text-align: left;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.card-price-wrap {
	display: flex;
	align-items: flex-end;
	justify-content: flex-start;
	gap: 6rpx;
	max-width: 100%;
}

.card-price-symbol {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: var(--skin-price-color, var(--theme-primary, #{$accent-primary}));
	margin-bottom: 6rpx;
}

.card-price {
	font-family: $font-family;
	font-size: var(--skin-price-size, 48rpx);
	font-weight: var(--skin-price-weight, 800);
	color: var(--skin-price-color, var(--theme-primary, #{$accent-primary}));
	line-height: var(--skin-price-line-height, 1);
}

.card-sales {
	font-family: $font-family;
	font-size: var(--skin-meta-text-size, 22rpx);
	color: var(--skin-meta-text-color, #{$text-secondary});
	margin: 0 0 3rpx 6rpx;
	text-align: left;
	white-space: nowrap;
}

.card-cart-btn {
	width: 86rpx;
	height: 86rpx;
	border-radius: 50%;
	background: var(--theme-page, $accent-primary-soft);
	border: 2rpx solid var(--theme-border, rgba(122, 79, 45, 0.18));
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	box-shadow: 0 12rpx 24rpx rgba(122, 79, 45, 0.18);
	box-sizing: border-box;
	@include active-press;
}

.card-cart-icon-image {
	width: 48rpx;
	height: 48rpx;
	display: block;
}
</style>
