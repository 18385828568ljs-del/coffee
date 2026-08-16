<template>
	<view class="page">
		<scroll-view class="content" scroll-y>
			<view v-if="bannerList.length" class="hero-section">
				<swiper
					class="hero-swiper"
					autoplay
					circular
					indicator-dots
					indicator-active-color="#FFFFFF"
					indicator-color="rgba(255, 255, 255, 0.42)"
				>
					<swiper-item v-for="(item, index) in bannerList" :key="getBannerKey(item, index)">
						<image
							v-if="getBannerImage(item, index)"
							class="hero-image"
							:src="getBannerImage(item, index)"
							mode="aspectFit"
							@error="handleBannerImageError(index)"
							@tap="handleBannerTapByIndex(index)"
						></image>
						<view
							v-else
							class="hero-image hero-fallback"
							@tap="handleBannerTapByIndex(index)"
						></view>
					</swiper-item>
				</swiper>
			</view>

			<view class="content-wrap">
				<view class="entry-grid">
					<view class="entry-card entry-card-primary" @tap="startOrder">
						<view class="entry-icon">
							<image class="entry-icon-image" src="/static/home/order.svg" mode="aspectFit"></image>
						</view>
						<text class="entry-title">点单</text>
						<text class="entry-label">ORDER</text>
					</view>

					<view class="entry-card entry-card-secondary" @tap="goMall">
						<view class="entry-icon">
							<image class="entry-icon-image" src="/static/home/shop.svg" mode="aspectFit"></image>
						</view>
						<text class="entry-title">商店</text>
						<text class="entry-label">SHOP</text>
					</view>
				</view>

				<view v-if="cardCampaign" class="card-draw-entry" @tap="goCardDraw">
					<image v-if="cardCampaign.coverImage" class="card-draw-cover" :src="resolveImageUrl(cardCampaign.coverImage)" mode="aspectFill"></image>
					<view v-else class="card-draw-poster"><text class="card-draw-en">COFFEE CARD</text><text class="card-draw-mark">DRAW</text></view>
					<view class="card-draw-copy"><text class="card-draw-title">{{ cardCampaign.title }}</text><text class="card-draw-desc">{{ cardCampaign.subtitle || '抽取属于你的今日咖啡卡片' }}</text></view>
					<text class="card-draw-arrow">›</text>
				</view>

				<image class="welcome-image" src="/static/banner/welcome.png" mode="widthFix"></image>

				<view v-if="featuredActivity" class="activity-section">
					<view class="activity-section-head">
						<text class="activity-section-title">参与线下活动</text>
						<view class="activity-more" @tap="goActivityList">
							<text>更多</text>
							<text class="activity-more-arrow">›</text>
						</view>
					</view>
					<view class="activity-card" @tap="goActivityDetail(featuredActivity)">
						<image
							v-if="getActivityImage(featuredActivity)"
							class="activity-cover"
							:src="getActivityImage(featuredActivity)"
							mode="widthFix"
						></image>
						<view v-else class="activity-cover activity-cover-empty">
							<text>线下活动</text>
						</view>
						<view class="activity-copy">
							<text class="activity-title">{{ featuredActivity.title }}</text>
							<text class="activity-meta">{{ formatActivityTime(featuredActivity.startTime) }}</text>
							<text class="activity-desc">{{ featuredActivity.summary || featuredActivity.location || '预约参加线下活动' }}</text>
						</view>
					</view>
				</view>

				<view class="about-section">
					<view class="about-section-head">
						<text class="about-section-title">关于我们</text>
					</view>
					<image class="about-image" src="/static/banner/about-us.jpg" mode="widthFix"></image>
				</view>

				<view class="content-bottom-space"></view>
			</view>
		</scroll-view>

		<view v-if="showWxLogin" class="wx-login-mask">
			<view class="wx-login-panel">
				<text class="wx-login-title">微信登录</text>
				<text class="wx-login-desc">登录后下单、查订单和会员权益会更顺畅。</text>
				<button
					class="wx-login-primary"
					@click="handleWxLogin"
					:disabled="wxLoginLoading"
				>
					{{ wxLoginLoading ? '登录中...' : '微信登录' }}
				</button>
				<view class="wx-login-secondary" @tap="dismissWxLogin">
					<text>稍后再说</text>
				</view>
			</view>
		</view>

		<bottom-tab-bar current="home" />
	</view>
</template>

<script>
import { bannerApi, offlineActivityApi, cardApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getToken } from '@/utils/auth.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { loginByWxAuth } from '@/utils/wx-login.js'
import { getLocalUserInfo } from '@/utils/session.js'

const DEFAULT_SHOP_ID = 1
const DEFAULT_SHOP_NAME = 'XX 咖啡'
const SCAN_MENU_CONTEXT_KEY = 'scanMenuEntryContext'
const WX_LOGIN_DISMISSED_KEY = 'wxLoginDismissed'

function decodeValue(value) {
	try {
		return decodeURIComponent(String(value || ''))
	} catch (error) {
		return String(value || '')
	}
}

function parseScene(scene) {
	const result = {}
	if (!scene) {
		return result
	}
	try {
		const decoded = decodeURIComponent(String(scene))
		decoded.split('&').forEach((part) => {
			const kv = part.split('=')
			if (kv[0]) {
				result[kv[0]] = decodeValue(kv[1] || '')
			}
		})
	} catch (error) {}
	return result
}

export default {
	data() {
		return {
			shopId: DEFAULT_SHOP_ID,
			shopName: DEFAULT_SHOP_NAME,
			tableNo: '',
			bannerList: [],
			activityList: [],
			cardCampaign: null,
			bannerImageErrorMap: {},
			showWxLogin: false,
			wxLoginLoading: false
		}
	},

	onLoad(options = {}) {
		this.resolveEntryContext(options)
		this.loadBanners()
		this.loadActivities()
		this.loadCardCampaign()
		this.maybeShowWxLogin()
	},

	onShow() {
		this.maybeShowWxLogin()
		this.loadCardCampaign()
	},

	onPullDownRefresh() {
		Promise.all([
			this.loadBanners(),
			this.loadActivities(),
			this.loadCardCampaign()
		]).finally(() => {
			uni.stopPullDownRefresh()
		})
	},

	computed: {
		featuredActivity() {
			return this.activityList[0] || null
		}
	},

	methods: {
		resolveImageUrl,
		resolveEntryContext(options = {}) {
			const sceneData = parseScene(options.scene)
			const shopId = options.shopId || sceneData.shopId
			const tableNo = options.tableNo || sceneData.tableNo
			const shopName = options.shopName || sceneData.shopName

			this.shopId = Number(shopId || DEFAULT_SHOP_ID) || DEFAULT_SHOP_ID
			this.tableNo = tableNo ? String(tableNo).trim() : ''
			this.shopName = shopName ? decodeValue(shopName) : DEFAULT_SHOP_NAME
		},

		async loadBanners() {
			try {
				const res = await requestPromise({
					url: bannerApi.list,
					method: 'GET'
				})
				if (isSuccessResponse(res)) {
					const list = (res.data && res.data.data) || []
					this.bannerList = Array.isArray(list) ? list.filter((item) => item && item.bannerImg) : []
					return
				}
				this.bannerList = []
			} catch (error) {
				this.bannerList = []
			}
		},

		async loadActivities() {
			try {
				const res = await requestPromise({
					url: offlineActivityApi.list,
					method: 'GET',
					header: this.getAuthHeader()
				})
				if (isSuccessResponse(res)) {
					const list = (res.data && res.data.data) || []
					this.activityList = Array.isArray(list) ? list : []
					return
				}
				this.activityList = []
			} catch (error) {
				this.activityList = []
			}
		},

		async loadCardCampaign() {
			try {
				const res = await requestPromise({ url: cardApi.active, method: 'GET', header: this.getAuthHeader() })
				this.cardCampaign = isSuccessResponse(res) ? (res.data.data || null) : null
			} catch (error) {
				this.cardCampaign = null
			}
		},

		getAuthHeader() {
			const token = getToken()
			return token ? {
				Authorization: `Bearer ${token}`,
				'X-Wx-Token': token
			} : {}
		},

		getBannerKey(item, index) {
			if (!item) {
				return `banner-${index}`
			}
			return item.id || item.bannerTitle || item.bannerImg || `banner-${index}`
		},

		getBannerImage(item, index) {
			if (!item || this.bannerImageErrorMap[index]) {
				return ''
			}
			return resolveImageUrl(item.bannerImg)
		},

		handleBannerImageError(index) {
			if (!this.bannerImageErrorMap[index]) {
				this.$set(this.bannerImageErrorMap, index, true)
			}
		},

		handleBannerTapByIndex(index) {
			this.handleBannerTap(this.bannerList[index] || null)
		},

		handleBannerTap(item) {
			const targetUrl = item && item.bannerUrl ? String(item.bannerUrl).trim() : ''
			if (!targetUrl || !targetUrl.startsWith('/pages/')) {
				return
			}
			if (this.isTabPage(targetUrl)) {
				uni.switchTab({ url: targetUrl.split('?')[0] })
				return
			}
			uni.navigateTo({ url: targetUrl })
		},

		isTabPage(url) {
			const pagePath = String(url || '').split('?')[0]
			return ['/pages/index/index', '/pages/scan/menu', '/pages/cart/cart', '/pages/me/me'].includes(pagePath)
		},

		buildScanMenuContext(category) {
			const context = {
				shopId: this.shopId
			}
			if (this.shopName) {
				context.shopName = this.shopName
			}
			if (this.tableNo) {
				context.tableNo = this.tableNo
			}
			if (category && category.name) {
				context.categoryName = category.name
			}
			return context
		},

		startOrder(category) {
			this.openScanMenu(this.buildScanMenuContext(category))
		},

		openScanMenu(context) {
			uni.setStorageSync(SCAN_MENU_CONTEXT_KEY, context || {})
			uni.switchTab({ url: '/pages/scan/menu' })
		},

		goMall() {
			uni.navigateTo({ url: '/pages/mall/index' })
		},

		getActivityImage(item) {
			return item && item.coverImage ? resolveImageUrl(item.coverImage) : ''
		},

		formatActivityTime(value) {
			return value ? String(value).replace(/:\d{2}$/, '') : '时间待定'
		},

		goActivityList() {
			uni.navigateTo({ url: '/pages/activity/list' })
		},

		goCardDraw() {
			uni.navigateTo({ url: '/pages/card/draw' })
		},

		goActivityDetail(item) {
			if (!item || !item.activityId) {
				return
			}
			uni.navigateTo({ url: `/pages/activity/detail?id=${item.activityId}` })
		},

		maybeShowWxLogin() {
			const userInfo = getLocalUserInfo()
			const dismissed = uni.getStorageSync(WX_LOGIN_DISMISSED_KEY)
			this.showWxLogin = !(userInfo && userInfo.userId) && !dismissed
		},

		dismissWxLogin() {
			uni.setStorageSync(WX_LOGIN_DISMISSED_KEY, true)
			this.showWxLogin = false
		},

		handleWxLogin() {
			if (this.wxLoginLoading) {
				return
			}
			this.wxLoginLoading = true
			loginByWxAuth().then(() => {
				uni.removeStorageSync(WX_LOGIN_DISMISSED_KEY)
				this.showWxLogin = false
				uni.showToast({
					title: '登录成功',
					icon: 'success'
				})
			}).catch((error) => {
				uni.showToast({
					title: (error && error.message) || '微信登录失败，请稍后再试',
					icon: 'none'
				})
			}).finally(() => {
				this.wxLoginLoading = false
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell(true);
	background: linear-gradient(180deg, #f3ede6 0%, #fbf8f4 48%, #ffffff 100%);
}

.content {
	flex: 1;
	min-height: 0;
}

.hero-section {
	position: relative;
	height: 562.5rpx;
	overflow: hidden;
	background: #ffffff;
}

.hero-swiper,
.hero-image {
	width: 100%;
	height: 562.5rpx;
	display: block;
}

.hero-fallback {
	background: $bg-muted;
}

.entry-title,
.entry-label {
	font-family: $font-family;
}

.content-wrap {
	margin-top: 24rpx;
	padding: 0 $space-page 0;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.entry-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 22rpx;
}

.entry-card {
	min-height: 190rpx;
	padding: 24rpx 22rpx 22rpx;
	border-radius: 22rpx;
	background: rgba(255, 255, 255, 0.96);
	border: 2rpx solid rgba(232, 224, 215, 0.9);
	box-shadow: 0 10rpx 26rpx rgba(32, 26, 23, 0.08);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.entry-card-primary {
	background: rgba(255, 255, 255, 0.96);
	border-color: rgba(232, 224, 215, 0.9);
	box-shadow: 0 10rpx 26rpx rgba(32, 26, 23, 0.08);
}

.entry-card-secondary {
	background: rgba(255, 255, 255, 0.96);
}

.entry-icon {
	width: 84rpx;
	height: 84rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	color: $text-primary;
}

.entry-card-primary .entry-icon {
	color: $accent-primary;
}

.entry-card-secondary .entry-icon {
	color: $accent-primary-deep;
}

.entry-icon-image {
	width: 76rpx;
	height: 76rpx;
	display: block;
}

.entry-title {
	margin-top: 16rpx;
	font-size: 30rpx;
	font-weight: 700;
	letter-spacing: 4rpx;
	text-indent: 4rpx;
	color: $text-primary;
}

.entry-label {
	margin-top: 6rpx;
	font-size: 17rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: $text-tertiary;
}

.entry-card-primary .entry-label {
	color: $accent-warm;
}

.welcome-image {
	width: 100%;
	display: block;
}

.card-draw-entry {
	position: relative;
	min-height: 172rpx;
	display: grid;
	grid-template-columns: 126rpx minmax(0, 1fr) 36rpx;
	align-items: center;
	gap: 22rpx;
	padding: 20rpx 24rpx;
	background: #5c2a12;
	border: 4rpx solid #201a17;
	box-shadow: 0 10rpx 24rpx rgba(66, 34, 18, 0.18);
	box-sizing: border-box;
	@include active-press;
}

.card-draw-cover,
.card-draw-poster {
	width: 126rpx;
	height: 126rpx;
	display: block;
	border: 4rpx solid #f6df54;
	box-sizing: border-box;
}

.card-draw-poster {
	background: #0f8bac;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	color: #201a17;
}

.card-draw-en { font-size: 16rpx; font-weight: 800; }
.card-draw-mark { font-size: 32rpx; font-weight: 900; }
.card-draw-copy { min-width: 0; display: flex; flex-direction: column; }
.card-draw-title { font-family: $font-family; font-size: 30rpx; font-weight: 800; color: #ffffff; }
.card-draw-desc { margin-top: 10rpx; font-family: $font-family; font-size: 22rpx; line-height: 1.4; color: #f5dc9b; }
.card-draw-arrow { font-size: 48rpx; color: #f6df54; }

.activity-section {
	display: flex;
	flex-direction: column;
	gap: 18rpx;
}

.activity-section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 8rpx;
}

.activity-section-title {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.activity-more {
	display: flex;
	align-items: center;
	gap: 4rpx;
	padding: 8rpx 0 8rpx 24rpx;
	@include active-press;
}

.activity-more text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-secondary;
}

.activity-more .activity-more-arrow {
	font-size: 30rpx;
	line-height: 1;
	color: $text-tertiary;
}

.about-section {
	display: flex;
	flex-direction: column;
	gap: 18rpx;
}

.about-section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 8rpx;
}

.about-section-title {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.about-image {
	width: 100%;
	display: block;
}

.activity-card {
	@include card(16rpx);
	display: flex;
	align-items: center;
	gap: 20rpx;
	min-height: 196rpx;
	@include active-press;
}

.activity-cover {
	width: 164rpx;
	flex-shrink: 0;
	border-radius: 16rpx;
	background: $accent-surface;
	display: block;
}

.activity-cover-empty {
	height: 164rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.activity-cover-empty text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 700;
	color: $text-primary;
}

.activity-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
	box-sizing: border-box;
}

.activity-title,
.activity-meta,
.activity-desc {
	font-family: $font-family;
}

.activity-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.activity-meta {
	font-size: 22rpx;
	font-weight: 600;
	color: $text-secondary;
}

.activity-desc {
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.45;
	color: $text-secondary;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.content-bottom-space {
	height: 34rpx;
}

.wx-login-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 99;
	display: flex;
	align-items: flex-end;
	background: rgba(32, 26, 23, 0.38);
}

.wx-login-panel {
	width: 100%;
	padding: 42rpx $space-page calc(42rpx + env(safe-area-inset-bottom));
	border-radius: 32rpx 32rpx 0 0;
	background: #fffaf5;
	box-sizing: border-box;
	box-shadow: 0 -12rpx 34rpx rgba(32, 26, 23, 0.16);
}

.wx-login-title,
.wx-login-desc {
	display: block;
	font-family: $font-family;
}

.wx-login-title {
	font-size: 34rpx;
	font-weight: 700;
	color: $text-primary;
}

.wx-login-desc {
	margin-top: 14rpx;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.5;
	color: $text-secondary;
}

.wx-login-primary {
	margin-top: 34rpx;
	height: 88rpx;
	border-radius: 16rpx;
	background: $accent-primary;
	color: #fff;
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
	line-height: 88rpx;
}

.wx-login-primary::after {
	border: 0;
}

.wx-login-primary[disabled] {
	opacity: 0.7;
}

.wx-login-secondary {
	height: 72rpx;
	margin-top: 14rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.wx-login-secondary text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-secondary;
}
</style>
