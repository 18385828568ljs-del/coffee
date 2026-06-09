<template>
	<view class="immersive-card-wrap" :class="{ 'is-compact': compact }">
		<template v-if="compact">
			<view class="immersive-card" @tap="handleTap">
				<view class="card-image-wrap">
					<image
						v-if="productImage"
						class="card-image"
						:src="productImage"
						mode="widthFix"
					/>
					<view v-else class="card-image-empty">
						<text>{{ shortName }}</text>
					</view>
					<view v-if="hasVideo" class="card-video-badge">
						<view class="card-video-triangle"></view>
						<text>讲解视频</text>
					</view>
					<view v-if="product.tag" class="card-tag">
						<text>{{ product.tag }}</text>
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
		<view v-else class="flip-card">
			<view class="flip-card-inner" :class="{ 'is-flipped': flipped }">
				<view class="flip-face flip-face-front" @tap="handleTap">
					<view class="immersive-card">
						<view class="card-image-wrap">
							<image
								v-if="productImage"
								class="card-image"
								:src="productImage"
								mode="aspectFit"
							/>
							<view v-else class="card-image-empty">
								<text>{{ shortName }}</text>
							</view>
							<view v-if="hasVideo" class="card-video-badge">
								<view class="card-video-triangle"></view>
								<text>讲解视频</text>
							</view>
							<view v-if="product.tag" class="card-tag">
								<text>{{ product.tag }}</text>
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
import { hasProductVideo } from '@/utils/productVideoMap.js'
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
		shopId: { type: [Number, String], default: 1 },
		tableNo: { type: String, default: '' }
	},
	data: function () {
		return { backMounted: false }
	},
	watch: {
		flipped: function (val) {
			if (val) this.backMounted = true
		}
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
		hasVideo() {
			return hasProductVideo(this.product)
		}
	},
	methods: {
		handleTap() {
			if (this.flipped) return
			this.$emit('flip', this.product)
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
		onBackAdded() {
			this.$emit('added', this.product)
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
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 12rpx 16rpx 12rpx;
	box-sizing: border-box;
	perspective: 1500rpx;
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
	height: 100%;
	transform-style: preserve-3d;
}

.flip-card-inner {
	position: absolute;
	inset: 0;
	transform-style: preserve-3d;
	transform-origin: center center;
	transition: transform 0.7s cubic-bezier(0.45, 0.05, 0.25, 1);
	will-change: transform;
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
	border-radius: 36rpx;
	overflow: hidden;
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
	width: 100%;
	display: flex;
	flex-direction: column;
	background: $bg-card;
	border-radius: 36rpx;
	border: 2rpx solid rgba(232, 224, 215, 0.92);
	box-shadow: 0 30rpx 60rpx rgba(32, 26, 23, 0.14);
	overflow: hidden;
	box-sizing: border-box;
}

.card-image-wrap {
	position: relative;
	width: 100%;
	background: $accent-surface;
	box-sizing: border-box;
	flex-shrink: 0;
	overflow: hidden;
}

.card-image {
	display: block;
	width: 100%;
}

.card-image-empty {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.card-image-empty text {
	font-size: 96rpx;
	font-weight: 700;
	color: rgba(111, 78, 55, 0.5);
}

.card-tag {
	position: absolute;
	top: 24rpx;
	left: 24rpx;
	padding: 6rpx 16rpx;
	border-radius: 999rpx;
	background: rgba(32, 26, 23, 0.78);
}

.card-tag text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: #ffffff;
}

.card-video-badge {
	position: absolute;
	top: 24rpx;
	right: 24rpx;
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 8rpx 18rpx;
	border-radius: 999rpx;
	background: rgba(111, 78, 55, 0.92);
	box-shadow: 0 8rpx 18rpx rgba(111, 78, 55, 0.32);
}

.card-video-triangle {
	width: 0;
	height: 0;
	border-left: 12rpx solid #ffffff;
	border-top: 8rpx solid transparent;
	border-bottom: 8rpx solid transparent;
}

.card-video-badge text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: #ffffff;
}

.card-body {
	padding: 16rpx 150rpx 18rpx;
	display: flex;
	flex-direction: row;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
	box-sizing: border-box;
}

.card-info-col {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 4rpx;
	min-width: 0;
	flex: 1;
}

.card-title {
	max-width: 100%;
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	line-height: 1.22;
	color: $text-primary;
	text-align: left;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.card-subtitle {
	max-width: 100%;
	font-family: $font-family;
	font-size: 20rpx;
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
	gap: 4rpx;
}

.card-price-wrap {
	display: flex;
	align-items: flex-end;
	justify-content: flex-end;
	flex-shrink: 0;
}

.card-price-symbol {
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 700;
	color: $accent-primary;
	margin-right: 3rpx;
	margin-bottom: 4rpx;
}

.card-price {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 700;
	color: $accent-primary;
	line-height: 1;
}

.card-sales {
	font-family: $font-family;
	font-size: 18rpx;
	color: $text-tertiary;
	margin-bottom: 0;
	text-align: right;
}
</style>
