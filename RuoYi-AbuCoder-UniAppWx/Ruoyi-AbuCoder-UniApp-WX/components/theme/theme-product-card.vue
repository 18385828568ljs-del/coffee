<template>
	<view class="product-card" :class="`product-card-${variant}`" :style="themeBackgroundStyle('productCard')" @tap="selectProduct">
		<image v-if="image" class="product-image" :src="image" mode="aspectFill" />
		<view class="product-copy">
			<text class="product-name">{{ name }}</text>
			<text v-if="description" class="product-desc">{{ description }}</text>
			<view class="product-foot"><text class="product-price">¥{{ price }}</text><button v-if="!readonly" class="add-button" @tap.stop="$emit('add', product)">+</button></view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ThemeProductCard',
	props: { product: { type: Object, default: () => ({}) }, readonly: { type: Boolean, default: false } },
	computed: {
		variant() { return this.themeComponent('productCard').variant || 'vertical' },
		name() { return this.product.productName || this.product.name || '咖啡' },
		description() { return this.product.description || this.product.subtitle || '' },
		image() { return this.product.productImageUrl || this.product.image || this.product.productImg || '' },
		price() { return Number(this.product.price || this.product.productPrice || 0).toFixed(2) }
	},
	methods: { selectProduct() { if (!this.readonly) this.$emit('select', this.product) } }
}
</script>

<style scoped>
.product-card { overflow: hidden; border: 2rpx solid var(--theme-border); border-radius: var(--theme-card-radius); background-color: var(--theme-surface); box-shadow: var(--theme-card-shadow); }
.product-card-horizontal { display: flex; }
.product-image { width: 100%; height: 280rpx; display: block; }
.product-card-horizontal .product-image { width: 220rpx; height: 220rpx; flex: 0 0 auto; }
.product-copy { min-width: 0; padding: 24rpx; display: flex; flex: 1; flex-direction: column; gap: 10rpx; }
.product-name { color: var(--theme-text); font-size: 30rpx; font-weight: 700; }
.product-desc { color: var(--theme-text-secondary); font-size: 22rpx; line-height: 1.45; }
.product-foot { margin-top: auto; display: flex; align-items: center; justify-content: space-between; }
.product-price { color: var(--theme-primary); font-size: 28rpx; font-weight: 700; }
.add-button { width: 56rpx; height: 56rpx; margin: 0; padding: 0; border-radius: 50%; color: var(--theme-button-text); background: var(--theme-button); font-size: 34rpx; line-height: 54rpx; }
.add-button::after { border: 0; }
</style>
