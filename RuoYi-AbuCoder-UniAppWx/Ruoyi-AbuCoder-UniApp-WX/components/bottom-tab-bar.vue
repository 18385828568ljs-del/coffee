<template>
	<view class="bottom-nav">
		<theme-tab-bar class="bottom-tab-bar-fill" :items="navItems" :current="current" :readonly="readonly" @select="switchTab" />
	</view>
</template>

<script>
import ThemeTabBar from '@/components/theme/theme-tab-bar.vue'

const NAV_ITEMS = [
	{
		key: 'home',
		label: '首页',
		url: '/pages/index/index',
		icon: '/static/tabbar/home-inactive.svg',
		activeIcon: '/static/tabbar/home-active.svg'
	},
	{
		key: 'scan',
		label: '点单',
		url: '/pages/scan/menu',
		icon: '/static/tabbar/scan-inactive.svg',
		activeIcon: '/static/tabbar/scan-active.svg'
	},
	{
		key: 'cart',
		label: '购物车',
		url: '/pages/cart/cart',
		icon: '/static/tabbar/cart-inactive.svg',
		activeIcon: '/static/tabbar/cart-active.svg'
	},
	{
		key: 'me',
		label: '我的',
		url: '/pages/me/me',
		icon: '/static/tabbar/me-inactive.svg',
		activeIcon: '/static/tabbar/me-active.svg'
	}
]

export default {
	name: 'BottomTabBar',
	components: { ThemeTabBar },
	props: {
		current: {
			type: String,
			default: 'home'
		},
		readonly: { type: Boolean, default: false }
	},
	data() {
		return {
			navItems: NAV_ITEMS
		}
	},
	methods: {
		switchTab(item) {
			if (!item || !item.url || item.key === this.current) {
				return
			}
			uni.switchTab({
				url: item.url
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.bottom-nav {
	@include bottom-tab-shell;
	z-index: 60;
	padding: 0;
	background: transparent;
	border-top: 0;
}

.bottom-tab-bar-fill {
	flex: 1;
	width: 100%;
	min-width: 0;
}
</style>
