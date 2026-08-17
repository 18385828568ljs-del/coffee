<template>
	<view class="shop-header" :class="`shop-header-${variant}`" :style="headerStyle">
		<image v-if="resolvedLogoSrc" class="shop-logo" :src="resolvedLogoSrc" mode="aspectFit" />
		<view class="shop-copy">
			<text class="shop-name">{{ title }}</text>
			<text v-if="subtitle" class="shop-subtitle">{{ subtitle }}</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ThemeShopHeader',
	props: { title: { type: String, default: '咖啡店' }, subtitle: { type: String, default: '' }, logoSrc: { type: String, default: '' } },
	computed: {
		variant() { return this.themeComponent('shopHeader').variant || 'centered' },
		resolvedLogoSrc() { return this.logoSrc || this.themeAssetUrl(this.themeConfig.brand && this.themeConfig.brand.logoAssetId) },
		headerStyle() {
			const componentStyle = this.themeBackgroundStyle('shopHeader')
			if (componentStyle.backgroundImage) return componentStyle
			const url = this.themeAssetUrl(this.themeConfig.brand && this.themeConfig.brand.headerAssetId)
			return url ? { backgroundImage: `url(${url})`, backgroundSize: 'cover', backgroundPosition: 'center' } : componentStyle
		}
	}
}
</script>

<style scoped>
.shop-header { min-height: 160rpx; padding: 28rpx 32rpx; display: flex; align-items: center; gap: 22rpx; box-sizing: border-box; background-color: var(--theme-primary); color: var(--theme-button-text); }
.shop-header-centered { flex-direction: column; justify-content: center; text-align: center; }
.shop-header-compact { min-height: 120rpx; }
.shop-logo { width: 76rpx; height: 76rpx; flex: 0 0 auto; }
.shop-copy { min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.shop-name { font-size: 34rpx; font-weight: 700; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.shop-subtitle { font-size: 22rpx; opacity: .78; }
</style>
