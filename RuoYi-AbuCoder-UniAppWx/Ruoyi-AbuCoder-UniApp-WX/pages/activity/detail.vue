<template>
	<view class="page">
		<app-nav title="活动详情" />

		<scroll-view class="content" scroll-y>
			<view v-if="activity" class="content-wrap">
				<image
					v-if="getActivityImage(activity)"
					class="cover"
					:src="getActivityImage(activity)"
					mode="widthFix"
				></image>
				<view v-else class="cover cover-empty">
					<text>线下活动</text>
				</view>

				<view class="section-card">
					<view class="title-row">
						<text class="title">{{ activity.title }}</text>
						<text class="status-pill">{{ statusText }}</text>
					</view>
					<text v-if="activity.summary" class="summary">{{ activity.summary }}</text>
				</view>

				<view class="section-card">
					<view class="info-row">
						<text class="info-label">时间</text>
						<text class="info-value">{{ formatTimeRange(activity) }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">地点</text>
						<text class="info-value">{{ activity.location || '到店参与' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">名额</text>
						<text class="info-value">已约 {{ activity.signupCount || 0 }}/{{ activity.quota || 0 }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">截止</text>
						<text class="info-value">{{ formatTime(activity.signupDeadline) }}</text>
					</view>
				</view>

				<view v-if="activity.content" class="section-card">
					<text class="section-title">活动详情</text>
					<text class="content-text">{{ activity.content }}</text>
				</view>

				<view class="bottom-space"></view>
			</view>
		</scroll-view>

		<view v-if="activity" class="action-bar">
			<button
				v-if="isReserved"
				class="action-btn action-btn-secondary"
				:disabled="submitting || !canCancel"
				@tap="cancelSignup"
			>
				{{ canCancel ? '取消预约' : '不可取消' }}
			</button>
			<button
				v-else
				class="action-btn"
				:disabled="submitting || !canSignup"
				@tap="submitSignup"
			>
				{{ signupButtonText }}
			</button>
		</view>
	</view>
</template>

<script>
import { offlineActivityApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { getLocalUserId, ensureLocalLogin } from '@/utils/session.js'
import { getToken } from '@/utils/auth.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'

function toTime(value) {
	return value ? new Date(String(value).replace(/-/g, '/')).getTime() : 0
}

export default {
	data() {
		return {
			activityId: '',
			activity: null,
			submitting: false
		}
	},

	computed: {
		isReserved() {
			return this.activity && this.activity.currentUserSignupStatus === 0
		},

		isFull() {
			return this.activity && Number(this.activity.signupCount || 0) >= Number(this.activity.quota || 0)
		},

		isStarted() {
			return this.activity && toTime(this.activity.startTime) > 0 && Date.now() >= toTime(this.activity.startTime)
		},

		isDeadlinePassed() {
			return this.activity && toTime(this.activity.signupDeadline) > 0 && Date.now() > toTime(this.activity.signupDeadline)
		},

		canSignup() {
			return !!(this.activity && !this.isFull && !this.isStarted && !this.isDeadlinePassed)
		},

		canCancel() {
			return !!(this.activity && this.isReserved && !this.isStarted)
		},

		statusText() {
			if (this.isReserved) {
				return '已预约'
			}
			if (this.isStarted) {
				return '已开始'
			}
			if (this.isDeadlinePassed) {
				return '已截止'
			}
			if (this.isFull) {
				return '已满员'
			}
			return '可预约'
		},

		signupButtonText() {
			if (this.isFull) {
				return '名额已满'
			}
			if (this.isStarted) {
				return '活动已开始'
			}
			if (this.isDeadlinePassed) {
				return '预约已截止'
			}
			return this.submitting ? '提交中...' : '立即预约'
		}
	},

	onLoad(options = {}) {
		this.activityId = options.id || ''
		this.loadDetail()
	},

	onPullDownRefresh() {
		this.loadDetail({ stopRefresh: true, noLoading: true })
	},

	methods: {
		async loadDetail(options = {}) {
			if (!this.activityId) {
				showError('活动不存在')
				return
			}
			if (!options.noLoading) {
				showBusy('加载中...')
			}
			try {
				const res = await requestPromise({
					url: offlineActivityApi.detail + this.activityId,
					method: 'GET',
					header: this.getAuthHeader(),
					data: {
						userId: getLocalUserId()
					}
				})
				if (isSuccessResponse(res)) {
					this.activity = res.data.data || null
					return
				}
				showError((res.data && res.data.msg) || '活动加载失败')
			} catch (error) {
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
			return value ? String(value).replace(/:\d{2}$/, '') : '待定'
		},

		formatTimeRange(item) {
			if (!item) {
				return '待定'
			}
			const start = this.formatTime(item.startTime)
			const end = item.endTime ? this.formatTime(item.endTime) : ''
			return end ? `${start} - ${end}` : start
		},

		async submitSignup() {
			if (!ensureLocalLogin('请先登录后预约', { redirect: true })) {
				return
			}
			if (!this.canSignup || this.submitting) {
				return
			}
			this.submitting = true
			try {
				const res = await requestPromise({
					url: offlineActivityApi.signup,
					method: 'POST',
					header: this.getAuthHeader(),
					data: {
						activityId: this.activityId
					}
				})
				if (isSuccessResponse(res)) {
					showSuccess('预约成功')
					await this.loadDetail({ noLoading: true })
					return
				}
				showError((res.data && res.data.msg) || '预约失败')
			} catch (error) {
				showError('预约失败')
			} finally {
				this.submitting = false
			}
		},

		async cancelSignup() {
			if (!this.canCancel || this.submitting) {
				return
			}
			const confirmed = await showConfirm({
				title: '取消预约',
				content: '确定取消这次线下活动预约吗？'
			})
			if (!confirmed) {
				return
			}
			this.submitting = true
			try {
				const res = await requestPromise({
					url: offlineActivityApi.cancel,
					method: 'POST',
					header: this.getAuthHeader(),
					data: {
						activityId: this.activityId
					}
				})
				if (isSuccessResponse(res)) {
					showSuccess('已取消预约')
					await this.loadDetail({ noLoading: true })
					return
				}
				showError((res.data && res.data.msg) || '取消失败')
			} catch (error) {
				showError('取消失败')
			} finally {
				this.submitting = false
			}
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
	padding: 10rpx $space-page 0;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.cover {
	width: 100%;
	display: block;
	border-radius: 20rpx;
	background: $accent-surface;
	overflow: hidden;
}

.cover-empty {
	display: flex;
	align-items: center;
	justify-content: center;
}

.cover-empty text {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 700;
	color: $text-primary;
}

.section-card {
	@include card(28rpx);
}

.title-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
}

.title,
.status-pill,
.summary,
.section-title,
.content-text,
.info-label,
.info-value,
.action-btn {
	font-family: $font-family;
}

.title {
	flex: 1;
	min-width: 0;
	font-size: 40rpx;
	font-weight: 800;
	line-height: 1.25;
	color: $text-primary;
}

.status-pill {
	flex-shrink: 0;
	padding: 8rpx 16rpx;
	border-radius: 999rpx;
	background: $accent-primary-soft;
	font-size: 22rpx;
	font-weight: 700;
	color: $text-primary;
}

.summary {
	display: block;
	margin-top: 18rpx;
	font-size: 26rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.info-row {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
	padding: 14rpx 0;
}

.info-row + .info-row {
	border-top: 2rpx solid $border-light;
}

.info-label {
	width: 84rpx;
	flex-shrink: 0;
	font-size: 24rpx;
	font-weight: 700;
	color: $text-primary;
}

.info-value {
	flex: 1;
	min-width: 0;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.5;
	color: $text-secondary;
}

.section-title {
	display: block;
	margin-bottom: 14rpx;
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.content-text {
	font-size: 26rpx;
	font-weight: 500;
	line-height: 1.7;
	color: $text-secondary;
	white-space: pre-wrap;
}

.bottom-space {
	height: 148rpx;
}

.action-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 90;
	padding: 18rpx $space-page calc(18rpx + env(safe-area-inset-bottom));
	background: rgba(255, 255, 255, 0.96);
	border-top: 2rpx solid $border-light;
	box-sizing: border-box;
}

.action-btn {
	width: 100%;
	height: 88rpx;
	border: 0;
	border-radius: 16rpx;
	background: $accent-primary;
	color: #FFFFFF;
	font-size: 28rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
}

.action-btn::after {
	display: none;
}

.action-btn[disabled] {
	opacity: 0.56;
}

.action-btn-secondary {
	background: $bg-muted;
	color: $text-primary;
}
</style>
