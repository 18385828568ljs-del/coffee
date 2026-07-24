<template>
	<view class="bottom-nav">
		<view
			v-for="item in navItems"
			:key="item.key"
			class="nav-item"
			:class="{ 'nav-item-active': item.key === current }"
			@tap="switchTab(item)"
		>
			<view class="nav-icon-wrap">
				<image class="nav-icon" :src="getIcon(item)" mode="aspectFit"></image>
			</view>
			<text class="nav-label">{{ item.label }}</text>
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
	z-index: 60;
}

.nav-item {
	@include bottom-tab-item;
	@include active-press;
	color: $text-secondary;
	border-radius: 999rpx;
}

.nav-icon-wrap {
	display: flex;
	align-items: center;
	justify-content: center;
}

.nav-item-active {
	background: $accent-primary-soft;
	box-shadow: inset 0 0 0 2rpx rgba(122, 79, 45, 0.14);
}

.nav-icon {
	width: 48rpx;
	height: 48rpx;
	display: block;
}

.nav-label {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 700;
	line-height: 1;
	color: $text-secondary;
}

.nav-item-active .nav-label {
	color: $accent-primary-deep;
}
</style>
