<template>
	<view class="scan-card-shell" :class="{ 'scan-card-shell-disabled': disabled }">
		<view class="scan-card-scene" @tap="handleOpen">
			<view class="scan-card-flipper" :class="{ 'scan-card-flipper-active': flipped }">
				<view class="scan-card-face scan-card-front">
					<view class="scan-card-image-wrap">
						<image
							v-if="productImage"
							class="scan-card-image"
							:src="productImage"
							mode="aspectFill"
						/>
						<view v-else class="scan-card-image-empty">
							<text>{{ shortName }}</text>
						</view>
					</view>
					<view class="scan-card-main">
						<view class="scan-card-top">
							<view v-if="product.tag" class="scan-card-tag">
								<text>{{ product.tag }}</text>
							</view>
							<text v-if="showMonthlySales" class="scan-card-sales">月售 {{ product.monthSales }}</text>
						</view>
						<text class="scan-card-title">{{ product.productName }}</text>
						<text class="scan-card-subtitle">{{ subtitleText }}</text>
						<view class="scan-card-footer">
							<view class="scan-card-price-wrap">
								<text class="scan-card-price-symbol">¥</text>
								<text class="scan-card-price">{{ displayPrice }}</text>
							</view>
							<view class="scan-card-add" @tap.stop="handleQuickAdd">
								<text>+</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { resolveImageUrl } from '@/utils/apiconfig.js'

export default {
	name: 'ScanProductCard',
	props: {
		product: {
			type: Object,
			default: function() {
				return {}
			}
		},
		disabled: {
			type: Boolean,
			default: false
		}
	},
	computed: {
		productImage() {
			return resolveImageUrl(this.product.imageUrl || this.product.productImage || '')
		},
		subtitleText() {
			return this.product.subTitle || this.product.flavorNotes || '点击查看商品详情'
		},
		displayPrice() {
			const amount = Number(this.product.price || 0)
			return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
		},
		shortName() {
			return String(this.product.productName || '咖啡').slice(0, 2)
		},
		showMonthlySales() {
			return this.product.monthSales !== undefined && this.product.monthSales !== null && this.product.monthSales !== ''
		}
	},
	methods: {
		handleOpen() {
			if (this.disabled) return
			this.$emit('open', this.product)
		},
		handleQuickAdd() {
			if (this.disabled) return
			this.$emit('quick-add', this.product)
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.scan-card-shell {
	width: 100%;
	box-sizing: border-box;
}

.scan-card-shell + .scan-card-shell {
	margin-top: 0;
}

.scan-card-shell-disabled {
	pointer-events: none;
}

.scan-card-scene {
	height: 100%;
}

.scan-card-flipper {
	position: relative;
	width: 100%;
	height: 100%;
}

.scan-card-face {
	position: relative;
	display: flex;
	@include card(12rpx, $radius-sm);
	width: 100%;
	height: 100%;
	min-height: 186rpx;
	flex-direction: row;
	gap: 14rpx;
	@include active-press;
}

.scan-card-front {
	align-items: center;
}

.scan-card-image-wrap {
	width: 154rpx;
	height: 154rpx;
	border-radius: $radius-sm;
	overflow: hidden;
	flex-shrink: 0;
	background: $accent-surface;
}

.scan-card-image,
.scan-card-image-empty {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.scan-card-image-empty text {
	font-size: 46rpx;
	font-weight: 700;
	color: rgba(111, 78, 55, 0.72);
	letter-spacing: 2rpx;
}

.scan-card-main {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	align-self: stretch;
	gap: 5rpx;
}

.scan-card-top {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.scan-card-tag {
	padding: 3rpx 10rpx;
	border-radius: 999rpx;
	background: $accent-surface;
	border: 1rpx solid rgba(111, 78, 55, 0.1);
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 600;
	line-height: 1.2;
	color: $text-primary;
}

.scan-card-sales {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.scan-card-title {
	margin-top: 0;
	font-family: $font-family;
	font-size: 29rpx;
	font-weight: 600;
	line-height: 1.35;
	color: $text-primary;
	display: -webkit-box;
	overflow: hidden;
	text-overflow: ellipsis;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}

.scan-card-subtitle {
	margin-top: 2rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.4;
	color: $text-secondary;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
	text-overflow: ellipsis;
}

.scan-card-footer {
	margin-top: auto;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 12rpx;
	min-height: 56rpx;
}

.scan-card-price-wrap {
	display: flex;
	align-items: flex-end;
	color: inherit;
}

.scan-card-price-symbol {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	margin-right: 4rpx;
	line-height: 1.2;
}

.scan-card-price {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	line-height: 1.1;
	color: $text-primary;
}

.scan-card-add {
	width: 60rpx;
	height: 60rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.scan-card-add {
	background: $accent-primary;
	border: 2rpx solid $accent-primary;
	box-sizing: border-box;
	@include active-press;
}

.scan-card-add text {
	color: #fff;
	font-size: 28rpx;
	font-weight: 600;
	line-height: 1;
}
</style>
