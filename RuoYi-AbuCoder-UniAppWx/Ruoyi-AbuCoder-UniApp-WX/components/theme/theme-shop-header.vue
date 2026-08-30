<template>
	<view class="shop-header" :class="`shop-header-${variant}`" :style="headerStyle">
		<image v-if="resolvedLogoSrc" class="shop-logo" :src="resolvedLogoSrc" mode="aspectFit" />
		<view class="shop-copy">
			<text class="shop-name" data-text-role="pageTitle">{{ resolvedTitle }}</text>
			<text v-if="resolvedSubtitle" class="shop-subtitle" data-text-role="metaText">{{ resolvedSubtitle }}</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ThemeShopHeader',
	props: { title: { type: String, default: '' }, subtitle: { type: String, default: '' }, logoSrc: { type: String, default: '' } },
	computed: {
		variant() { return this.themeComponent('shopHeader').variant || 'compact' },
		resolvedTitle() {
			const skinTitle = this.themeSkinContent('shopHeader').title
			return skinTitle || this.title || '咖啡店'
		},
		resolvedSubtitle() {
			const skinSubtitle = this.themeSkinContent('shopHeader').subtitle
			return skinSubtitle || this.subtitle || ''
		},
		resolvedLogoSrc() {
			const skinLogo = this.themeSkinContent('shopHeader').logoAssetId
			return this.logoSrc || this.themeAssetUrl(skinLogo) || this.themeAssetUrl(this.themeConfig.brand && this.themeConfig.brand.logoAssetId)
		},
		headerStyle() {
			const slotStyle = this.themeSkinSlotStyle('shopHeader')
			const skinStyle = this.themeSkinAssetStyle('shopHeader')
			const style = { ...slotStyle, ...skinStyle }
			const componentStyle = this.themeBackgroundStyle('shopHeader')
			if (!style.backgroundImage && componentStyle.backgroundImage) Object.assign(style, componentStyle)
			// The skin slot is the single source of truth. A cleared slot must stay cleared.
			if (!style.backgroundImage) delete style.backgroundColor
			const titleToken = this.themeTypographyToken('pageTitle')
			const subtitleToken = this.themeTypographyToken('metaText')
			if (titleToken.color) style['--shop-header-title-color'] = titleToken.color
			if (subtitleToken.color) style['--shop-header-subtitle-color'] = subtitleToken.color
			return style
		}
	}
}
</script>

<style scoped>
.shop-header { height: 128rpx; padding: 16rpx 32rpx; display: flex; align-items: center; gap: 22rpx; box-sizing: border-box; background-color: transparent; color: var(--shop-header-title-color, var(--skin-page-title-color, var(--theme-text))); background-repeat: no-repeat; background-position: center; background-size: 100% 100%; overflow: hidden; }
.shop-header-centered { flex-direction: column; justify-content: center; text-align: center; }
.shop-header-compact { height: 128rpx; }
.shop-logo { width: 76rpx; height: 76rpx; flex: 0 0 auto; }
.shop-copy { min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.shop-name { color: var(--shop-header-title-color, var(--skin-page-title-color, var(--theme-text))); font-size: var(--skin-page-title-size, 34rpx); font-weight: var(--skin-page-title-weight, 700); line-height: var(--skin-page-title-line-height, 1.3); letter-spacing: var(--skin-page-title-letter-spacing, 0); text-shadow: var(--skin-page-title-shadow, none); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.shop-subtitle { color: var(--shop-header-subtitle-color, var(--skin-meta-text-color, var(--theme-text-secondary))); font-size: var(--skin-meta-text-size, 22rpx); line-height: var(--skin-meta-text-line-height, 1.4); letter-spacing: var(--skin-meta-text-letter-spacing, 0); text-shadow: var(--skin-meta-text-shadow, none); }
</style>
