<template>
	<view class="theme-tab-bar" :class="`theme-tab-bar-${variant}`" data-skin-component="tabBar" :style="tabBarStyle">
		<view v-for="item in items" :key="item.key" class="theme-tab-item" :class="{ active: item.key === current }" :style="itemStyle(item)" @tap="select(item)">
			<image v-if="item.icon" class="theme-tab-icon" :src="item.key === current && item.activeIcon ? item.activeIcon : item.icon" mode="aspectFit" />
			<text :data-text-role="item.key === current ? 'tabTextActive' : 'tabText'">{{ item.label }}</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ThemeTabBar',
	props: { items: { type: Array, default: () => [] }, current: { type: String, default: '' }, readonly: { type: Boolean, default: false } },
	computed: {
		variant() { return this.themeComponent('tabBar').variant || 'standard' },
		tabBarStyle() {
			const slot = this.themeSkinSlot('tabBar')
			return { ...this.themeSkinAssetStyle('tabBar'), backgroundColor: slot.backgroundColor || 'var(--theme-surface)' }
		}
	},
	methods: {
		select(item) { if (!this.readonly) this.$emit('select', item) },
		itemStyle(item) {
			const active = item.key === this.current
			const slot = this.themeSkinSlot('tabBar')
			const token = this.themeTypographyToken(active ? 'tabTextActive' : 'tabText')
			return {
				color: token.color || (active ? (slot.activeTextColor || 'var(--theme-primary)') : (slot.textColor || 'var(--theme-text-secondary)')),
				backgroundColor: active ? (slot.activeBackgroundColor || 'transparent') : 'transparent'
			}
		}
	}
}
</script>

<style scoped>
.theme-tab-bar { width: 100%; min-height: 104rpx; padding: 10rpx 20rpx calc(10rpx + env(safe-area-inset-bottom)); display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); box-sizing: border-box; border-top: 2rpx solid var(--theme-border); background-color: var(--theme-surface); background-repeat: no-repeat; background-position: center; background-size: 100% 100%; }
.theme-tab-bar-brand { border-top-color: var(--theme-primary); }
.theme-tab-item { min-width: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; border-radius: 8rpx; color: var(--skin-tab-text-color, var(--theme-text-secondary)); font-size: var(--skin-tab-text-size, 21rpx); font-weight: var(--skin-tab-text-weight, 400); }
.theme-tab-item.active { color: var(--skin-tab-text-active-color, var(--theme-primary)); font-size: var(--skin-tab-text-active-size, 21rpx); font-weight: var(--skin-tab-text-active-weight, 700); }
.theme-tab-icon { width: 46rpx; height: 46rpx; }
</style>
