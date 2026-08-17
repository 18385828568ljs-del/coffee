<template>
	<scroll-view :scroll-x="variant === 'text-row'" class="category-scroll">
		<view class="category-nav" :class="`category-nav-${variant}`">
			<view v-for="(item, index) in items" :key="item.key || index" class="category-item" :style="itemStyle(item)" @tap="$emit('select', item, index)">
				<image v-if="item.icon && variant === 'icon-grid'" class="category-icon" :src="item.icon" mode="aspectFit" />
				<text class="category-label">{{ item.label || item.name }}</text>
			</view>
		</view>
	</scroll-view>
</template>

<script>
export default {
	name: 'ThemeCategoryNav',
	props: { items: { type: Array, default: () => [] } },
	computed: { variant() { return this.themeComponent('categoryNav').variant || 'icon-grid' } },
	methods: {
		itemStyle(item) {
			if (!item || !item.skinSlot) return {}
			const slot = this.themeSkinSlot(item.skinSlot)
			return {
				...this.themeSkinSlotStyle(item.skinSlot),
				color: slot.textColor || 'var(--theme-text)',
				'--category-text-color': slot.textColor || 'var(--theme-text)'
			}
		}
	}
}
</script>

<style scoped>
.category-scroll { width: 100%; }
.category-nav { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20rpx; }
.category-nav-text-row { display: inline-flex; min-width: 100%; }
.category-item { min-width: 0; min-height: 112rpx; padding: 20rpx; display: flex; align-items: center; justify-content: center; gap: 14rpx; box-sizing: border-box; border: 2rpx solid var(--theme-border); border-radius: var(--theme-card-radius); background: var(--theme-surface); box-shadow: var(--theme-card-shadow); }
.category-nav-text-row .category-item { min-width: 200rpx; min-height: 76rpx; white-space: nowrap; box-shadow: none; }
.category-icon { width: 56rpx; height: 56rpx; }
.category-label { color: var(--category-text-color, var(--theme-text)); font-size: 27rpx; font-weight: 700; }
</style>
