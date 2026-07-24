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
						:class="{
							'app-nav-side-hidden': !showBack && !leftText && !showToggle,
							'app-nav-left-text-only': !showBack && leftText && !showToggle,
							'app-nav-left-with-toggle': !showBack && leftText && showToggle
						}"
						@click="handleBack"
					>
						<view
							v-if="showToggle"
							class="app-nav-toggle-btn"
							@click.stop="handleToggle(toggleValue === 'double' ? 'single' : 'double')"
						>
							<view v-if="toggleValue === 'double'" class="app-nav-toggle-glyph app-nav-toggle-list">
								<text></text>
								<text></text>
								<text></text>
							</view>
							<view v-else class="app-nav-toggle-glyph app-nav-toggle-grid">
								<text></text>
								<text></text>
								<text></text>
								<text></text>
							</view>
						</view>
						<view v-if="showBack" class="app-nav-back-arrow"></view>
						<text v-if="showBack" class="app-nav-side-text">返回</text>
						<text v-if="!showBack && leftText" class="app-nav-side-text app-nav-left-text">{{ leftText }}</text>
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
const TAB_PAGES = ['/pages/index/index', '/pages/scan/menu', '/pages/cart/cart', '/pages/me/me']

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
		leftText: {
			type: String,
			default: ''
		},
		rightText: {
			type: String,
			default: ''
		},
		fallbackUrl: {
			type: String,
			default: '/pages/index/index'
		},
		forceFallback: {
			type: Boolean,
			default: false
		},
		theme: {
			type: String,
			default: 'light' // 'light' or 'transparent'
		},
		showToggle: {
			type: Boolean,
			default: false
		},
		toggleValue: {
			type: String,
			default: 'single' // 'single' or 'double'
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
			if (this.forceFallback) {
				this.jumpTo(this.fallbackUrl)
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

		handleToggle(value) {
			this.$emit('toggle', value)
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
			uni.navigateTo({
				url,
				fail: () => {
					uni.switchTab({
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
	background: rgba(255, 255, 255, 0.98);
	box-shadow: 0 4rpx 12rpx rgba(32, 26, 23, 0.04);
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
	background: rgba(255, 255, 255, 0.28);
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
	position: relative;
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

.app-nav-toggle-btn {
	@include touch-target;
	width: 72rpx;
	height: 72rpx;
	min-width: 72rpx;
	min-height: 72rpx;
	border-radius: 18rpx;
	background: rgba(255, 255, 255, 0.92);
	border: 2rpx solid rgba(232, 224, 215, 0.82);
	box-shadow: 0 4rpx 10rpx rgba(32, 26, 23, 0.06);
	display: flex;
	align-items: center;
	justify-content: center;
	transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

	&:active {
		transform: scale(0.94);
		opacity: 0.88;
	}
}

.app-nav-toggle-glyph {
	width: 32rpx;
	height: 32rpx;
	color: $accent-primary;
}

.app-nav-toggle-list {
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.app-nav-toggle-list text {
	display: block;
	width: 100%;
	height: 4rpx;
	border-radius: 999rpx;
	background: currentColor;
}

.app-nav-toggle-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 6rpx;
}

.app-nav-toggle-grid text {
	display: block;
	border-radius: 4rpx;
	border: 3rpx solid currentColor;
	box-sizing: border-box;
}

.app-nav-left-text-only {
	pointer-events: none;
}

.app-nav-left-with-toggle {
	width: 230rpx;
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
	position: absolute;
	left: 50%;
	top: 50%;
	width: 240rpx;
	transform: translate(-50%, -50%);
	font-size: 34rpx;
	color: $text-primary;
	pointer-events: none;
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

.app-nav-left-text {
	flex: 1;
	min-width: 0;
	max-width: 136rpx;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.app-nav-left-with-toggle .app-nav-left-text {
	max-width: 136rpx;
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
