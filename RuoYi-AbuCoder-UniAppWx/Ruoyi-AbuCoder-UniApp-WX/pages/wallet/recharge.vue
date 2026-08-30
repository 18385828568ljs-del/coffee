<template>
	<view class="page" :style="themePageStyle">
		<app-nav title="充值中心" fallback-url="/pages/me/me" />

		<scroll-view class="content" scroll-y>
			<view class="content-wrap">
				<!-- 余额卡片 -->
				<view class="balance-card">
					<view class="balance-top">
						<text class="balance-label">当前余额（元）</text>
					</view>
					<view class="balance-row">
						<text class="balance-amount">{{ formatMoney(walletInfo.balance) }}</text>
					</view>
					<view class="balance-stats">
						<view class="stat-item">
							<text class="stat-value">¥{{ formatMoney(walletInfo.totalRecharge) }}</text>
							<text class="stat-label">累计充值</text>
						</view>
						<view class="stat-divider"></view>
						<view class="stat-item">
							<text class="stat-value">¥{{ formatMoney(walletInfo.totalGift) }}</text>
							<text class="stat-label">累计赠送</text>
						</view>
						<view class="stat-divider"></view>
						<view class="stat-item">
							<text class="stat-value">¥{{ formatMoney(walletInfo.totalConsumed) }}</text>
							<text class="stat-label">累计消费</text>
						</view>
					</view>
				</view>

				<!-- 在线充值状态 -->
				<view class="section-card">
					<text class="section-title">在线充值暂未开放</text>
					<text class="rule-text">当前仅支持使用已有余额支付订单。</text>
				</view>

				<!-- 充值说明 -->
				<view class="section-card">
					<text class="section-title">使用说明</text>
					<view class="rules-list">
						<text class="rule-text">• 余额可用于商城和扫码点单支付</text>
						<text class="rule-text">• 历史充值与余额流水可在钱包记录中查看</text>
						<text class="rule-text">• 如有疑问请联系客服处理</text>
					</view>
				</view>

				<view class="bottom-space"></view>
			</view>
		</scroll-view>

		<!-- 底部状态 -->
		<view class="bottom-bar">
			<view class="bottom-info">
				<text class="bottom-label">服务状态</text>
				<text class="bottom-price">暂未开放</text>
			</view>
			<view class="recharge-btn disabled">
				<text>在线充值暂未开放</text>
			</view>
		</view>
	</view>
</template>

<script>
import { walletApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy } from '@/utils/ui-feedback.js'

export default {
	data() {
		return {
			walletInfo: {
				balance: 0,
				totalRecharge: 0,
				totalGift: 0,
				totalConsumed: 0
			}
		}
	},

	onLoad() {
		if (!ensureLocalLogin()) {
			return
		}
		this.loadData()
	},

	onShow() {
		// 从其他页面返回时刷新余额
		if (ensureLocalLogin()) {
			this.loadWalletInfo()
		}
	},

	methods: {
		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		async loadData() {
			showBusy('加载中...')
			try {
				await this.loadWalletInfo()
			} finally {
				hideBusy()
			}
		},

		async loadWalletInfo() {
			try {
				const res = await requestPromise({
					url: walletApi.info,
					method: 'GET'
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.walletInfo = {
						balance: res.data.data.balance || 0,
						totalRecharge: res.data.data.totalRecharge || 0,
						totalGift: res.data.data.totalGift || 0,
						totalConsumed: res.data.data.totalConsumed || 0
					}
				}
			} catch (error) {
				// 接口未就绪时静默处理
				console.warn('[recharge] 加载钱包信息失败', error)
			}
		},

	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
	padding-bottom: calc(156rpx + env(safe-area-inset-bottom));
}

.content {
	flex: 1;
	min-height: 0;
}

.content-wrap {
	padding: 0 $space-page 32rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

/* 余额卡片 */
.balance-card {
	@include hero-card(32rpx);
	background: linear-gradient(145deg, #3D2B1F 0%, #6F4E37 55%, #9A6C45 100%);
	border: none;
}

.balance-label {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: rgba(255, 255, 255, 0.7);
}

.balance-row {
	margin-top: 12rpx;
}

.balance-amount {
	font-family: $font-family;
	font-size: 64rpx;
	font-weight: 700;
	color: #FFFFFF;
	line-height: 1.1;
	letter-spacing: -1rpx;
}

.balance-stats {
	margin-top: 28rpx;
	padding-top: 24rpx;
	border-top: 2rpx solid rgba(255, 255, 255, 0.15);
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.stat-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6rpx;
}

.stat-value {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

.stat-label {
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 500;
	color: rgba(255, 255, 255, 0.6);
}

.stat-divider {
	width: 2rpx;
	height: 40rpx;
	background: rgba(255, 255, 255, 0.15);
	flex-shrink: 0;
}

/* 内容区 */
.section-card {
	@include card(28rpx);
}

.section-title {
	display: block;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
	margin-bottom: 20rpx;
}

/* 使用说明 */
.rules-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.rule-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

/* 底部 */
.bottom-space {
	height: 156rpx;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	padding: 16rpx $space-page calc(24rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	box-sizing: border-box;
}

.bottom-info {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.bottom-label {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.bottom-price {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}

.recharge-btn {
	min-width: 236rpx;
	min-height: 88rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.recharge-btn.disabled {
	background: $border-strong;
	@include disabled-state;
}

.recharge-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}
</style>
