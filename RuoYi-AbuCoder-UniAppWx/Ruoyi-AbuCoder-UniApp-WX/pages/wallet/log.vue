<template>
	<view class="page">
		<app-nav title="余额明细" fallback-url="/pages/me/me" />

		<scroll-view
			class="content"
			scroll-y
			@scrolltolower="loadMore"
		>
			<view class="content-wrap">
				<!-- 余额卡片 -->
				<view class="balance-card">
					<text class="balance-label">当前余额（元）</text>
					<text class="balance-amount">{{ formatMoney(walletInfo.balance) }}</text>
					<view class="balance-actions">
						<view class="action-btn" @click="goRecharge">
							<text>去充值</text>
						</view>
					</view>
				</view>

				<!-- 流水列表 -->
				<view class="section-card" v-if="logList.length > 0">
					<text class="section-title">收支记录</text>
					<view class="log-list">
						<view
							v-for="(item, index) in logList"
							:key="item.logId || index"
							class="log-row"
						>
							<view class="log-info">
								<text class="log-title">{{ getLogTitle(item) }}</text>
								<text class="log-time">{{ item.createTime || '' }}</text>
								<text v-if="item.remark" class="log-remark">{{ item.remark }}</text>
							</view>
							<text
								class="log-amount"
								:class="Number(item.amount) > 0 ? 'log-amount-in' : 'log-amount-out'"
							>
								{{ Number(item.amount) > 0 ? '+' : '' }}{{ formatMoney(item.amount) }}
							</text>
						</view>
					</view>
				</view>

				<!-- 空状态 -->
				<view v-if="!loading && logList.length === 0" class="empty-state">
					<text class="empty-text">暂无收支记录</text>
					<view class="empty-action" @click="goRecharge">
						<text>去充值</text>
					</view>
				</view>

				<!-- 加载状态 -->
				<view v-if="loading" class="loading-state">
					<text class="loading-text">加载中...</text>
				</view>
				<view v-else-if="noMore && logList.length > 0" class="loading-state">
					<text class="loading-text">没有更多记录了</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { walletApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'

// 流水类型映射
const LOG_TYPE_MAP = {
	1: '充值入账',
	2: '消费扣款',
	3: '退款返还',
	4: '赠送到账'
}

export default {
	data() {
		return {
			walletInfo: {
				balance: 0
			},
			logList: [],
			pageNum: 1,
			pageSize: 20,
			loading: false,
			noMore: false
		}
	},

	onLoad() {
		if (!ensureLocalLogin()) {
			return
		}
		this.loadWalletInfo()
		this.loadLogs()
	},

	onShow() {
		// 从充值页返回时刷新余额
		if (ensureLocalLogin()) {
			this.loadWalletInfo()
		}
	},

	onPullDownRefresh() {
		this.pageNum = 1
		this.noMore = false
		this.logList = []
		Promise.all([
			this.loadWalletInfo(),
			this.loadLogs()
		]).finally(() => {
			uni.stopPullDownRefresh()
		})
	},

	methods: {
		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		getLogTitle(item) {
			return LOG_TYPE_MAP[item.type] || '余额变动'
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
				console.warn('[wallet-log] 加载钱包信息失败', error)
			}
		},

		async loadLogs() {
			if (this.loading || this.noMore) {
				return
			}
			this.loading = true
			try {
				const res = await requestPromise({
					url: walletApi.log,
					method: 'GET',
					data: {
						pageNum: this.pageNum,
						pageSize: this.pageSize
					}
				})
				if (isSuccessResponse(res)) {
					const data = res.data.data
					// 兼容分页数据格式
					const rows = Array.isArray(data) ? data : (data && data.rows ? data.rows : [])
					if (rows.length < this.pageSize) {
						this.noMore = true
					}
					this.logList = this.pageNum === 1 ? rows : this.logList.concat(rows)
					this.pageNum++
				}
			} catch (error) {
				console.warn('[wallet-log] 加载流水失败', error)
			} finally {
				this.loading = false
			}
		},

		loadMore() {
			this.loadLogs()
		},

		goRecharge() {
			uni.navigateTo({
				url: '/pages/wallet/recharge'
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
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
	@include hero-card(28rpx);
	background: linear-gradient(145deg, #3D2B1F 0%, #6F4E37 55%, #9A6C45 100%);
	border: none;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.balance-label {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: rgba(255, 255, 255, 0.7);
}

.balance-amount {
	font-family: $font-family;
	font-size: 56rpx;
	font-weight: 700;
	color: #FFFFFF;
	line-height: 1.1;
}

.balance-actions {
	margin-top: 16rpx;
	display: flex;
}

.action-btn {
	height: 56rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-xs;
	background: rgba(255, 255, 255, 0.2);
	@include active-press;
}

.action-btn text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: #FFFFFF;
}

/* 流水列表 */
.section-card {
	@include card(28rpx);
}

.section-title {
	display: block;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
	margin-bottom: 16rpx;
}

.log-list {
	display: flex;
	flex-direction: column;
}

.log-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
	padding: 20rpx 0;
}

.log-row + .log-row {
	border-top: 2rpx solid $border-light;
}

.log-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6rpx;
}

.log-title {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.log-time {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.log-remark {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.log-amount {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	flex-shrink: 0;
}

.log-amount-in {
	color: $accent-success;
}

.log-amount-out {
	color: $text-primary;
}

/* 空状态 */
.empty-state {
	padding: 100rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 24rpx;
}

.empty-text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.empty-action {
	height: 64rpx;
	padding: 0 36rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.empty-action text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

/* 加载状态 */
.loading-state {
	padding: 32rpx 0;
	text-align: center;
}

.loading-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
}
</style>
