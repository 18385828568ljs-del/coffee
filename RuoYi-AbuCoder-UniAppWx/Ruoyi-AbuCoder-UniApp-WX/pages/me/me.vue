<template>
	<view class="page" :style="themePageStyle">
		<app-nav title="我的" :show-back="false" />

		<scroll-view class="content" scroll-y>
			<view class="content-wrap">
				<view :class="['profile-header', `profile-header-${profileHeaderVariant}`]" data-skin-component="meProfileHeader" :style="themeSkinComponentStyle('meProfileHeader')" @tap="handleProfileAction">
					<view class="profile-header-avatar">
						<image v-if="userInfo.avatar" class="profile-header-avatar-image" :src="userInfo.avatar" mode="aspectFill"></image>
						<text v-else>{{ avatarFallbackText }}</text>
					</view>
					<view class="profile-header-copy">
						<text class="profile-header-name">{{ isLogin ? userInfo.nickName : '点击登录' }}</text>
						<text class="profile-header-desc">{{ isLogin ? '点击编辑头像和昵称' : '微信登录后同步订单与地址' }}</text>
					</view>
					<button v-if="!isLogin" class="profile-header-login" :disabled="loginLoading" @tap.stop="handleWxLogin">
						{{ loginLoading ? '登录中...' : '登录' }}
					</button>
				</view>

				<!-- 会员卡区域 -->
				<view v-if="isLogin || themePreviewMode" class="member-card" :class="memberCardThemeClass" data-skin-component="memberCard" :style="themeSkinComponentStyle('memberCard')" @click="goMemberCard">
					<view class="member-card-shine"></view>
					<view class="member-top">
						<view class="member-level">
							<view class="level-copy">
								<text class="level-name" data-text-role="memberTitle">{{ memberLevelName }}</text>
								<text class="level-subtitle" data-text-role="metaText">咖啡会员卡</text>
							</view>
						</view>
						<view class="member-discount">
							<text class="discount-text" data-text-role="metaText">{{ discountText }}</text>
						</view>
					</view>
					<view class="member-bottom">
						<view class="balance-block">
							<text class="balance-value" data-text-role="memberValue">¥{{ formatMoney(walletInfo.balance) }}</text>
							<text class="balance-label" data-text-role="metaText">余额</text>
						</view>
						<view class="member-entry">
							<text data-text-role="buttonSecondary">查看会员卡</text>
						</view>
						<view class="recharge-btn" @click.stop="goRecharge">
							<text data-text-role="buttonSecondary">充值</text>
						</view>
					</view>
				</view>

				<view class="section-card" data-skin-component="meOrderCenter" :style="themeSkinComponentStyle('meOrderCenter')">
					<view class="section-header" @click="goOrderList()">
						<text class="section-title">订单中心</text>
						<text class="section-link">查看全部</text>
					</view>
					<view class="order-grid">
						<view
							v-for="(item, index) in orderTabs.slice(1)"
							:key="item.name"
							class="order-item"
							@click="goOrderList(index + 1)"
						>
							<view class="order-icon">
								<image class="order-icon-image" :src="item.image" mode="aspectFit"></image>
							</view>
							<text class="order-label">{{ item.name }}</text>
						</view>
					</view>
				</view>

				<view class="section-card" data-skin-component="meAddressCard" :style="themeSkinComponentStyle('meAddressCard')">
					<view class="menu-list">
						<view class="menu-row" @click="goAddress">
							<view class="menu-copy">
								<text class="menu-title">收货地址</text>
								<text class="menu-desc">管理下单收货信息</text>
							</view>
							<text class="menu-value">进入</text>
						</view>
						<view v-if="isLogin" class="menu-row" @click="goWalletLog">
							<view class="menu-copy">
								<text class="menu-title">余额明细</text>
								<text class="menu-desc">充值消费记录</text>
							</view>
							<text class="menu-value">进入</text>
						</view>
					</view>
				</view>

				<view v-if="isLogin" class="logout-btn" @click="logout">
					<text>退出登录</text>
				</view>
			</view>
		</scroll-view>

		<!-- #ifdef H5 -->
		<bottom-tab-bar current="me" />
		<!-- #endif -->
	</view>
</template>

<script>
import { memberApi, walletApi } from '@/utils/apiconfig.js'
import { clearLocalSession, ensureLocalLogin, normalizeWxUserInfo } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { loginByWxAuth } from '@/utils/wx-login.js'
import { showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'
import { themeRuntime } from '@/theme/runtime.js'

const ORDER_TABS = [
	{ name: '全部' },
	{ name: '待付款', image: '/static/order-status/pay.svg' },
	{ name: '待发货', image: '/static/order-status/ship.svg' },
	{ name: '待收货', image: '/static/order-status/receive.svg' },
	{ name: '已完成', image: '/static/order-status/complete.svg' }
]

// 兜底等级配置: 仅在 /api/member/level-config 加载失败时使用,正常情况实时从后端取
const FALLBACK_LEVEL_CONFIG = [
	{ level: 1, levelName: 'Lv.1', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 2, levelName: 'Lv.2', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 3, levelName: 'Lv.3', icon: '', threshold: 0, discountRate: 1.0 },
	{ level: 4, levelName: 'Lv.4', icon: '', threshold: 0, discountRate: 1.0 }
]

export default {
	data() {
		return {
			userInfo: {},
			orderTabs: ORDER_TABS,
			memberInfo: {
				level: 1,
				levelName: '',
				discountRate: 1.0,
				totalSpending: 0
			},
			walletInfo: {
				balance: 0
			},
			// 等级配置实时从后端拿,后台改了立即同步;首屏拿到前先用兜底常量保证渲染
			levelConfigList: FALLBACK_LEVEL_CONFIG.slice(),
			loginLoading: false
		}
	},

	computed: {
		profileHeaderVariant() {
			return this.themeComponent('profileHeader').variant || 'brand'
		},

		isLogin() {
			return !!(this.userInfo && this.userInfo.userId)
		},

		// 头像兜底文字: 优先取 nickName 首字,未登录时显示问号占位
		avatarFallbackText() {
			const name = (this.userInfo && this.userInfo.nickName) || ''
			if (!name) return '?'
			// 兼容 emoji / 多字节字符,用扩展运算符按字符切
			const arr = [...name]
			return arr[0] || '?'
		},

		memberCardThemeClass() {
			return `member-card-level-${this.memberInfo.level || 1}`
		},

		memberLevelName() {
			return this.memberInfo.levelName || 'Lv.' + (this.memberInfo.level || 1)
		},

		discountText() {
			const rate = this.memberInfo.discountRate || 1.0
			if (rate >= 1) {
				return '暂无折扣'
			}
			return Math.round(rate * 100) + '折'
		},

		nextLevelInfo() {
			const currentLevel = this.memberInfo.level || 1
			const nextConfig = this.levelConfigList.find(c => c.level === currentLevel + 1)
			if (!nextConfig) {
				return null // 已最高等级
			}
			const spending = Number(this.memberInfo.totalSpending || 0)
			const gap = Math.max(0, nextConfig.threshold - spending)
			return {
				name: nextConfig.levelName,
				gap: gap,
				threshold: nextConfig.threshold
			}
		},

		progressPercent() {
			const currentLevel = this.memberInfo.level || 1
			const currentConfig = this.levelConfigList.find(c => c.level === currentLevel)
			const nextConfig = this.levelConfigList.find(c => c.level === currentLevel + 1)
			if (!nextConfig || !currentConfig) {
				return 100 // 满级
			}
			const spending = Number(this.memberInfo.totalSpending || 0)
			const range = nextConfig.threshold - currentConfig.threshold
			const progress = spending - currentConfig.threshold
			return Math.min(100, Math.max(0, (progress / range) * 100))
		}
	},

	onLoad() {
		this.loadUserInfo()
		this.loadThemeForContext()
	},

	onShow() {
		this.loadUserInfo()
		this.loadThemeForContext()
		if (this.isLogin) {
			this.loadMemberAndWallet()
		}
	},

	methods: {
		loadThemeForContext() {
			// 装修预览跨页面导航时，预览主题保存在 themeRuntime 内存中；
			// 不要在“我的”页重新加载已发布主题覆盖当前草稿预览。
			if (this.themePreviewMode) return
			const runtimeStoreCode = themeRuntime.state && themeRuntime.state.storeCode
			const scanContext = uni.getStorageSync('scanMenuEntryContext')
			const storeCode = scanContext && scanContext.storeCode
			themeRuntime.loadPublished(runtimeStoreCode || storeCode)
		},

		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		normalizeUserInfo(userData, fallback = {}) {
			return normalizeWxUserInfo(userData, fallback)
		},

		handleProfileAction() {
			if (!this.isLogin) {
				return
			}
			uni.navigateTo({
				url: '/pages/me/userinfo'
			})
		},

		handleWxLogin() {
			if (this.loginLoading) {
				return
			}
			this.loginLoading = true
			loginByWxAuth().then((userInfo) => {
				this.userInfo = this.normalizeUserInfo(userInfo)
				showSuccess('登录成功')
				this.loadMemberAndWallet()
			}).catch((error) => {
				showError((error && error.message) || '微信登录失败，请稍后再试')
			}).finally(() => {
				this.loginLoading = false
			})
		},

		loadUserInfo() {
			const userInfo = uni.getStorageSync('userInfo')
			this.userInfo = userInfo && userInfo.userId ? normalizeWxUserInfo(userInfo) : {}
		},

		async loadMemberAndWallet() {
			await Promise.all([
				this.loadLevelConfig(),
				this.loadMemberInfo(),
				this.loadWalletInfo()
			])
		},

		async loadLevelConfig() {
			try {
				const res = await requestPromise({
					url: memberApi.levelConfig,
					method: 'GET'
				})
				if (isSuccessResponse(res) && Array.isArray(res.data.data) && res.data.data.length) {
					// 后端字段对齐 FALLBACK_LEVEL_CONFIG: level/levelName/icon/threshold/discountRate
					this.levelConfigList = res.data.data.map(item => ({
						level: Number(item.level) || 0,
						levelName: item.levelName || '',
						icon: item.icon || '',
						threshold: Number(item.threshold) || 0,
						discountRate: Number(item.discountRate) || 1
					}))
				}
			} catch (error) {
				console.warn('[me] 加载等级配置失败,沿用本地兜底', error)
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
						levelName: data.levelName,
						discountRate: data.discountRate || 1.0,
						totalSpending: data.totalSpending || 0
					}
				}
			} catch (error) {
				console.warn('[me] 加载会员信息失败', error)
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
				console.warn('[me] 加载钱包信息失败', error)
			}
		},

		goOrderList(index = 0) {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: `/pages/order/list?tab=${index}`
			})
		},

		goAddress() {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: '/pages/address/list'
			})
		},

		goRecharge() {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: '/pages/wallet/recharge'
			})
		},

		goMemberCard() {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: '/pages/member/card'
			})
		},

		goWalletLog() {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: '/pages/wallet/log'
			})
		},

		goHome() {
			uni.switchTab({
				url: '/pages/index/index'
			})
		},

		goCart() {
			uni.switchTab({
				url: '/pages/cart/cart'
			})
		},

		async logout() {
			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要退出登录吗？'
			})
			if (!confirmed) {
				return
			}
			clearLocalSession()
			this.userInfo = {}
			this.memberInfo = { level: 1, levelName: '', discountRate: 1.0, totalSpending: 0 }
			this.walletInfo = { balance: 0 }
			showSuccess('已退出登录')
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
	background: var(--theme-page);
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

.profile-header {
	padding: 28rpx;
	display: flex;
	align-items: center;
	gap: 22rpx;
	border: 2rpx solid var(--theme-border);
	border-radius: var(--theme-card-radius);
	background: var(--theme-surface);
	box-shadow: var(--theme-card-shadow);
	box-sizing: border-box;
	@include active-press;
}

.profile-header-brand {
	color: var(--theme-button-text);
	border-color: var(--theme-primary);
	background: var(--theme-primary);
}

.profile-header-avatar {
	width: 104rpx;
	height: 104rpx;
	flex: 0 0 auto;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	background: var(--theme-border);
	font-size: 36rpx;
	font-weight: 700;
}

.profile-header-avatar-image {
	width: 100%;
	height: 100%;
}

.profile-header-copy {
	min-width: 0;
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 7rpx;
}

.profile-header-name {
	font-size: 34rpx;
	font-weight: 700;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.profile-header-desc {
	font-size: 22rpx;
	opacity: 0.74;
}

.profile-header-login {
	margin: 0;
	padding: 0 24rpx;
	height: 64rpx;
	color: var(--theme-button-text);
	background: var(--theme-button);
	border: 2rpx solid currentColor;
	border-radius: var(--theme-button-radius);
	font-size: 24rpx;
	line-height: 60rpx;
}

.profile-header-login::after {
	border: 0;
}

.profile-card {
	@include active-press;
	padding: 28rpx 8rpx 24rpx;
	box-sizing: border-box;
}

.profile-top {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
}

.profile-user {
	flex: 1;
	min-width: 0;
	display: flex;
	align-items: center;
	gap: 24rpx;
}

.user-avatar {
	width: 116rpx;
	height: 116rpx;
	flex-shrink: 0;
	border-radius: 50%;
	overflow: hidden;
	background: $accent-primary-soft;
	box-shadow:
		0 12rpx 28rpx rgba(111, 78, 55, 0.16),
		inset 0 0 0 4rpx rgba(255, 255, 255, 0.76);
}

.user-avatar image {
	width: 100%;
	height: 100%;
	display: block;
}

.user-avatar-fallback {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(180deg, #A27A5C 0%, #6F4E37 100%);
}

.user-avatar-fallback-text {
	color: #FFF;
	font-size: 42rpx;
	font-weight: 700;
}

.user-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.user-name {
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 700;
	color: $text-primary;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.user-desc,
.user-tip {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	color: $text-secondary;
	line-height: 1.45;
}

.profile-arrow {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background: rgba(111, 78, 55, 0.08);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.profile-arrow text {
	font-family: $font-family;
	font-size: 38rpx;
	font-weight: 600;
	color: $text-primary;
	line-height: 1;
}

/* 会员卡区域 */
.profile-login-btn {
	margin-top: 24rpx;
	width: 100%;
	height: 76rpx;
	border-radius: 16rpx;
	background: $accent-primary;
	color: #fff;
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	line-height: 76rpx;
}

.profile-login-btn::after {
	border: 0;
}

.profile-login-btn[disabled] {
	opacity: 0.7;
}

.member-card {
	position: relative;
	overflow: hidden;
	min-height: 224rpx;
	padding: 28rpx;
	border-radius: 28rpx;
	box-shadow: 0 18rpx 38rpx rgba(32, 26, 23, 0.18);
	box-sizing: border-box;
	background-repeat: no-repeat;
	background-position: center;
	background-size: 100% 100%;
	@include active-press;
}

.member-card-level-1 {
	background: linear-gradient(145deg, #7b5138 0%, #b37a51 58%, #e4c3a2 100%);
}

.member-card-level-2 {
	background: linear-gradient(145deg, #596168 0%, #9fa9ae 58%, #e8edf0 100%);
}

.member-card-level-3 {
	background: linear-gradient(145deg, #5b3516 0%, #b9822e 54%, #f4d28b 100%);
}

.member-card-level-4 {
	background: linear-gradient(145deg, #151313 0%, #3d332b 58%, #90714e 100%);
}

.member-card-shine {
	position: absolute;
	right: -88rpx;
	top: -104rpx;
	width: 250rpx;
	height: 250rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.16);
}

.member-top {
	position: relative;
	z-index: 1;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.member-level {
	display: flex;
	align-items: center;
	gap: 10rpx;
	min-width: 0;
}

.level-name {
	font-family: $font-family;
	font-size: var(--skin-member-title-size, 28rpx);
	font-weight: var(--skin-member-title-weight, 600);
	color: var(--skin-member-title-color, #FFFFFF);
}

.level-copy {
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.level-subtitle {
	font-family: $font-family;
	font-size: var(--skin-meta-text-size, 20rpx);
	font-weight: var(--skin-meta-text-weight, 500);
	color: var(--skin-meta-text-color, rgba(255, 255, 255, 0.72));
}

.member-discount {
	min-height: 44rpx;
	padding: 0 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.2);
	flex-shrink: 0;
}

.member-discount .discount-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: rgba(255, 255, 255, 0.8);
}

.member-bottom {
	position: relative;
	z-index: 1;
	margin-top: 24rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
}

.balance-block {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.balance-value {
	font-family: $font-family;
	font-size: var(--skin-member-value-size, 36rpx);
	font-weight: var(--skin-member-value-weight, 700);
	color: var(--skin-member-value-color, #FFFFFF);
}

.balance-label {
	font-family: $font-family;
	font-size: var(--skin-meta-text-size, 20rpx);
	font-weight: var(--skin-meta-text-weight, 500);
	color: var(--skin-meta-text-color, rgba(255, 255, 255, 0.72));
}

.member-entry {
	flex-shrink: 0;
}

.member-entry text {
	font-family: $font-family;
	font-size: var(--skin-button-secondary-size, 22rpx);
	font-weight: var(--skin-button-secondary-weight, 600);
	color: var(--skin-button-secondary-color, rgba(255, 255, 255, 0.86));
}

.recharge-btn {
	height: 60rpx;
	padding: 0 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-xs;
	background: rgba(255, 255, 255, 0.2);
	@include active-press;
}

.recharge-btn text {
	font-family: $font-family;
	font-size: var(--skin-button-secondary-size, 22rpx);
	font-weight: var(--skin-button-secondary-weight, 600);
	color: var(--skin-button-secondary-color, #FFFFFF);
}

/* 以下为现有样式保留 */
.section-card {
	@include card(28rpx);
}

.section-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 20rpx;
	@include active-press;
}

.section-title {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 600;
	color: $text-primary;
}

.section-link {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-primary;
}

.order-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 16rpx;
}

.order-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10rpx;
	padding: 12rpx 8rpx 10rpx;
	@include active-press;
}

.order-icon {
	width: 84rpx;
	height: 84rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.order-icon-image {
	width: 84rpx;
	height: 84rpx;
}

.order-label {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-secondary;
}

.menu-list {
	display: flex;
	flex-direction: column;
}

.menu-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	padding: 20rpx 0;
	@include active-press;
}

.menu-row + .menu-row {
	border-top: 2rpx solid $border-light;
}

.menu-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.menu-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.menu-desc {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
	line-height: 1.5;
}

.menu-value {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-primary;
}

.logout-btn {
	@include card(24rpx);
	text-align: center;
	@include active-press;
}

.logout-btn text {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $accent-danger;
}
</style>
