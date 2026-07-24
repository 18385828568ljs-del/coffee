<template>
	<view class="page">
		<app-nav title="会员卡" fallback-url="/pages/me/me" />

		<scroll-view class="content" scroll-y>
			<view class="content-wrap">
				<view class="card-stage">
					<swiper
						class="tier-swiper"
						:current="currentIndex"
						:previous-margin="'46rpx'"
						:next-margin="'46rpx'"
						:circular="false"
						@change="handleCardChange"
					>
						<swiper-item
							v-for="card in tierCards"
							:key="card.level"
							class="tier-slide"
						>
							<view
								class="tier-card"
								:class="[card.themeClass, { 'tier-card-current': card.level === currentLevel }]"
							>
								<view class="card-shine"></view>
								<view class="tier-card-top">
									<view>
										<text class="card-name">{{ card.levelName }}</text>
										<text class="card-subtitle">{{ getCardStateText(card) }}</text>
									</view>
									<view class="level-mark">
										<text>Lv.{{ card.level }}</text>
									</view>
								</view>
								<view class="tier-card-main">
									<text class="card-discount">{{ card.discountText }}</text>
									<text class="card-discount-label">会员折扣</text>
								</view>
								<view class="tier-card-bottom">
									<text>累计消费 ¥{{ formatMoney(memberInfo.totalSpending) }}</text>
									<text>{{ getCardStateText(card) }}</text>
								</view>
							</view>
						</swiper-item>
					</swiper>
				</view>

				<view class="balance-card">
					<view class="balance-item">
						<text class="balance-value">¥{{ formatMoney(walletInfo.balance) }}</text>
						<text class="balance-label">卡内余额</text>
					</view>
					<view class="balance-divider"></view>
					<view class="balance-item">
						<text class="balance-value">{{ selectedCard.discountText }}</text>
						<text class="balance-label">当前卡权益</text>
					</view>
				</view>

				<view class="progress-card">
					<view class="progress-head">
						<text class="section-title">{{ selectedCard.levelName }}</text>
						<text class="section-note">{{ getCardStateText(selectedCard) }}</text>
					</view>
					<view class="progress-track">
						<view class="progress-fill" :style="{ width: selectedProgressPercent + '%' }"></view>
					</view>
					<text class="progress-text">{{ selectedProgressText }}</text>
				</view>

				<view class="actions">
					<view class="action action-primary" @click="goRecharge">
						<text>去充值</text>
					</view>
					<view class="action action-secondary" @click="goWalletLog">
						<text>余额明细</text>
					</view>
				</view>

				<view class="benefit-card">
					<text class="section-title">本卡权益</text>
					<view class="benefit-list">
						<view class="benefit-row">
							<text class="benefit-title">等级折扣</text>
							<text class="benefit-desc">{{ selectedCard.discountText }}，下单时自动计算。</text>
						</view>
						<view class="benefit-row">
							<text class="benefit-title">储值余额</text>
							<text class="benefit-desc">余额可用于商城和点单消费。</text>
						</view>
						<view class="benefit-row">
							<text class="benefit-title">消费升级</text>
							<text class="benefit-desc">累计消费达到门槛后自动升级。</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { memberApi, walletApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'

const FALLBACK_LEVEL_CONFIG = [
	{ level: 1, levelName: 'Lv.1', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 2, levelName: 'Lv.2', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 3, levelName: 'Lv.3', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 4, levelName: 'Lv.4', icon: '', threshold: 0, discountRate: 1.0 }
]

const THEME_CLASSES = ['tier-bronze', 'tier-silver', 'tier-gold', 'tier-black']

export default {
	data() {
		return {
			currentIndex: 0,
			memberInfo: {
				level: 1,
				levelName: '',
				discountRate: 1.0,
				totalSpending: 0
			},
			walletInfo: {
				balance: 0
			},
			levelConfigList: FALLBACK_LEVEL_CONFIG.slice()
		}
	},

	computed: {
		currentLevel() {
			return Number(this.memberInfo.level || 1)
		},

		tierCards() {
			return this.levelConfigList.map((item, index) => {
				const threshold = Number(item.threshold || 0)
				const level = Number(item.level || index + 1)
				return {
					level,
					levelName: item.levelName || `Lv.${index + 1}`,
					threshold,
					discountRate: Number(item.discountRate || 1),
					discountText: this.formatDiscount(item.discountRate),
					themeClass: THEME_CLASSES[index] || THEME_CLASSES[0],
					isUnlocked: level <= this.currentLevel,
					unlockText: threshold > 0 ? `消费满 ¥${this.formatMoney(threshold)} 解锁` : (level === 1 ? '注册即拥有' : '等级配置加载中')
				}
			})
		},

		selectedCard() {
			return this.tierCards[this.currentIndex] || this.tierCards[0] || {
				level: 1,
				levelName: 'Lv.1',
				threshold: 0,
				discountText: '暂无折扣',
				unlockText: '注册即拥有'
			}
		},

		selectedProgressPercent() {
			const spending = Number(this.memberInfo.totalSpending || 0)
			const target = Number(this.selectedCard.threshold || 0)
			if (target <= 0) {
				return this.selectedCard.level <= this.currentLevel ? 100 : 0
			}
			if (spending >= target) {
				return 100
			}
			return Math.min(100, Math.max(0, (spending / target) * 100))
		},

		selectedProgressText() {
			const spending = Number(this.memberInfo.totalSpending || 0)
			const target = Number(this.selectedCard.threshold || 0)
			if (target <= 0 && this.selectedCard.level > 1) {
				return '等级配置加载中，请稍后刷新。'
			}
			if (this.selectedCard.level <= this.currentLevel) {
				return this.selectedCard.level === this.currentLevel
					? `当前正在使用${this.selectedCard.levelName}权益。`
					: `已拥有${this.selectedCard.levelName}权益。`
			}
			if (spending >= target) {
				return `消费已达到${this.selectedCard.levelName}门槛，系统会自动升级。`
			}
			return `再消费 ¥${this.formatMoney(target - spending)} 解锁${this.selectedCard.levelName}。`
		}
	},

	onLoad() {
		if (!ensureLocalLogin('请先登录', { redirect: true })) {
			return
		}
		this.loadData()
	},

	onShow() {
		if (ensureLocalLogin()) {
			this.loadData()
		}
	},

	onPullDownRefresh() {
		this.loadData().finally(() => {
			uni.stopPullDownRefresh()
		})
	},

	methods: {
		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		formatDiscount(value) {
			const rate = Number(value || 1)
			if (rate >= 1) {
				return '暂无折扣'
			}
			return Math.round(rate * 100) + '折'
		},

		syncCurrentIndex() {
			const index = this.levelConfigList.findIndex(item => Number(item.level) === this.currentLevel)
			this.currentIndex = index >= 0 ? index : 0
		},

		handleCardChange(event) {
			this.currentIndex = event.detail.current || 0
		},

		getCardStateText(card) {
			if (!card) {
				return ''
			}
			if (card.level === this.currentLevel) {
				return '当前等级'
			}
			if (card.level < this.currentLevel) {
				return '已拥有'
			}
			return card.unlockText
		},

		async loadData() {
			await Promise.all([
				this.loadLevelConfig(),
				this.loadMemberInfo(),
				this.loadWalletInfo()
			])
			this.syncCurrentIndex()
		},

		async loadLevelConfig() {
			try {
				const res = await requestPromise({
					url: memberApi.levelConfig,
					method: 'GET'
				})
				if (isSuccessResponse(res) && Array.isArray(res.data.data) && res.data.data.length) {
					this.levelConfigList = res.data.data.map(item => ({
						level: Number(item.level) || 0,
						levelName: item.levelName || '',
						icon: item.icon || '',
						threshold: Number(item.threshold) || 0,
						discountRate: Number(item.discountRate) || 1
					}))
				}
			} catch (error) {
				console.warn('[member-card] 加载等级配置失败', error)
			}
		},

		async loadMemberInfo() {
			try {
				const res = await requestPromise({
					url: memberApi.info,
					method: 'GET'
				})
				if (isSuccessResponse(res) && res.data.data) {
					const data = res.data.data
					this.memberInfo = {
						level: data.level || 1,
						levelName: data.levelName || `Lv.${data.level || 1}`,
						discountRate: data.discountRate || 1.0,
						totalSpending: data.totalSpending || 0
					}
				}
			} catch (error) {
				console.warn('[member-card] 加载会员信息失败', error)
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
						balance: res.data.data.balance || 0
					}
				}
			} catch (error) {
				console.warn('[member-card] 加载钱包信息失败', error)
			}
		},

		goRecharge() {
			uni.navigateTo({
				url: '/pages/wallet/recharge'
			})
		},

		goWalletLog() {
			uni.navigateTo({
				url: '/pages/wallet/log'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
	background: #f8f6f3;
}

.content {
	flex: 1;
	min-height: 0;
}

.content-wrap {
	padding: 0 0 36rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.card-stage {
	padding: 8rpx 0 0;
}

.tier-swiper {
	height: 372rpx;
}

.tier-slide {
	box-sizing: border-box;
	padding: 0 10rpx;
}

.tier-card {
	position: relative;
	overflow: hidden;
	height: 344rpx;
	padding: 30rpx;
	border-radius: 28rpx;
	box-shadow: 0 18rpx 38rpx rgba(32, 26, 23, 0.18);
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.tier-card-current {
	box-shadow: 0 20rpx 42rpx rgba(32, 26, 23, 0.24);
}

.tier-bronze {
	background: linear-gradient(145deg, #7b5138 0%, #b37a51 58%, #e4c3a2 100%);
}

.tier-silver {
	background: linear-gradient(145deg, #596168 0%, #9fa9ae 58%, #e8edf0 100%);
}

.tier-gold {
	background: linear-gradient(145deg, #5b3516 0%, #b9822e 54%, #f4d28b 100%);
}

.tier-black {
	background: linear-gradient(145deg, #151313 0%, #3d332b 58%, #90714e 100%);
}

.card-shine {
	position: absolute;
	right: -96rpx;
	top: -110rpx;
	width: 270rpx;
	height: 270rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.16);
}

.tier-card-top,
.tier-card-main,
.tier-card-bottom {
	position: relative;
	z-index: 1;
}

.tier-card-top,
.tier-card-bottom {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 18rpx;
}

.card-name,
.card-subtitle,
.level-mark text,
.card-discount,
.card-discount-label,
.tier-card-bottom text,
.section-title,
.section-note,
.progress-text,
.balance-value,
.balance-label,
.benefit-title,
.benefit-desc,
.action text {
	font-family: $font-family;
}

.card-name {
	display: block;
	font-size: 36rpx;
	font-weight: 800;
	color: #ffffff;
}

.card-subtitle {
	display: block;
	margin-top: 8rpx;
	font-size: 22rpx;
	font-weight: 600;
	color: rgba(255, 255, 255, 0.68);
}

.level-mark {
	min-height: 48rpx;
	padding: 0 18rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.18);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.level-mark text {
	font-size: 22rpx;
	font-weight: 800;
	color: #ffffff;
}

.tier-card-main {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.card-discount {
	font-size: 66rpx;
	font-weight: 800;
	line-height: 1;
	color: #ffffff;
}

.card-discount-label {
	font-size: 22rpx;
	font-weight: 600;
	color: rgba(255, 255, 255, 0.7);
}

.tier-card-bottom {
	align-items: flex-end;
	padding-top: 18rpx;
	border-top: 2rpx solid rgba(255, 255, 255, 0.16);
}

.tier-card-bottom text {
	font-size: 20rpx;
	font-weight: 600;
	color: rgba(255, 255, 255, 0.72);
}

.balance-card,
.progress-card,
.benefit-card {
	margin: 0 $space-page;
}

.balance-card {
	@include card(26rpx);
	display: flex;
	align-items: center;
}

.balance-item {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.balance-item:last-child {
	align-items: flex-end;
}

.balance-value {
	font-size: 34rpx;
	font-weight: 800;
	color: $text-primary;
}

.balance-label {
	font-size: 20rpx;
	font-weight: 600;
	color: $text-secondary;
}

.balance-divider {
	width: 2rpx;
	height: 54rpx;
	margin: 0 24rpx;
	background: $border-light;
}

.progress-card,
.benefit-card {
	@include card(28rpx);
}

.progress-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.section-title {
	font-size: 30rpx;
	font-weight: 800;
	color: $text-primary;
}

.section-note {
	font-size: 22rpx;
	font-weight: 600;
	color: $text-secondary;
}

.progress-track {
	margin-top: 22rpx;
	height: 12rpx;
	border-radius: 999rpx;
	background: $accent-primary-soft;
	overflow: hidden;
}

.progress-fill {
	height: 100%;
	border-radius: 999rpx;
	background: $accent-primary;
}

.progress-text {
	display: block;
	margin-top: 16rpx;
	font-size: 23rpx;
	font-weight: 600;
	line-height: 1.45;
	color: $text-secondary;
}

.actions {
	margin: 0 $space-page;
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 18rpx;
}

.action {
	height: 88rpx;
	border-radius: $radius-sm;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.action text {
	font-size: 26rpx;
	font-weight: 800;
}

.action-primary {
	background: $accent-primary;
}

.action-primary text {
	color: #ffffff;
}

.action-secondary {
	background: #ffffff;
	border: 2rpx solid $border-subtle;
}

.action-secondary text {
	color: $text-primary;
}

.benefit-list {
	margin-top: 20rpx;
	display: flex;
	flex-direction: column;
}

.benefit-row {
	padding: 20rpx 0;
	display: flex;
	justify-content: space-between;
	gap: 18rpx;
}

.benefit-row + .benefit-row {
	border-top: 2rpx solid $border-light;
}

.benefit-title {
	font-size: 26rpx;
	font-weight: 800;
	color: $text-primary;
	flex-shrink: 0;
}

.benefit-desc {
	flex: 1;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.45;
	text-align: right;
	color: $text-secondary;
}
</style>
