<template>
	<view class="page" :style="themePageStyle">
		<scroll-view class="content" scroll-y>
			<view class="brand-strip-actions">
				<view v-if="decoratorPreview && !isLivePreview" class="skin-switcher" @tap="toggleLocalSkin">
					<text>{{ activeSkinLabel }}</text>
					<text class="skin-switcher-arrow">↗</text>
				</view>
			</view>
			<view v-if="themeBannerItems.length" class="hero-section" data-skin-component="homeBanner" :style="themeSkinSlotStyle('heroBanner')">
				<swiper
					class="hero-swiper"
					autoplay
					circular
					indicator-dots
					indicator-active-color="#FFFFFF"
					indicator-color="rgba(255, 255, 255, 0.42)"
				>
					<swiper-item v-for="(item, index) in themeBannerItems" :key="item.id || index">
						<image
							v-if="item.image"
							class="hero-image"
							:src="item.image"
							mode="aspectFill"
							@error="handleBannerImageError(index)"
							@tap="handleBannerTap(item.source)"
						></image>
						<view v-else class="hero-image hero-fallback"></view>
					</swiper-item>
				</swiper>
				<image v-if="themeSkinDecorationUrl('homeBannerArtText')" class="art-text-layer" :src="themeSkinDecorationUrl('homeBannerArtText')" :style="themeSkinDecorationStyle('homeBannerArtText')" mode="widthFix"></image>
				<view v-if="homeBannerContent.visible && (homeBannerContent.title || homeBannerContent.subtitle)" class="hero-copy" :style="themeSkinLayoutStyle('homeBanner')">
					<text v-if="homeBannerContent.title" class="hero-title" data-text-role="bannerTitle">{{ homeBannerContent.title }}</text>
					<text v-if="homeBannerContent.subtitle" class="hero-subtitle" data-text-role="bannerSubtitle">{{ homeBannerContent.subtitle }}</text>
				</view>
			</view>

			<view class="content-wrap">
				<view class="entry-grid">
					<view class="entry-card entry-card-primary" data-skin-component="actionCard" :style="[entryCardStyle('orderCard'), themeSkinAssetStyle('actionCard')]" @tap="startOrder">
						<view class="entry-icon">
							<image class="entry-icon-image" src="/static/home/order.svg" mode="aspectFit"></image>
						</view>
						<text class="entry-title" data-text-role="actionTitle" :style="entryTextStyle('orderCard')">点单</text>
						<text class="entry-label" data-text-role="actionSubtitle" :style="entrySecondaryTextStyle('orderCard')">ORDER</text>
					</view>

					<view class="entry-card entry-card-secondary" data-skin-component="actionCard" :style="[entryCardStyle('shopCard'), themeSkinAssetStyle('actionCard')]" @tap="goMall">
						<view class="entry-icon">
							<image class="entry-icon-image" src="/static/home/shop.svg" mode="aspectFit"></image>
						</view>
						<text class="entry-title" data-text-role="actionTitle" :style="entryTextStyle('shopCard')">商店</text>
						<text class="entry-label" data-text-role="actionSubtitle" :style="entrySecondaryTextStyle('shopCard')">SHOP</text>
					</view>
				</view>

				<view class="welcome-image-wrap" data-skin-component="sectionBanner">
					<image class="welcome-image" :src="themeSkinAsset('sectionBanner') || welcomeImage" :style="themeSkinSlotStyle('welcomeBanner')" mode="widthFix"></image>
					<image v-if="themeSkinDecorationUrl('sectionBannerArtText')" class="art-text-layer" :src="themeSkinDecorationUrl('sectionBannerArtText')" :style="themeSkinDecorationStyle('sectionBannerArtText')" mode="widthFix"></image>
				</view>

				<view v-if="featuredActivity" class="activity-section">
					<view class="activity-section-head">
						<text class="activity-section-title" data-text-role="sectionTitle">参与线下活动</text>
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

				<view class="about-section" :style="aboutSectionStyle">
					<view class="about-section-head">
						<text class="about-section-title" data-text-role="sectionTitle">关于我们</text>
					</view>
					<image class="about-image" data-skin-component="aboutImage" :src="aboutImage" mode="widthFix"></image>
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

		<!-- #ifdef H5 -->
		<bottom-tab-bar current="home" />
		<!-- #endif -->
	</view>
</template>

<script>
import { bannerApi, offlineActivityApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getToken } from '@/utils/auth.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { loginByWxAuth } from '@/utils/wx-login.js'
import { getLocalUserInfo } from '@/utils/session.js'
import { themeRuntime } from '@/theme/runtime.js'

const DEFAULT_STORE_CODE = '1'
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
			shopName: DEFAULT_SHOP_NAME,
			storeCode: '',
			tableNo: '',
			bannerList: [],
			activityList: [],
			bannerImageErrorMap: {},
			showWxLogin: false,
			wxLoginLoading: false,
			decoratorPreview: false,
			previewToken: ''
		}
	},

	async onLoad(options = {}) {
		this.resolveEntryContext(options)
		this.decoratorPreview = this.themePreviewMode || String(options.decoratorPreview || '') === '1'
		const sceneToken = String(options.scene || '').trim()
		this.previewToken = String(options.previewToken || this.previewToken || '').trim()
		if (!this.previewToken && /^(?:[A-Za-z0-9_-]{22}|[A-Za-z0-9_-]{43})$/.test(sceneToken)) this.previewToken = sceneToken
		if (this.decoratorPreview) {
			themeRuntime.setPreviewMode(true)
			if (this.previewToken) {
				const preview = await themeRuntime.loadPreview(this.storeCode, this.previewToken)
				if (preview && preview.storeCode && !this.storeCode) this.storeCode = preview.storeCode
			}
		} else {
			await themeRuntime.loadPublished(this.storeCode)
		}
		this.loadBanners()
		this.loadActivities()
		this.maybeShowWxLogin()
	},

	async onShow() {
		if (!this.decoratorPreview) await themeRuntime.loadPublished(this.storeCode)
		this.maybeShowWxLogin()
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
		homeBannerContent() {
			return this.themeSkinContent('homeBanner')
		},
		themeBannerItems() {
				const skinImages = this.themeSkinAssets('homeBanner')
				if (skinImages.length) {
					return skinImages.map((image, index) => ({
						id: `skin-home-banner-${index}`,
						image,
						title: '一杯好咖啡',
						source: null
					}))
				}
				const hero = this.themeSkinSlot('heroBanner')
				const heroImage = hero.backgroundImage || this.themeAssetUrl(hero.assetId)
				if (hero.backgroundType === 'image' && heroImage) {
					return [{ id: 'skin-hero-banner', image: heroImage, title: '当季推荐', source: null }]
			}
			const items = this.bannerList.map((item, index) => ({
				id: this.getBannerKey(item, index),
				image: this.getBannerImage(item, index),
				title: item.bannerTitle || '',
				source: item
			})).filter((item) => item.image)
			return items.length ? items : [{ id: 'default-home-banner', image: '/static/banner/welcome.png', title: 'Fresh coffee', source: null }]
		},
		homeEntries() {
			return [
				{ key: 'order', skinSlot: 'orderCard', label: '点单', icon: '/static/home/order.svg' },
				{ key: 'mall', skinSlot: 'shopCard', label: '商店', icon: '/static/home/shop.svg' }
			]
		},
		welcomeImage() {
			const slot = this.themeSkinSlot('welcomeBanner')
			return slot.backgroundType === 'image' && slot.backgroundImage ? slot.backgroundImage : '/static/banner/welcome.png'
		},
		aboutImage() {
			return this.themeSkinAsset('aboutImage')
				|| this.themeSkinSlot('aboutSection').image
				|| '/static/banner/about-us.jpg'
		},
		aboutSectionStyle() {
			const slot = this.themeSkinSlot('aboutSection')
			return {
				backgroundColor: 'transparent',
				'--about-title-color': slot.titleColor || 'var(--theme-text)'
			}
		},
		featuredActivity() {
			return this.activityList[0] || null
		},
		activeSkinLabel() {
			return themeRuntime.state.versionId === 'midnight' ? '夜幕' : '复古'
		},
		isLivePreview() {
			return themeRuntime.state.source === 'PREVIEW'
		},
	},

	methods: {
		entryCardStyle(slotKey) {
			return this.themeSkinSlotStyle(slotKey)
		},
		entryTextStyle(slotKey) {
			const slot = this.themeSkinSlot(slotKey)
			const token = this.themeTypographyToken('actionTitle')
			return { color: token.color || slot.textColor || '' }
		},
		entrySecondaryTextStyle(slotKey) {
			const slot = this.themeSkinSlot(slotKey)
			const token = this.themeTypographyToken('actionSubtitle')
			return { color: token.color || slot.secondaryTextColor || '' }
		},
		toggleLocalSkin() {
			const next = themeRuntime.state.versionId === 'midnight' ? 'vintage' : 'midnight'
			themeRuntime.useLocalSkin(next)
			uni.showToast({ title: next === 'midnight' ? '已切换夜幕皮肤' : '已切换复古皮肤', icon: 'none' })
		},
		resolveEntryContext(options = {}) {
			const sceneData = parseScene(options.scene)
			const tableNo = options.tableNo || sceneData.tableNo
			const shopName = options.shopName || sceneData.shopName
			const storeCode = options.storeCode || sceneData.storeCode
			this.previewToken = options.previewToken || sceneData.previewToken || ''

			this.tableNo = tableNo ? String(tableNo).trim() : ''
			this.shopName = shopName ? decodeValue(shopName) : DEFAULT_SHOP_NAME
			// 开发者工具直接打开首页时没有 scene/storeCode，仍需加载默认门店的线上皮肤。
			this.storeCode = storeCode ? String(storeCode).trim() : DEFAULT_STORE_CODE
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
			const context = {}
			if (this.storeCode) context.storeCode = this.storeCode
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
		},

		maybeShowWxLogin() {
			if (this.decoratorPreview) {
				this.showWxLogin = false
				return
			}
			const userInfo = getLocalUserInfo()
			const dismissed = uni.getStorageSync(WX_LOGIN_DISMISSED_KEY)
			this.showWxLogin = !(userInfo && userInfo.userId) && !dismissed
		},

		dismissWxLogin() {
			uni.setStorageSync(WX_LOGIN_DISMISSED_KEY, true)
			this.showWxLogin = false
		},

		handleWxLogin() {
			if (this.decoratorPreview) return
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
	background: var(--theme-page);
}

.content {
	flex: 1;
	min-height: 0;
}

.brand-strip-actions { position: absolute; top: 24rpx; right: $space-page; z-index: 4; }

.brand-mark {
	width: 72rpx;
	height: 72rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border: 2rpx solid var(--theme-primary);
	border-radius: 50%;
	color: var(--theme-primary);
	font-size: 38rpx;
	font-weight: 800;
}

.brand-copy {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 2rpx;
}

.brand-name { color: var(--theme-text); font-size: 34rpx; font-weight: 800; letter-spacing: 2rpx; }
.brand-caption { color: var(--theme-text-secondary); font-size: 21rpx; letter-spacing: 3rpx; }

.skin-switcher {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 12rpx 16rpx;
	border: 2rpx solid var(--theme-primary);
	border-radius: 999rpx;
	color: var(--theme-primary);
	font-size: 21rpx;
	font-weight: 700;
	@include active-press;
}

.skin-switcher-arrow { font-size: 26rpx; line-height: 1; }

.hero-section {
	position: relative;
	display: block;
	width: 100%;
	/* Keep the carousel prominent on the home page while preserving its fixed layout. */
	height: 420rpx;
	overflow: hidden;
	background-color: var(--theme-surface, #ffffff);
}

.hero-swiper,
.hero-image {
	width: 100%;
	height: 420rpx;
	display: block;
}

.hero-copy {
	position: absolute;
	left: 42rpx;
	top: 50%;
	width: 54%;
	z-index: 2;
	display: flex;
	flex-direction: column;
	gap: 14rpx;
	transform: translateY(-50%);
	pointer-events: none;
}

.welcome-image-wrap {
	position: relative;
}

.art-text-layer {
	position: absolute;
	z-index: 3;
	height: auto;
	pointer-events: none;
}

.hero-title {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	word-break: break-word;
	font-size: var(--skin-banner-title-size, 52rpx);
	font-weight: var(--skin-banner-title-weight, 800);
	line-height: var(--skin-banner-title-line-height, 1.2);
	letter-spacing: var(--skin-banner-title-letter-spacing, 0);
	color: var(--skin-banner-title-color, var(--theme-text));
	text-shadow: var(--skin-banner-title-shadow, none);
}

.hero-subtitle {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	word-break: break-word;
	font-size: var(--skin-banner-subtitle-size, 24rpx);
	font-weight: var(--skin-banner-subtitle-weight, 500);
	line-height: var(--skin-banner-subtitle-line-height, 1.5);
	color: var(--skin-banner-subtitle-color, var(--theme-text-secondary));
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
	background: var(--theme-surface);
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
	background: var(--theme-surface);
	border-color: rgba(232, 224, 215, 0.9);
	box-shadow: 0 10rpx 26rpx rgba(32, 26, 23, 0.08);
}

.entry-card-secondary {
	background: var(--theme-surface);
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
	font-size: var(--skin-action-title-size, 30rpx);
	font-weight: var(--skin-action-title-weight, 700);
	letter-spacing: var(--skin-action-title-letter-spacing, 4rpx);
	text-indent: 4rpx;
	color: var(--skin-action-title-color, #{$text-primary});
}

.entry-label {
	margin-top: 6rpx;
	font-size: var(--skin-action-subtitle-size, 17rpx);
	font-weight: var(--skin-action-subtitle-weight, 700);
	letter-spacing: var(--skin-action-subtitle-letter-spacing, 2rpx);
	color: var(--skin-action-subtitle-color, #{$text-tertiary});
}

.entry-card-primary .entry-label {
	color: $accent-warm;
}

.welcome-image {
	width: 100%;
	display: block;
}

.welcome-banner {
	overflow: hidden;
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
	font-size: var(--skin-section-title-size, 32rpx);
	font-weight: var(--skin-section-title-weight, 700);
	line-height: var(--skin-section-title-line-height, 1.35);
	letter-spacing: var(--skin-section-title-letter-spacing, 0);
	color: var(--skin-section-title-color, #{$text-primary});
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
	font-size: var(--skin-section-title-size, 32rpx);
	font-weight: var(--skin-section-title-weight, 700);
	line-height: var(--skin-section-title-line-height, 1.35);
	letter-spacing: var(--skin-section-title-letter-spacing, 0);
	color: var(--skin-section-title-color, var(--about-title-color));
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
