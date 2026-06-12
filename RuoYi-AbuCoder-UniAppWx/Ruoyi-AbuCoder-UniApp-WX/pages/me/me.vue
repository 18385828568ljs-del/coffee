<template>
	<view class="page">
		<app-nav title="我的" :show-back="false" />

		<scroll-view class="content" scroll-y>
			<view class="content-wrap">
				<view class="profile-card" @click="handleProfileAction">
					<view class="profile-top">
						<view class="profile-user">
							<view class="user-avatar">
								<image v-if="userInfo.avatar" :src="userInfo.avatar" mode="aspectFill"></image>
								<view v-else class="user-avatar-fallback">
									<text class="user-avatar-fallback-text">{{ avatarFallbackText }}</text>
								</view>
							</view>
							<view class="user-copy">
								<text class="user-name">{{ isLogin ? userInfo.nickName : '点击登录' }}</text>
								<text class="user-desc">{{ isLogin ? '管理订单、地址与账号设置' : '微信授权后即可同步订单与地址信息' }}</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 会员卡区域 -->
				<view v-if="isLogin" class="member-card" :class="memberCardThemeClass" @click="goMemberCard">
					<view class="member-card-shine"></view>
					<view class="member-top">
						<view class="member-level">
							<view class="level-copy">
								<text class="level-name">{{ memberLevelName }}</text>
								<text class="level-subtitle">咖啡会员卡</text>
							</view>
						</view>
						<view class="member-discount">
							<text class="discount-text">{{ discountText }}</text>
						</view>
					</view>
					<view class="member-bottom">
						<view class="balance-block">
							<text class="balance-value">¥{{ formatMoney(walletInfo.balance) }}</text>
							<text class="balance-label">余额</text>
						</view>
						<view class="member-entry">
							<text>查看会员卡</text>
						</view>
						<view class="recharge-btn" @click.stop="goRecharge">
							<text>充值</text>
						</view>
					</view>
				</view>

				<view class="section-card">
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

				<view class="section-card">
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

		<bottom-tab-bar current="me" />
	</view>
</template>

<script>
import { loginApi, memberApi, walletApi } from '@/utils/apiconfig.js'
import { clearLocalSession, ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'

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
			levelConfigList: FALLBACK_LEVEL_CONFIG.slice()
		}
	},

	computed: {
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
	},

	onShow() {
		this.loadUserInfo()
		if (this.isLogin) {
			this.loadMemberAndWallet()
		}
	},

	methods: {
		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		normalizeUserInfo(userData, fallback = {}) {
			return {
				userId: userData.id || fallback.userId || '',
				openid: userData.openid || fallback.openid || '',
				nickName: userData.nickname || fallback.nickName || '微信用户',
				// 空字符串而非占位图,避免项目里尚未提供 default-avatar.png 时触发 404;
				// 模板通过 v-if 判空渲染本地首字兜底。
				avatar: userData.avatar || fallback.avatar || '',
				phone: userData.phone || fallback.phone || ''
			}
		},

		handleProfileAction() {
			if (!this.isLogin) {
				this.wxLogin()
				return
			}
			uni.navigateTo({
				url: '/pages/me/settings'
			})
		},

		wxLogin() {
			showBusy('登录中...')
			uni.login({
				provider: 'weixin',
				success: (loginRes) => {
					uni.request({
						url: loginApi.wxLogin,
						method: 'POST',
						data: {
							code: loginRes.code
						},
						// 单独给本地接口一个较短超时,避免默认 60s 卡死用户
						timeout: 8000,
						success: (res) => {
							hideBusy()
							const httpOk = res && res.statusCode && res.statusCode >= 200 && res.statusCode < 300
							const bizOk = httpOk && res.data && res.data.code === 0
							if (bizOk) {
								const userData = res.data.data
								if (res.data.token) {
									uni.setStorageSync('token', res.data.token)
								}

								this.userInfo = this.normalizeUserInfo(userData)
								uni.setStorageSync('userInfo', this.userInfo)

								uni.getUserProfile({
									desc: '用于完善用户资料',
									success: (profileRes) => {
										const userInfo = profileRes.userInfo
										this.saveUserInfo({
											openid: userData.openid,
											nickName: userInfo.nickName,
											avatarUrl: userInfo.avatarUrl
										})
									},
									fail: () => {
										showSuccess('登录成功')
										// 登录成功后加载会员和钱包
										this.loadMemberAndWallet()
									}
								})
								return
							}

							// 业务失败(200 但 code != 0),走统一失败兜底。
							const backendMsg = (res.data && res.data.msg) || ''
							this.handleLoginFailure(backendMsg || '登录失败')
						},
						fail: (err) => {
							hideBusy()
							// 网络层失败: timeout / 500 / network error / DNS 失败 等
							// uni.request 里 fail 的 errMsg 常见: "request:fail timeout" / "request:fail ..."
							const reason = (err && (err.errMsg || err.message)) || '网络请求失败'
							this.handleLoginFailure(reason)
						}
					})
				},
				fail: () => {
					hideBusy()
					this.handleLoginFailure('获取登录凭证失败')
				}
			})
		},

		handleLoginFailure(reason) {
			showError(reason || '微信登录失败，请稍后重试')
		},


		saveUserInfo(data) {
			uni.request({
				url: loginApi.wxLogin.replace('/wxlogin', '/saveUserInfo'),
				method: 'POST',
				data,
				success: (res) => {
					if (res.data.code === 0) {
						const userData = res.data.data
						if (res.data.token) {
							uni.setStorageSync('token', res.data.token)
						}
						this.userInfo = this.normalizeUserInfo(userData, {
							userId: this.userInfo.userId,
							openid: data.openid,
							nickName: data.nickName,
							avatar: data.avatarUrl
						})
						uni.setStorageSync('userInfo', this.userInfo)
						showSuccess('登录成功')
						// 登录成功后加载会员和钱包
						this.loadMemberAndWallet()
					}
				}
			})
		},

		loadUserInfo() {
			const userInfo = uni.getStorageSync('userInfo')
			this.userInfo = userInfo && userInfo.userId ? userInfo : {}
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

.profile-card {
	@include hero-card(28rpx);
	@include active-press;
}

.profile-top {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.profile-user {
	flex: 1;
	min-width: 0;
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.user-avatar {
	width: 96rpx;
	height: 96rpx;
	flex-shrink: 0;
	border-radius: 50%;
	overflow: hidden;
	background: $accent-primary-soft;
	box-shadow: inset 0 0 0 2rpx rgba(111, 78, 55, 0.08);
}

.user-avatar image {
	width: 100%;
	height: 100%;
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
	font-size: 36rpx;
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
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}

.user-desc,
.user-tip {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

/* 会员卡区域 */
.member-card {
	position: relative;
	overflow: hidden;
	min-height: 224rpx;
	padding: 28rpx;
	border-radius: 28rpx;
	box-shadow: 0 18rpx 38rpx rgba(32, 26, 23, 0.18);
	box-sizing: border-box;
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
	font-size: 28rpx;
	font-weight: 600;
	color: #FFFFFF;
}

.level-copy {
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.level-subtitle {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: rgba(255, 255, 255, 0.58);
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
	font-size: 36rpx;
	font-weight: 700;
	color: #FFFFFF;
}

.balance-label {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: rgba(255, 255, 255, 0.6);
}

.member-entry {
	flex-shrink: 0;
}

.member-entry text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: rgba(255, 255, 255, 0.76);
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
	font-size: 22rpx;
	font-weight: 600;
	color: #FFFFFF;
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
