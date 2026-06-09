<template>
	<view class="page">
		<app-nav title="设置" fallback-url="/pages/me/me" />

		<view class="content">
			<view class="section-card">
				<text class="section-title">账号信息</text>
				<view class="info-row">
					<text class="info-label">昵称</text>
					<text class="info-value">{{ userInfo.nickName || '未登录' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">手机号</text>
					<text class="info-value">{{ userInfo.phone || '未绑定' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">用户标识</text>
					<text class="info-value">{{ userInfo.userId || '--' }}</text>
				</view>
			</view>

			<view class="section-card">
				<text class="section-title">通用设置</text>
				<view class="action-item" @click="clearOrderDraft">
					<text class="action-text">清除待确认订单缓存</text>
					<text class="action-desc">避免旧草稿影响下次下单</text>
				</view>
				<view class="action-item" @click="copyServiceWechat">
					<text class="action-text">复制客服微信</text>
					<text class="action-desc">AbuCoffeeService</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { showSuccess } from '@/utils/ui-feedback.js'

const SERVICE_WECHAT = 'AbuCoffeeService'

export default {
	data() {
		return {
			userInfo: {}
		}
	},

	onShow() {
		this.userInfo = uni.getStorageSync('userInfo') || {}
	},

	methods: {
		clearOrderDraft() {
			uni.removeStorageSync('orderConfirmDraft')
			uni.removeStorageSync('selectedOrderAddress')
			showSuccess('已清除')
		},

		copyServiceWechat() {
			uni.setClipboardData({
				data: SERVICE_WECHAT,
				success: () => {
					showSuccess('已复制客服微信')
				}
			})
		},

		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/me/me'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
}

.content {
	padding: 0 $space-page 24rpx;
}

.section-card {
	@include card(24rpx);
	margin-bottom: 20rpx;
}

.section-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.info-row,
.action-item {
	padding: 18rpx 0;
}

.info-row + .info-row,
.action-item + .action-item {
	border-top: 2rpx solid $border-light;
}

.info-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
}

.info-label,
.action-text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	color: $text-primary;
}

.info-value {
	flex: 1;
	text-align: right;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
	word-break: break-all;
}

.action-desc {
	display: block;
	margin-top: 8rpx;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}
</style>
