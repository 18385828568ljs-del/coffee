<template>
	<view class="page">
		<app-nav :title="campaign ? campaign.title : '今日抽卡'" right-text="我的卡片" @right="goMine" />
		<scroll-view class="content" scroll-y>
			<view v-if="loading" class="state-view"><text>正在准备卡片...</text></view>
			<view v-else-if="!campaign" class="state-view">
				<view class="empty-mark">CARD</view><text class="state-title">当前没有抽卡活动</text><text class="state-desc">新卡片正在准备中</text>
			</view>
			<view v-else class="draw-wrap">
				<view class="campaign-copy"><text class="campaign-title">{{ campaign.title }}</text><text class="campaign-subtitle">{{ campaign.subtitle || '抽取属于你的今日咖啡卡片' }}</text></view>
				<view class="card-stage">
					<view class="card-flipper" :class="{ revealed }">
						<view class="card-face card-back" @tap="drawCard">
							<image v-if="campaign.coverImage" class="card-cover" :src="resolveImageUrl(campaign.coverImage)" mode="aspectFill"></image>
							<view v-else class="card-back-pattern"><text class="back-small">COFFEE</text><text class="back-main">DRAW</text><view class="back-seal"><text>今日</text></view></view>
						</view>
						<view class="card-face card-front"><image v-if="result" class="result-image" :src="resolveImageUrl(result.finalImageUrl)" mode="aspectFit"></image></view>
					</view>
				</view>
				<button v-if="!result" class="draw-button" :disabled="drawing" @tap="drawCard">{{ drawing ? '抽取中...' : '抽取卡片' }}</button>
				<view v-else class="result-actions">
					<button v-if="result.leftProductId" class="action-primary" @tap="openProduct(result.leftProductType, result.leftProductId)">查看推荐</button>
					<button class="action-secondary" @tap="saveCard">保存卡片</button>
				</view>
				<text class="rule-text">每位用户在本次活动中可抽取一次</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { cardApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getToken } from '@/utils/auth.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { showBusy, hideBusy, showError, showSuccess } from '@/utils/ui-feedback.js'

export default {
	data() { return { loading: true, drawing: false, revealed: false, campaign: null, result: null } },
	onLoad() { this.loadCampaign() },
	onPullDownRefresh() { this.loadCampaign().finally(() => uni.stopPullDownRefresh()) },
	onShareAppMessage() { return { title: this.campaign ? this.campaign.title : '今日咖啡卡片', path: '/pages/card/draw' } },
	methods: {
		resolveImageUrl,
		authHeader() { const token = getToken(); return token ? { Authorization: `Bearer ${token}`, 'X-Wx-Token': token } : {} },
		async loadCampaign() {
			this.loading = true
			try {
				const res = await requestPromise({ url: cardApi.active, method: 'GET', header: this.authHeader() })
				this.campaign = isSuccessResponse(res) ? (res.data.data || null) : null
				this.result = null; this.revealed = false
				if (this.campaign && this.campaign.currentUserDrawId) await this.loadResult(this.campaign.currentUserDrawId)
			} catch (error) { this.campaign = null } finally { this.loading = false }
		},
		async loadResult(drawId) {
			try {
				const res = await requestPromise({ url: `${cardApi.result}${drawId}`, method: 'GET', header: this.authHeader() })
				if (isSuccessResponse(res)) { this.result = res.data.data; this.revealed = true }
			} catch (error) {}
		},
		async drawCard() {
			if (this.drawing || this.result || !this.campaign) return
			if (!ensureLocalLogin('请先登录后抽卡')) return
			this.drawing = true
			try {
				const requestNo = `card-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
				const res = await requestPromise({ url: cardApi.draw, method: 'POST', header: { ...this.authHeader(), 'Content-Type': 'application/json' }, data: { campaignId: this.campaign.campaignId, requestNo } })
				if (!isSuccessResponse(res)) { showError((res.data && res.data.msg) || '抽卡失败'); return }
				this.result = res.data.data
				setTimeout(() => { this.revealed = true }, 120)
			} catch (error) { showError('抽卡失败，请稍后重试') } finally { this.drawing = false }
		},
		openProduct(type, id) {
			if (type === 'mall') { uni.navigateTo({ url: `/pages/product/detail?id=${id}` }); return }
			uni.switchTab({ url: '/pages/scan/menu' })
		},
		goMine() { if (ensureLocalLogin('请先登录查看卡片')) uni.navigateTo({ url: '/pages/card/mine' }) },
		saveCard() {
			if (!this.result || !this.result.finalImageUrl) return
			showBusy('保存中...')
			uni.downloadFile({ url: resolveImageUrl(this.result.finalImageUrl), success: (download) => {
				if (download.statusCode !== 200) { hideBusy(); showError('图片下载失败'); return }
				uni.saveImageToPhotosAlbum({ filePath: download.tempFilePath, success: () => { hideBusy(); showSuccess('已保存到相册') }, fail: () => { hideBusy(); showError('请允许保存到相册') } })
			}, fail: () => { hideBusy(); showError('图片下载失败') } })
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';
.page{@include page-shell;min-height:100vh;background:#f8f5f0}.content{flex:1;min-height:0}.draw-wrap{padding:12rpx 42rpx 60rpx;display:flex;flex-direction:column;align-items:center}.campaign-copy{text-align:center;margin-bottom:24rpx}.campaign-title,.campaign-subtitle,.rule-text,.state-view text{font-family:$font-family}.campaign-title{display:block;font-size:38rpx;font-weight:800;color:#201a17}.campaign-subtitle{display:block;margin-top:10rpx;font-size:24rpx;color:#6e655e}.card-stage{width:560rpx;height:747rpx;perspective:1600rpx}.card-flipper{position:relative;width:100%;height:100%;transition:transform .75s ease;transform-style:preserve-3d}.card-flipper.revealed{transform:rotateY(180deg)}.card-face{position:absolute;inset:0;backface-visibility:hidden;overflow:hidden;box-shadow:0 18rpx 40rpx rgba(53,31,20,.22)}.card-back{background:#5c2a12;border:10rpx solid #201a17;box-sizing:border-box}.card-cover,.result-image{width:100%;height:100%;display:block}.card-front{transform:rotateY(180deg);background:#5c2a12}.card-back-pattern{height:100%;margin:24rpx;border:5rpx solid #f6df54;background-color:#0f8bac;background-image:linear-gradient(45deg,#f486a4 25%,transparent 25%,transparent 75%,#f486a4 75%),linear-gradient(45deg,#f6df54 25%,transparent 25%,transparent 75%,#f6df54 75%);background-size:100rpx 100rpx;background-position:0 0,50rpx 50rpx;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#201a17}.back-small{font-size:34rpx;font-weight:900}.back-main{font-size:86rpx;font-weight:900}.back-seal{width:116rpx;height:116rpx;margin-top:28rpx;border:7rpx solid #201a17;border-radius:50%;background:#f6df54;display:flex;align-items:center;justify-content:center;font-size:34rpx;font-weight:800}.draw-button,.action-primary,.action-secondary{height:88rpx;border-radius:8rpx;font-family:$font-family;font-size:28rpx;font-weight:700;display:flex;align-items:center;justify-content:center}.draw-button{width:560rpx;margin-top:28rpx;background:#7a4f2d;color:#fff}.draw-button::after,.action-primary::after,.action-secondary::after{display:none}.result-actions{width:560rpx;margin-top:28rpx;display:grid;grid-template-columns:1fr 1fr;gap:16rpx}.action-primary{background:#7a4f2d;color:#fff}.action-secondary{background:#fff;color:#7a4f2d;border:2rpx solid #d7ccbf}.rule-text{margin-top:18rpx;font-size:22rpx;color:#9a9188}.state-view{min-height:720rpx;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#6e655e}.empty-mark{width:190rpx;height:240rpx;border:8rpx solid #5c2a12;background:#f6df54;display:flex;align-items:center;justify-content:center;font-size:34rpx;font-weight:900;transform:rotate(-4deg)}.state-title{margin-top:36rpx;font-size:30rpx;font-weight:700}.state-desc{margin-top:10rpx;font-size:24rpx;color:#9a9188}
.card-flipper{-webkit-transform-style:preserve-3d}.card-face{-webkit-backface-visibility:hidden}
</style>
