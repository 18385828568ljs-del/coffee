<template>
	<view v-if="visible" class="result-dialog-mask" @touchmove.stop.prevent>
		<view class="result-dialog-panel" @tap.stop>
			<view class="result-dialog-icon-shell">
				<image class="result-dialog-icon" :src="icon" mode="aspectFit"></image>
			</view>
			<text class="result-dialog-title">{{ title }}</text>
			<text v-if="description" class="result-dialog-description">{{ description }}</text>
			<view class="result-dialog-actions" :class="{ 'result-dialog-actions-single': !showCancel }">
				<view
					v-if="showCancel"
					class="result-dialog-btn result-dialog-btn-secondary"
					@tap="handleCancel"
				>
					<text>{{ cancelText }}</text>
				</view>
				<view class="result-dialog-btn result-dialog-btn-primary" @tap="handleConfirm">
					<text>{{ confirmText }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ResultDialog',
	props: {
		visible: {
			type: Boolean,
			default: false
		},
		icon: {
			type: String,
			default: '/static/order-status/complete.svg'
		},
		title: {
			type: String,
			default: '操作成功'
		},
		description: {
			type: String,
			default: ''
		},
		confirmText: {
			type: String,
			default: '我知道了'
		},
		cancelText: {
			type: String,
			default: '取消'
		},
		showCancel: {
			type: Boolean,
			default: false
		}
	},
	methods: {
		handleConfirm() {
			this.$emit('confirm')
		},
		handleCancel() {
			this.$emit('cancel')
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.result-dialog-mask {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	z-index: 1200;
	padding: 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(32, 26, 23, 0.18);
	box-sizing: border-box;
}

.result-dialog-panel {
	width: 100%;
	max-width: 560rpx;
	padding: 40rpx 32rpx 32rpx;
	border-radius: 28rpx;
	background: rgba(255, 255, 255, 0.98);
	border: 2rpx solid rgba(232, 224, 215, 0.96);
	box-shadow: 0 24rpx 56rpx rgba(32, 26, 23, 0.12);
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
	animation: dialog-pop 0.22s ease-out;
}

.result-dialog-icon-shell {
	width: 144rpx;
	height: 144rpx;
	border-radius: 72rpx;
	background: linear-gradient(180deg, #FFFFFF 0%, #F5F0E9 100%);
	border: 2rpx solid rgba(232, 224, 215, 0.96);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: inset 0 2rpx 8rpx rgba(255, 255, 255, 0.86);
}

.result-dialog-icon {
	width: 112rpx;
	height: 112rpx;
}

.result-dialog-title {
	margin-top: 24rpx;
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	line-height: 1.4;
	color: $text-primary;
	text-align: center;
}

.result-dialog-description {
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
	text-align: center;
}

.result-dialog-actions {
	width: 100%;
	margin-top: 28rpx;
	display: flex;
	gap: 16rpx;
}

.result-dialog-actions-single {
	justify-content: center;
}

.result-dialog-btn {
	flex: 1;
	min-height: 88rpx;
	padding: 0 24rpx;
	border-radius: $radius-sm;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.result-dialog-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
}

.result-dialog-btn-primary {
	background: $accent-primary;
	border: 2rpx solid $accent-primary;
}

.result-dialog-btn-primary text {
	color: #FFFFFF;
}

.result-dialog-btn-secondary {
	background: $bg-surface;
	border: 2rpx solid $border-subtle;
}

.result-dialog-btn-secondary text {
	color: $text-secondary;
}

@keyframes dialog-pop {
	0% {
		opacity: 0;
		transform: translateY(24rpx) scale(0.96);
	}

	100% {
		opacity: 1;
		transform: translateY(0) scale(1);
	}
}
</style>
