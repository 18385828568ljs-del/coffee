<template>
	<view class="app-nav" :class="['app-nav-theme-' + theme]">
		<view class="app-nav-placeholder">
			<view class="app-nav-status" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="app-nav-shell">
				<view class="app-nav-row app-nav-row-placeholder"></view>
			</view>
		</view>
		<view class="app-nav-fixed">
			<view class="app-nav-status" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="app-nav-shell">
				<view class="app-nav-row">
					<view
						class="app-nav-side app-nav-back"
						:class="{ 'app-nav-side-hidden': !showBack }"
						@click="handleBack"
					>
						<view v-if="showBack" class="app-nav-back-arrow"></view>
						<text v-if="showBack" class="app-nav-side-text">返回</text>
					</view>
					<text class="app-nav-title">{{ title }}</text>
					<view
						class="app-nav-side app-nav-right"
						:class="{ 'app-nav-right-clickable': !!rightText }"
						@click="handleRight"
					>
						<text v-if="rightText" class="app-nav-side-text app-nav-right-text">{{ rightText }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
const TAB_PAGES = ['/pages/index/index', '/pages/cart/cart', '/pages/me/me', '/pages/scan/menu']

export default {
	props: {
		title: {
			type: String,
			default: ''
		},
		showBack: {
			type: Boolean,
			default: true
		},
		rightText: {
			type: String,
			default: ''
		},
		fallbackUrl: {
			type: String,
			default: '/pages/index/index'
		},
		theme: {
			type: String,
			default: 'light' // 'light' or 'transparent'
		}
	},

	data() {
		return {
			statusBarHeight: 0
		}
	},

	created() {
		this.statusBarHeight = this.resolveStatusBarHeight()
	},

	methods: {
		resolveStatusBarHeight() {
			if (typeof uni.getWindowInfo === 'function') {
				const windowInfo = uni.getWindowInfo()
				return Number(windowInfo.statusBarHeight || 0)
			}
			const systemInfo = uni.getSystemInfoSync()
			return Number(systemInfo.statusBarHeight || 0)
		},

		handleBack() {
			if (!this.showBack) {
				return
			}
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack({
					delta: 1
				})
				return
			}
			this.jumpTo(this.fallbackUrl)
		},

		handleRight() {
			if (!this.rightText) {
				return
			}
			this.$emit('right')
		},

		jumpTo(url) {
			if (!url) {
				return
			}
			if (TAB_PAGES.includes(url)) {
				uni.switchTab({
					url
				})
				return
			}
			uni.redirectTo({
				url,
				fail: () => {
					uni.reLaunch({
						url: '/pages/index/index'
					})
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.app-nav {
	position: relative;
	flex-shrink: 0;
	background: transparent;
	margin-bottom: $top-nav-content-gap;
}

.app-nav-fixed {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 30;
	background: rgba(255, 255, 255, 0.9);
	backdrop-filter: blur(24rpx) saturate(140%);
	-webkit-backdrop-filter: blur(24rpx) saturate(140%);
	box-shadow: 0 10rpx 28rpx rgba(32, 26, 23, 0.06);
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.88);
	overflow: hidden;
}

.app-nav-fixed::before {
	content: '';
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	height: 100%;
	background: rgba(255, 255, 255, 0.48);
	pointer-events: none;
}

.app-nav-shell {
	@include top-nav-shell;
	padding: 0;
	position: relative;
	z-index: 1;
}

.app-nav-row {
	@include top-nav-row;
	padding: 0 32rpx;
	background: transparent;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.72);
	box-sizing: border-box;
}

.app-nav-side {
	@include top-nav-side;
	@include active-press;
	min-height: 88rpx;
}

.app-nav-row-placeholder {
	opacity: 0;
	pointer-events: none;
}

.app-nav-side-hidden {
	visibility: hidden;
	pointer-events: none;
}

.app-nav-back {
	justify-content: flex-start;
	gap: 12rpx;
	min-width: 88rpx;
}

.app-nav-back-arrow {
	width: 18rpx;
	height: 18rpx;
	border-left: 4rpx solid $text-primary;
	border-bottom: 4rpx solid $text-primary;
	transform: rotate(45deg);
	flex-shrink: 0;
}

.app-nav-title {
	@include top-nav-title;
	font-size: 34rpx;
	color: $text-primary;
}

.app-nav-right {
	justify-content: flex-end;
	min-width: 88rpx;
}

.app-nav-right-clickable {
	pointer-events: auto;
}

.app-nav-side-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-primary;
}

.app-nav-right-text {
	color: $text-primary;
	text-align: right;
}

/* === Transparent Theme === */
.app-nav-theme-transparent {
	margin-bottom: 0;
	
	.app-nav-fixed {
		background: transparent;
		backdrop-filter: none;
		-webkit-backdrop-filter: none;
		box-shadow: none;
		border-bottom: none;
	}

	.app-nav-fixed::before {
		display: none;
	}

	.app-nav-row {
		border-bottom: none;
	}

	.app-nav-title,
	.app-nav-side-text {
		color: #FFFFFF;
		text-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.22);
	}

	.app-nav-back-arrow {
		border-left-color: #FFFFFF;
		border-bottom-color: #FFFFFF;
	}
}
</style>
