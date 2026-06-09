<template>
	<view class="bottom-nav">
		<view class="nav-bar">
			<view
				v-for="item in navItems"
				:key="item.key"
				class="nav-item"
				:class="{ 'nav-item-active': item.key === current }"
				@tap="switchTab(item)"
			>
				<image class="nav-icon" :src="getIcon(item)" mode="aspectFit"></image>
				<text class="nav-label">{{ item.label }}</text>
			</view>
		</view>
	</view>
</template>

<script>
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
	props: {
		current: {
			type: String,
			default: 'home'
		}
	},
	data() {
		return {
			navItems: NAV_ITEMS
		}
	},
	methods: {
		getIcon(item) {
			return item.key === this.current ? item.activeIcon : item.icon
		},
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
}

.nav-bar {
	@include bottom-tab-bar;
}

.nav-item {
	@include bottom-tab-item;
	@include active-press;
}

.nav-item-active {
	background: $accent-primary-soft;
	box-shadow: inset 0 0 0 2rpx rgba(111, 78, 55, 0.06);
}

.nav-icon {
	width: 48rpx;
	height: 48rpx;
	display: block;
}

.nav-label {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	line-height: 1;
	color: $text-secondary;
}

.nav-item-active .nav-label {
	color: $text-primary;
}
</style>
