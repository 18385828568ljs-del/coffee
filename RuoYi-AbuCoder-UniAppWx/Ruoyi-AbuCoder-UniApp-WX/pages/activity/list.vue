<template>
	<view class="page">
		<app-nav title="线下活动" />

		<scroll-view class="content" scroll-y>
			<view class="content-wrap">
				<view v-if="activityList.length" class="activity-list">
					<view
						v-for="item in activityList"
						:key="item.activityId"
						class="activity-card"
						@tap="goDetail(item)"
					>
						<image
							v-if="getActivityImage(item)"
							class="activity-image"
							:src="getActivityImage(item)"
							mode="widthFix"
						></image>
						<view v-else class="activity-image activity-image-empty">
							<text>线下活动</text>
						</view>
						<view class="activity-info">
							<view class="activity-top">
								<text class="activity-title">{{ item.title }}</text>
								<text class="activity-status">{{ getSignupText(item) }}</text>
							</view>
							<text class="activity-meta">{{ formatTime(item.startTime) }}</text>
							<text class="activity-meta">{{ item.location || '到店参与' }}</text>
							<view class="activity-bottom">
								<text class="activity-quota">已约 {{ item.signupCount || 0 }}/{{ item.quota || 0 }}</text>
								<text class="activity-link">查看详情</text>
							</view>
						</view>
					</view>
				</view>

				<view v-else class="empty-card">
					<text class="empty-title">暂无线下活动</text>
					<text class="empty-desc">商家发布线下活动后会显示在这里。</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { offlineActivityApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getToken } from '@/utils/auth.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError } from '@/utils/ui-feedback.js'

export default {
	data() {
		return {
			activityList: []
		}
	},

	onLoad() {
		this.loadActivities()
	},

	onPullDownRefresh() {
		this.loadActivities({ stopRefresh: true, noLoading: true })
	},

	methods: {
		async loadActivities(options = {}) {
			if (!options.noLoading) {
				showBusy('加载中...')
			}
			try {
				const res = await requestPromise({
					url: offlineActivityApi.list,
					method: 'GET',
					header: this.getAuthHeader()
				})
				if (isSuccessResponse(res)) {
					this.activityList = Array.isArray(res.data.data) ? res.data.data : []
					return
				}
				this.activityList = []
				showError((res.data && res.data.msg) || '活动加载失败')
			} catch (error) {
				this.activityList = []
				showError('活动加载失败')
			} finally {
				if (!options.noLoading) {
					hideBusy()
				}
				if (options.stopRefresh) {
					uni.stopPullDownRefresh()
				}
			}
		},

		getAuthHeader() {
			const token = getToken()
			return token ? {
				Authorization: `Bearer ${token}`,
				'X-Wx-Token': token
			} : {}
		},

		getActivityImage(item) {
			return item && item.coverImage ? resolveImageUrl(item.coverImage) : ''
		},

		formatTime(value) {
			return value ? String(value).replace(/:\d{2}$/, '') : '时间待定'
		},

		getSignupText(item) {
			if (!item) {
				return ''
			}
			if (item.currentUserSignupStatus === 0) {
				return '已预约'
			}
			if (Number(item.signupCount || 0) >= Number(item.quota || 0)) {
				return '已满员'
			}
			return '可预约'
		},

		goDetail(item) {
			if (!item || !item.activityId) {
				return
			}
			uni.navigateTo({
				url: `/pages/activity/detail?id=${item.activityId}`
			})
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
	padding: 10rpx $space-page 32rpx;
	box-sizing: border-box;
}

.activity-list {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.activity-card {
	@include card(0);
	overflow: hidden;
	@include active-press;
}

.activity-image {
	width: 100%;
	display: block;
	background: $accent-surface;
}

.activity-image-empty {
	display: flex;
	align-items: center;
	justify-content: center;
}

.activity-image-empty text {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.activity-info {
	padding: 24rpx;
	display: flex;
	flex-direction: column;
	gap: 10rpx;
}

.activity-top,
.activity-bottom {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.activity-title,
.activity-status,
.activity-meta,
.activity-quota,
.activity-link,
.empty-title,
.empty-desc {
	font-family: $font-family;
}

.activity-title {
	flex: 1;
	min-width: 0;
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.activity-status {
	flex-shrink: 0;
	padding: 8rpx 14rpx;
	border-radius: 999rpx;
	background: $accent-primary-soft;
	font-size: 20rpx;
	font-weight: 700;
	color: $text-primary;
}

.activity-meta {
	font-size: 24rpx;
	font-weight: 500;
	color: $text-secondary;
}

.activity-quota {
	font-size: 22rpx;
	font-weight: 600;
	color: $text-secondary;
}

.activity-link {
	font-size: 22rpx;
	font-weight: 700;
	color: $text-primary;
}

.empty-card {
	@include card(40rpx 24rpx);
	min-height: 420rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 12rpx;
	text-align: center;
}

.empty-title {
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.empty-desc {
	font-size: 24rpx;
	font-weight: 500;
	color: $text-secondary;
}
</style>
