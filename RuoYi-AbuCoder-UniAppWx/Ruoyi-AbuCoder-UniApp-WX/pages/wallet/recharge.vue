<template>
	<view class="page">
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

				<!-- 充值模板选择 -->
				<view class="section-card">
					<text class="section-title">选择充值金额</text>
					<view class="template-grid">
						<view
							v-for="(item, index) in templates"
							:key="item.templateId || index"
							class="template-item"
							:class="{ 'template-item-active': selectedIndex === index }"
							@click="selectTemplate(index)"
						>
							<text class="template-amount">¥{{ formatMoney(item.payAmount) }}</text>
							<text v-if="Number(item.giftAmount) > 0" class="template-gift">
								赠 ¥{{ formatMoney(item.giftAmount) }}
							</text>
							<text v-else class="template-gift template-gift-none">无赠送</text>
							<text v-if="Number(item.giftAmount) > 0" class="template-total">
								到账 ¥{{ formatMoney(item.totalAmount) }}
							</text>
						</view>
					</view>
				</view>

				<!-- 充值说明 -->
				<view class="section-card">
					<text class="section-title">充值说明</text>
					<view class="rules-list">
						<text class="rule-text">• 充值后余额可用于商城内所有商品的支付</text>
						<text class="rule-text">• 充值金额实时到账，赠送金额一并发放</text>
						<text class="rule-text">• 充值后余额不可提现、不可转赠</text>
						<text class="rule-text">• 如有疑问请联系客服处理</text>
					</view>
				</view>

				<view class="bottom-space"></view>
			</view>
		</scroll-view>

		<!-- 底部充值按钮 -->
		<view class="bottom-bar">
			<view class="bottom-info">
				<text class="bottom-label">充值金额</text>
				<text class="bottom-price">¥{{ selectedTemplate ? formatMoney(selectedTemplate.payAmount) : '0.00' }}</text>
			</view>
			<view
				class="recharge-btn"
				:class="{ disabled: recharging || !selectedTemplate }"
				@click="doRecharge"
			>
				<text>{{ recharging ? '充值中...' : '立即充值' }}</text>
			</view>
		</view>

		<!-- 充值成功弹窗 -->
		<result-dialog
			:visible="successVisible"
			icon="/static/order-status/complete.svg"
			title="充值成功"
			:description="successDesc"
			confirm-text="我知道了"
			@confirm="closeSuccess"
		/>
	</view>
</template>

<script>
import ResultDialog from '@/components/result-dialog.vue'
import { walletApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError, showSuccess, showConfirm } from '@/utils/ui-feedback.js'

// 前端兜底模板（当接口未就绪时使用）
const FALLBACK_TEMPLATES = [
	{ templateId: 1, payAmount: 50, giftAmount: 0, totalAmount: 50 },
	{ templateId: 2, payAmount: 100, giftAmount: 8, totalAmount: 108 },
	{ templateId: 3, payAmount: 200, giftAmount: 20, totalAmount: 220 },
	{ templateId: 4, payAmount: 500, giftAmount: 70, totalAmount: 570 },
	{ templateId: 5, payAmount: 1000, giftAmount: 180, totalAmount: 1180 }
]

export default {
	components: {
		ResultDialog
	},
	data() {
		return {
			walletInfo: {
				balance: 0,
				totalRecharge: 0,
				totalGift: 0,
				totalConsumed: 0
			},
			templates: [],
			selectedIndex: -1,
			recharging: false,
			successVisible: false,
			successDesc: ''
		}
	},

	computed: {
		selectedTemplate() {
			return this.selectedIndex >= 0 ? this.templates[this.selectedIndex] : null
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
				await Promise.all([
					this.loadWalletInfo(),
					this.loadTemplates()
				])
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

		async loadTemplates() {
			try {
				const res = await requestPromise({
					url: walletApi.rechargeTemplates,
					method: 'GET'
				})
				if (isSuccessResponse(res) && Array.isArray(res.data.data) && res.data.data.length > 0) {
					this.templates = res.data.data
					return
				}
			} catch (error) {
				console.warn('[recharge] 加载充值模板失败，使用兜底数据', error)
			}
			// 接口未就绪时使用兜底模板
			this.templates = FALLBACK_TEMPLATES
		},

		selectTemplate(index) {
			this.selectedIndex = this.selectedIndex === index ? -1 : index
		},

		async doRecharge() {
			if (this.recharging || !this.selectedTemplate) {
				if (!this.selectedTemplate) {
					showError('请先选择充值金额')
				}
				return
			}

			const template = this.selectedTemplate
			const confirmed = await showConfirm({
				title: '确认充值',
				content: `确定充值 ¥${this.formatMoney(template.payAmount)}${Number(template.giftAmount) > 0 ? '（赠送 ¥' + this.formatMoney(template.giftAmount) + '）' : ''} 吗？`
			})
			if (!confirmed) {
				return
			}

			this.recharging = true
			showBusy('充值中...')

			try {
				const res = await requestPromise({
					url: walletApi.recharge,
					method: 'POST',
					data: {
						templateId: template.templateId,
						payAmount: template.payAmount
					}
				})

				if (isSuccessResponse(res)) {
					// 联调模式：直接模拟回调确认入账
					const rechargeNo = res.data.data && res.data.data.rechargeNo
					if (rechargeNo) {
						await this.confirmRecharge(rechargeNo)
					}
					await this.loadWalletInfo()
					this.successDesc = `充值 ¥${this.formatMoney(template.payAmount)} 成功${Number(template.giftAmount) > 0 ? '，赠送 ¥' + this.formatMoney(template.giftAmount) + ' 已到账' : ''}。`
					this.successVisible = true
					this.selectedIndex = -1
					return
				}

				showError((res.data && res.data.msg) || '充值失败')
			} catch (error) {
				showError('充值失败，请稍后重试')
			} finally {
				this.recharging = false
				hideBusy()
			}
		},

		async confirmRecharge(rechargeNo) {
			try {
				await requestPromise({
					url: walletApi.rechargeCallback,
					method: 'POST',
					data: { rechargeNo }
				})
			} catch (error) {
				// 静默处理
			}
		},

		closeSuccess() {
			this.successVisible = false
		}
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

/* 充值模板 */
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

.template-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 16rpx;
}

.template-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 6rpx;
	padding: 24rpx 12rpx;
	border-radius: $radius-sm;
	border: 2rpx solid $border-subtle;
	background: $bg-surface;
	box-sizing: border-box;
	transition: all 0.2s ease;
	@include active-press;
}

.template-item-active {
	border-color: $accent-primary;
	background: $accent-surface;
	box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.12);
}

.template-amount {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.template-gift {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $accent-warm;
}

.template-gift-none {
	color: $text-tertiary;
}

.template-total {
	font-family: $font-family;
	font-size: 18rpx;
	font-weight: 500;
	color: $text-secondary;
}

/* 充值说明 */
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
