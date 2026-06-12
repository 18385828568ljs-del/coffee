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
					<view class="entry-card" @tap="startOrder">
						<view class="entry-icon">
							<view class="entry-icon-cup">
								<view class="cup-body"></view>
								<view class="cup-handle"></view>
								<view class="cup-steam cup-steam-one"></view>
								<view class="cup-steam cup-steam-two"></view>
							</view>
						</view>
						<text class="entry-title">点单</text>
						<text class="entry-label">ORDER</text>
					</view>

					<view class="entry-card" @tap="goMall">
						<view class="entry-icon">
							<view class="entry-icon-bag">
								<view class="bag-handle"></view>
								<view class="bag-body"></view>
							</view>
						</view>
						<text class="entry-title">商店</text>
						<text class="entry-label">SHOP</text>
					</view>
				</view>

				<view class="welcome-card">
					<view class="welcome-copy">
						<text class="welcome-title">欢迎光临</text>
						<text class="welcome-desc">现磨咖啡、轻食与小店周边，选择点单或进入商店。</text>
					</view>
					<view v-if="tableNo" class="table-pill">
						<text>{{ tableNo }} 桌</text>
					</view>
				</view>

				<view v-if="featuredActivity" class="activity-section">
					<view class="activity-section-head">
						<text class="activity-section-title">参与线下活动</text>
					</view>
					<view class="activity-card" @tap="goActivityDetail(featuredActivity)">
						<image
							v-if="getActivityImage(featuredActivity)"
							class="activity-cover"
							:src="getActivityImage(featuredActivity)"
							mode="aspectFill"
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

				<view class="content-bottom-space"></view>
			</view>
		</scroll-view>

		<bottom-tab-bar current="home" />
	</view>
</template>

<script>
import { bannerApi, offlineActivityApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getToken } from '@/utils/auth.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'

const DEFAULT_SHOP_ID = 1
const DEFAULT_SHOP_NAME = 'XX 咖啡'
const SCAN_MENU_CONTEXT_KEY = 'scanMenuEntryContext'

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
			bannerImageErrorMap: {}
		}
	},

	onLoad(options = {}) {
		this.resolveEntryContext(options)
		this.loadBanners()
		this.loadActivities()
	},

	onPullDownRefresh() {
		Promise.all([
			this.loadBanners(),
			this.loadActivities()
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

		goActivityDetail(item) {
			if (!item || !item.activityId) {
				return
			}
			uni.navigateTo({ url: `/pages/activity/detail?id=${item.activityId}` })
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
.entry-label,
.welcome-title,
.welcome-desc,
.table-pill text {
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
	min-height: 214rpx;
	padding: 30rpx 24rpx 26rpx;
	border-radius: 28rpx;
	background: rgba(255, 255, 255, 0.98);
	border: 2rpx solid rgba(232, 224, 215, 0.92);
	box-shadow: 0 20rpx 44rpx rgba(32, 26, 23, 0.12);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.entry-icon {
	width: 76rpx;
	height: 76rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	color: $text-primary;
}

.entry-title {
	margin-top: 22rpx;
	font-size: 32rpx;
	font-weight: 600;
	letter-spacing: 8rpx;
	text-indent: 8rpx;
	color: $text-primary;
}

.entry-label {
	margin-top: 8rpx;
	font-size: 18rpx;
	font-weight: 600;
	letter-spacing: 2rpx;
	color: $text-tertiary;
}

.entry-icon-cup {
	position: relative;
	width: 70rpx;
	height: 70rpx;
}

.cup-body {
	position: absolute;
	left: 10rpx;
	right: 14rpx;
	bottom: 8rpx;
	height: 34rpx;
	border: 4rpx solid currentColor;
	border-top-width: 5rpx;
	border-radius: 0 0 16rpx 16rpx;
}

.cup-handle {
	position: absolute;
	right: 2rpx;
	bottom: 17rpx;
	width: 20rpx;
	height: 20rpx;
	border: 4rpx solid currentColor;
	border-left: 0;
	border-radius: 0 18rpx 18rpx 0;
}

.cup-steam {
	position: absolute;
	top: 4rpx;
	width: 4rpx;
	height: 24rpx;
	border-radius: 999rpx;
	background: currentColor;
	opacity: 0.72;
}

.cup-steam-one {
	left: 24rpx;
	transform: rotate(-16deg);
}

.cup-steam-two {
	left: 42rpx;
	transform: rotate(16deg);
}

.entry-icon-bag {
	position: relative;
	width: 68rpx;
	height: 70rpx;
}

.bag-body {
	position: absolute;
	left: 8rpx;
	right: 8rpx;
	bottom: 6rpx;
	height: 46rpx;
	border: 4rpx solid currentColor;
	border-radius: 8rpx 8rpx 14rpx 14rpx;
}

.bag-handle {
	position: absolute;
	left: 21rpx;
	top: 4rpx;
	width: 26rpx;
	height: 22rpx;
	border: 4rpx solid currentColor;
	border-bottom: 0;
	border-radius: 22rpx 22rpx 0 0;
}

.welcome-card {
	min-height: 136rpx;
	padding: 28rpx;
	border-radius: 28rpx;
	background: rgba(255, 255, 255, 0.96);
	border: 2rpx solid rgba(232, 224, 215, 0.9);
	box-shadow: $shadow-soft;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
	box-sizing: border-box;
}

.welcome-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

.welcome-title {
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.welcome-desc {
	font-size: 23rpx;
	font-weight: 500;
	line-height: 1.55;
	color: $text-secondary;
}

.table-pill {
	flex-shrink: 0;
	min-height: 60rpx;
	padding: 0 22rpx;
	border-radius: 999rpx;
	background: $accent-primary-soft;
	display: flex;
	align-items: center;
	justify-content: center;
}

.table-pill text {
	font-size: 22rpx;
	font-weight: 700;
	color: $text-primary;
}

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
	height: 164rpx;
	flex-shrink: 0;
	border-radius: 16rpx;
	background: $accent-surface;
	display: block;
}

.activity-cover-empty {
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
</style>
