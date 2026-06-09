<template>
	<view class="page">
		<app-nav title="店铺说明" fallback-url="/pages/me/me" />

		<view class="content">
			<view class="hero-card">
				<text class="hero-title">山野咖啡豆铺</text>
				<text class="hero-subtitle">专注精品咖啡豆、小批量烘焙与日常手冲分享。</text>
			</view>

			<view class="section-card">
				<text class="section-title">店铺介绍</text>
				<text class="section-text">我们以咖啡豆零售为主，重视豆子新鲜度、烘焙节奏和发货体验，希望让每一笔订单都简单、稳定、清晰。</text>
			</view>

			<view class="section-card">
				<text class="section-title">联系店主</text>
				<text class="section-text">客服微信：{{ serviceWechat }}</text>
				<view class="copy-btn" @click="copyWechat">
					<text>复制微信号</text>
				</view>
			</view>

			<view class="section-card">
				<text class="section-title">发货说明</text>
				<text class="section-text">默认按下单顺序安排烘焙与发货，活动优惠以下单时实际匹配结果为准。支付接口当前仍处于联调保留状态。</text>
			</view>
		</view>
	</view>
</template>

<script>
import { showSuccess } from '@/utils/ui-feedback.js'

export default {
	data() {
		return {
			serviceWechat: 'AbuCoffeeService'
		}
	},

	methods: {
		copyWechat() {
			uni.setClipboardData({
				data: this.serviceWechat,
				success: () => {
					showSuccess('已复制')
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

.hero-card {
	@include hero-card(24rpx);
	margin-bottom: 20rpx;
}

.hero-title {
	display: block;
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}

.hero-subtitle,
.section-text {
	display: block;
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
}

.hero-subtitle {
	color: $text-secondary;
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

.section-text {
	color: $text-secondary;
}

.copy-btn {
	margin-top: 18rpx;
	min-height: 88rpx;
	padding: 0 28rpx;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.copy-btn text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: #FFFFFF;
}
</style>
