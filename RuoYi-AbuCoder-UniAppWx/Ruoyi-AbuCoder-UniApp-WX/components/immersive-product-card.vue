<template>
	<view class="immersive-card-wrap" :class="{ 'is-compact': compact, 'is-active-card': visualActive }">
		<template v-if="compact">
			<view class="immersive-card" @tap="handleTap">
				<view class="card-image-wrap">
					<image
						v-if="productImage"
						class="card-image"
						:src="productImage"
						mode="widthFix"
						@load="measureFrontCard"
					/>
					<view v-else class="card-image-empty">
						<text class="card-image-empty-text">{{ shortName }}</text>
					</view>
				</view>

				<view class="card-body">
					<view class="card-info-col">
						<text class="card-title">{{ product.productName }}</text>
						<text v-if="subtitleText" class="card-subtitle">{{ subtitleText }}</text>
					</view>
					<view class="card-price-col">
						<view class="card-price-wrap">
							<text class="card-price-symbol">¥</text>
							<text class="card-price">{{ displayPrice }}</text>
						</view>
						<text v-if="showMonthlySales" class="card-sales">月售 {{ product.monthSales }}</text>
					</view>
				</view>
			</view>
		</template>
		<view v-else class="flip-card" :class="{ 'is-flipped-card': keepFlipFrame }" :style="activeCardHeightStyle">
			<view class="flip-card-inner" :class="{ 'is-flipped': flipped }" :style="activeCardHeightStyle">
				<view class="flip-face flip-face-front" @tap="handleTap">
					<view class="immersive-card front-card-measure" :style="activeCardHeightStyle">
						<view class="card-image-wrap">
							<image
								v-if="productImage"
								class="card-image"
								:src="productImage"
								mode="widthFix"
								@load="measureFrontCard"
							/>
							<view v-else class="card-image-empty">
								<text class="card-image-empty-text">{{ shortName }}</text>
							</view>
						</view>

						<view class="card-body">
							<view class="card-info-col">
								<text class="card-title">{{ product.productName }}</text>
								<text v-if="subtitleText" class="card-subtitle">{{ subtitleText }}</text>
							</view>
							<view class="card-price-col">
								<view class="card-price-wrap">
									<text class="card-price-symbol">¥</text>
									<text class="card-price">{{ displayPrice }}</text>
								</view>
								<text v-if="showMonthlySales" class="card-sales">月售 {{ product.monthSales }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="flip-face flip-face-back">
					<product-card-back
						v-if="backMounted"
						:product="product"
						:shop-id="shopId"
						:table-no="tableNo"
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
		activeCard: { type: Boolean, default: false },
		shopId: { type: [Number, String], default: 1 },
		tableNo: { type: String, default: '' }
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
		this.$nextTick(this.measureFrontCard)
	},
	beforeDestroy: function () {
		this.clearReturnTimer()
	},
	computed: {
		productImage() {
			return resolveImageUrl(this.product.imageUrl || this.product.productImage || '')
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
			if (this.compact) {
				if (typeof done === 'function') done(false)
				return
			}
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
		handleTap() {
			if (this.flipped) return
			this.measureFrontCard(() => {
				this.$emit('flip', this.product)
			})
		},
		handleQuickAdd() {
			this.$emit('quick-add', this.product)
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
	transition: transform 0.24s ease-out, filter 0.24s ease-out;
}

.immersive-card-wrap.is-active-card {
	transform: translateY(-8rpx) scale(1.012);
	filter: drop-shadow(0 28rpx 46rpx rgba(32, 26, 23, 0.24));
}

.immersive-card-wrap.is-compact {
	height: auto;
	align-items: stretch;
	padding: 0;
	perspective: none;
}

.immersive-card-wrap.is-compact .immersive-card {
	height: auto;
}

.immersive-card-wrap.is-compact .card-image-wrap {
	height: auto;
	padding: 0;
	background: $bg-card;
}

.immersive-card-wrap.is-compact .card-image {
	display: block;
	width: 100%;
	height: auto;
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
	background: #fbf7f2;
	border-radius: 30rpx;
	border: 2rpx solid rgba(255, 255, 255, 0.72);
	box-shadow: 0 22rpx 50rpx rgba(32, 26, 23, 0.22);
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
	background: #fbf7f2;
	box-sizing: border-box;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
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
	background: linear-gradient(145deg, #2b211c 0%, #6f4e37 100%);
}

.card-image-empty-text {
	font-size: 96rpx;
	font-weight: 700;
	color: rgba(255, 255, 255, 0.62);
}

.card-body {
	position: relative;
	z-index: 2;
	padding: 26rpx 30rpx 30rpx;
	background: rgba(255, 255, 255, 0.86);
	border-top: 2rpx solid rgba(232, 224, 215, 0.68);
	display: flex;
	flex-direction: row;
	align-items: flex-end;
	justify-content: space-between;
	gap: 22rpx;
	box-sizing: border-box;
}

.card-info-col {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 10rpx;
	min-width: 0;
	flex: 1;
}

.card-title {
	max-width: 100%;
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 800;
	line-height: 1.2;
	color: $text-primary;
	text-align: left;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.card-subtitle {
	max-width: 100%;
	font-family: $font-family;
	font-size: 23rpx;
	font-weight: 500;
	line-height: 1.45;
	color: $text-secondary;
	text-align: left;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.card-price-col {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	flex-shrink: 0;
	gap: 8rpx;
	padding: 14rpx 18rpx;
	border-radius: 20rpx;
	background: rgba(111, 78, 55, 0.08);
}

.card-price-wrap {
	display: flex;
	align-items: flex-end;
	justify-content: flex-end;
	flex-shrink: 0;
}

.card-price-symbol {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 700;
	color: $accent-primary;
	margin-right: 3rpx;
	margin-bottom: 4rpx;
}

.card-price {
	font-family: $font-family;
	font-size: 36rpx;
	font-weight: 700;
	color: $accent-primary;
	line-height: 1;
}

.card-sales {
	font-family: $font-family;
	font-size: 19rpx;
	color: $text-tertiary;
	margin-bottom: 0;
	text-align: right;
}
</style>
