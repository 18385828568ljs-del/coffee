<template>
	<view v-if="config.visible !== false && items.length" class="activity-banner" :style="bannerStyle">
		<swiper class="activity-swiper" :autoplay="items.length > 1" circular :indicator-dots="variant === 'carousel' && items.length > 1">
			<swiper-item v-for="(item, index) in items" :key="item.id || index" @tap="$emit('select', item, index)">
				<image v-if="item.image" class="activity-image" :src="item.image" mode="aspectFill" />
				<view v-else class="activity-fallback"><text>{{ item.title || '品牌活动' }}</text></view>
			</swiper-item>
		</swiper>
	</view>
</template>

<script>
export default {
	name: 'ThemeActivityBanner',
	props: { items: { type: Array, default: () => [] } },
	computed: {
		config() { return this.themeComponent('activityBanner') },
		variant() { return this.config.variant || 'single' },
		bannerStyle() {
			return { ...this.themeBackgroundStyle('activityBanner'), ...this.themeSkinSlotStyle('heroBanner') }
		}
	}
}
</script>

<style scoped>
.activity-banner { overflow: hidden; border-radius: var(--theme-image-radius); background-color: var(--theme-surface); }
.activity-swiper, .activity-image, .activity-fallback { width: 100%; height: 400rpx; }
.activity-image { display: block; }
.activity-fallback { display: flex; align-items: center; justify-content: center; color: var(--theme-text); background: var(--theme-border); }
.activity-fallback text { font-size: 30rpx; font-weight: 700; }
</style>
