<template>
	<view class="page">
		<app-nav title="我的订单" fallback-url="/pages/me/me" />

		<view class="order-type-switch">
			<view
				class="order-type-item"
				:class="{ active: orderType === 'mall' }"
				@tap="switchOrderType('mall')"
			>
				<text>商城订单</text>
			</view>
			<view
				class="order-type-item"
				:class="{ active: orderType === 'scan' }"
				@tap="switchOrderType('scan')"
			>
				<text>点单订单</text>
			</view>
		</view>

		<scroll-view class="tabs-scroll" scroll-x>
			<view class="tabs">
				<view
					v-for="(tab, index) in tabs"
					:key="index"
					class="tab-item"
					:class="{ active: currentTab === index }"
					@tap="switchTab(index)"
				>
					<text class="tab-text">{{ tab.name }}</text>
				</view>
			</view>
		</scroll-view>

		<scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
			<navigator
				v-for="(order, index) in orderList"
				:key="order.orderId || index"
				class="order-card"
				:url="getDetailUrl(order)"
				open-type="navigate"
				hover-class="none"
			>
				<view class="order-card-header">
					<view class="order-title-group">
						<text class="order-type-tag">{{ order.typeLabel }}</text>
						<text class="order-no">订单号 {{ order.orderNo }}</text>
						<text class="order-time">{{ order.createTime || '' }}</text>
					</view>
					<text class="order-status" :style="{ color: getStatusColor(order.status) }">
						{{ getStatusText(order.status) }}
					</text>
				</view>

				<view
					v-for="(product, idx) in order.items"
					:key="idx"
					class="product-row"
				>
					<image
						v-if="getOrderImage(product)"
						class="product-image"
						:src="getOrderImage(product)"
						mode="aspectFill"
					></image>
					<view v-else class="product-image product-image-empty"></view>
					<view class="product-info">
						<text class="product-name">{{ product.productName }}</text>
						<text class="product-spec">{{ product.spec || '现烘发货 / 新鲜包装' }}</text>
						<view class="product-meta">
							<text class="product-price">¥{{ formatMoney(product.price) }}</text>
							<text class="product-quantity">x{{ product.quantity }}</text>
						</view>
					</view>
				</view>

				<view v-if="orderType === 'scan' && (order.shopName || order.tableNo)" class="summary-extra">
					<text class="summary-text">{{ order.shopName || '咖啡门店' }}{{ order.tableNo ? ' · ' + order.tableNo + '桌' : '' }}</text>
				</view>
				<view v-if="orderType === 'scan' && (order.pickupNo || order.estimatedWaitMinutes)" class="summary-extra scan-progress-extra">
					<text v-if="order.pickupNo" class="summary-text">取餐号 {{ order.pickupNo }}</text>
					<text v-if="order.estimatedWaitMinutes" class="summary-text">预计等待 {{ order.estimatedWaitMinutes }} 分钟</text>
				</view>

				<view v-if="orderType === 'mall' && (order.deliveryTime || order.expressNo)" class="summary-extra">
					<text v-if="order.deliveryTime" class="summary-text">发货时间 {{ order.deliveryTime }}</text>
					<text v-if="order.expressNo" class="summary-text">物流单号 {{ order.expressNo }}</text>
				</view>

				<view class="order-footer">
					<view class="summary-price-wrap">
						<text class="summary-price">合计 ¥{{ formatMoney(order.payAmount || order.totalAmount) }}</text>
					</view>

					<view class="order-actions">
						<view
							v-if="order.status === 0"
							class="action-btn"
							:class="{ 'action-btn-disabled': isOrderPending(order.orderId) }"
							@tap.stop="cancelOrder(order.orderId)"
						>
							<text>{{ isOrderActionPending(order.orderId, 'cancel') ? '取消中...' : '取消订单' }}</text>
						</view>
						<view
							v-if="order.status === 0"
							class="action-btn action-btn-primary"
							:class="{ 'action-btn-disabled': isOrderPending(order.orderId) }"
							@tap.stop="payOrder(order)"
						>
							<text>{{ isOrderActionPending(order.orderId, 'pay') ? '支付中...' : '立即支付' }}</text>
						</view>
						<view
							v-if="orderType === 'mall' && order.status === 2"
							class="action-btn action-btn-light"
							:class="{ 'action-btn-disabled': isOrderPending(order.orderId) }"
							@tap.stop="confirmOrder(order.orderId)"
						>
							<text>{{ isOrderActionPending(order.orderId, 'confirm') ? '确认中...' : '确认收货' }}</text>
						</view>
						<view
							v-if="orderType === 'scan' && canUrgeOrder(order)"
							class="action-btn action-btn-light"
							:class="{ 'action-btn-disabled': isOrderPending(order.orderId) }"
							@tap.stop="urgeOrder(order.orderId)"
						>
							<text>{{ isOrderActionPending(order.orderId, 'urge') ? '催单中...' : '催单' }}</text>
						</view>
					</view>
				</view>
			</navigator>

			<view class="load-more" v-if="loading">
				<text>加载中...</text>
			</view>

			<view class="no-more" v-else-if="!hasMore && orderList.length > 0">
				<text>没有更多了</text>
			</view>

			<view class="empty-order" v-if="orderList.length === 0 && !loading">
				<image class="empty-image" src="/static/empty-order.svg" mode="aspectFit"></image>
				<text class="empty-title">还没有订单</text>
				<text class="empty-text">{{ orderType === 'scan' ? '扫码点单后，这里会显示点单订单。' : '去首页挑选几款合适的咖啡豆吧。' }}</text>
			</view>
		</scroll-view>

		<result-dialog
			:visible="paySuccessVisible"
			icon="/static/order-status/complete.svg"
			title="支付成功"
			description="订单状态已更新，现在可以直接查看订单详情。"
			confirm-text="查看订单"
			cancel-text="稍后查看"
			:show-cancel="true"
			@confirm="goToPaidOrder"
			@cancel="closePaySuccessDialog"
		/>
	</view>
</template>

<script>
import ResultDialog from '@/components/result-dialog.vue'
import { orderApi, scanOrderApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'
import { getToken } from '@/utils/auth.js'

const ORDER_TABS = [
	{ name: '全部', status: null },
	{ name: '待付款', status: 0 },
	{ name: '待发货', status: 1 },
	{ name: '待收货', status: 2 },
	{ name: '已完成', status: 3 }
]

const SCAN_ORDER_TABS = [
	{ name: '全部', status: null },
	{ name: '待付款', status: 0 },
	{ name: '制作中', status: 2 },
	{ name: '待取餐', status: 3 },
	{ name: '已完成', status: 4 },
	{ name: '已取消', status: 5 }
]

const ORDER_STATUS_META = {
	0: { text: '待付款', color: '#9A6C45' },
	1: { text: '待发货', color: '#84776C' },
	2: { text: '待收货', color: '#8D7C70' },
	3: { text: '已完成', color: '#6E655E' },
	4: { text: '已取消', color: '#C86E60' }
}

const SCAN_ORDER_STATUS_META = {
	0: { text: '待付款', color: '#9A6C45' },
	2: { text: '制作中', color: '#84776C' },
	3: { text: '待取餐', color: '#9A6C45' },
	4: { text: '已完成', color: '#6E655E' },
	5: { text: '已取消', color: '#C86E60' }
}

export default {
	components: {
		ResultDialog
	},
	data() {
		return {
			orderType: 'mall',
			currentTab: 0,
			orderList: [],
			pageNum: 1,
			pageSize: 10,
			hasMore: true,
			loading: false,
			pendingOrderActionKey: '',
			paySuccessVisible: false,
			paidOrderId: null
		}
	},

	computed: {
		tabs() {
			return this.orderType === 'scan' ? SCAN_ORDER_TABS : ORDER_TABS
		}
	},

	onLoad(options) {
		if (options.type === 'scan') {
			this.orderType = 'scan'
		}
		if (options.tab !== undefined) {
			const tabIndex = Number(options.tab)
			if (!Number.isNaN(tabIndex) && tabIndex >= 0 && tabIndex < this.tabs.length) {
				this.currentTab = tabIndex
			}
		}
	},

	onShow() {
		this.resetList()
		this.loadOrders()
	},

	onPullDownRefresh() {
		this.resetList()
		this.loadOrders()
	},

	methods: {
		authHeader() {
			const token = getToken()
			const header = {}
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},

		formatMoney(value) {
			return Number(value || 0).toFixed(2)
		},

		getOrderImage(item) {
			return resolveImageUrl(item.productImg || item.productImage || item.imageUrl)
		},

		getDetailUrl(order) {
			if (!order || !order.orderId) {
				return ''
			}
			if (this.orderType === 'scan') {
				return `/pages/scan/detail?orderId=${order.orderId}`
			}
			return `/pages/order/detail?id=${order.orderId}`
		},

		canUrgeOrder(order) {
			if (!order || this.orderType !== 'scan') return false
			return order.status === 2 || order.status === 3
		},

		switchOrderType(type) {
			if (type === this.orderType) return
			this.orderType = type
			this.currentTab = 0
			this.resetList()
			this.loadOrders()
		},

		resetList() {
			this.pageNum = 1
			this.orderList = []
			this.hasMore = true
		},

		switchTab(index) {
			this.currentTab = index
			this.resetList()
			this.loadOrders()
		},

		loadOrders() {
			if (this.orderType === 'scan') {
				this.loadScanOrders()
				return
			}
			if (this.loading || !this.hasMore) {
				return
			}

			this.loading = true
			const status = this.tabs[this.currentTab].status
			const requestData = status !== null && status !== undefined
				? { pageNum: this.pageNum, pageSize: this.pageSize, status }
				: { pageNum: this.pageNum, pageSize: this.pageSize }
			requestPromise({
				url: orderApi.list,
				method: 'GET',
				data: requestData,
				header: this.authHeader()
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						const orders = (res.data.rows || []).map((order) => ({
							...order,
							orderId: order.orderId || order.id || null,
							items: order.items || order.orderItems || [],
							typeLabel: '商城订单'
						}))
						this.orderList = [...this.orderList, ...orders]
						this.hasMore = orders.length >= this.pageSize
						return
					}
					this.hasMore = false
					showError((res.data && res.data.msg) || '加载失败')
				})
				.catch(() => {
					showError('加载失败')
				})
				.finally(() => {
					this.loading = false
					uni.stopPullDownRefresh()
				})
		},

		loadScanOrders() {
			if (this.loading || !this.hasMore) {
				return
			}

			this.loading = true
			const status = this.tabs[this.currentTab].status
			const requestData = status !== null && status !== undefined
				? { pageNum: this.pageNum, pageSize: this.pageSize, status }
				: { pageNum: this.pageNum, pageSize: this.pageSize }
			requestPromise({
				url: scanOrderApi.list,
				method: 'GET',
				data: requestData,
				header: this.authHeader()
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						const orders = this.getList((res && res.data) || {}).map((order) => ({
							...order,
							orderId: order.orderId || order.id || null,
							items: order.items || order.orderItems || [],
							typeLabel: '点单订单'
						}))
						this.orderList = [...this.orderList, ...orders]
						this.hasMore = orders.length >= this.pageSize
						return
					}
					this.hasMore = false
					showError((res.data && res.data.msg) || '加载失败')
				})
				.catch(() => {
					showError('加载失败')
				})
				.finally(() => {
					this.loading = false
					uni.stopPullDownRefresh()
				})
		},

		getList(body) {
			if (Array.isArray(body)) return body
			if (body.data && Array.isArray(body.data)) return body.data
			if (body.rows && Array.isArray(body.rows)) return body.rows
			if (body.list && Array.isArray(body.list)) return body.list
			return []
		},

		loadMore() {
			if (this.hasMore && !this.loading) {
				this.pageNum += 1
				this.loadOrders()
			}
		},

		getStatusText(status) {
			if (this.orderType === 'scan') {
				return SCAN_ORDER_STATUS_META[status] ? SCAN_ORDER_STATUS_META[status].text : '未知'
			}
			return ORDER_STATUS_META[status] ? ORDER_STATUS_META[status].text : '未知'
		},

		getStatusColor(status) {
			if (this.orderType === 'scan') {
				return SCAN_ORDER_STATUS_META[status] ? SCAN_ORDER_STATUS_META[status].color : '#6E655E'
			}
			return ORDER_STATUS_META[status] ? ORDER_STATUS_META[status].color : '#6E655E'
		},

		getOrderActionKey(orderId, action) {
			return `${action}:${orderId}`
		},

		isOrderActionPending(orderId, action) {
			return this.pendingOrderActionKey === this.getOrderActionKey(orderId, action)
		},

		isOrderPending(orderId) {
			return !!orderId && this.pendingOrderActionKey.endsWith(`:${orderId}`)
		},

		refreshCurrentList() {
			this.resetList()
			this.loadOrders()
		},

		closePaySuccessDialog() {
			this.paySuccessVisible = false
			this.paidOrderId = null
		},

		goToPaidOrder() {
			const orderId = this.paidOrderId
			this.closePaySuccessDialog()
			if (!orderId) {
				return
			}
			uni.navigateTo({
				url: this.getDetailUrl({ orderId })
			})
		},

		async cancelOrder(orderId) {
			if (this.pendingOrderActionKey) {
				return
			}
			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要取消该订单吗？'
			})
			if (!confirmed) {
				return
			}
			this.pendingOrderActionKey = this.getOrderActionKey(orderId, 'cancel')
			try {
				const result = await requestPromise({
					url: (this.orderType === 'scan' ? scanOrderApi.cancel : orderApi.cancel) + orderId,
					method: 'PUT',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('订单已取消')
					this.refreshCurrentList()
					return
				}

				showError((result.data && result.data.msg) || '取消失败')
			} catch (error) {
				showError('取消失败')
			} finally {
				this.pendingOrderActionKey = ''
			}
		},

		async confirmOrder(orderId) {
			if (this.pendingOrderActionKey) {
				return
			}
			const confirmed = await showConfirm({
				title: '提示',
				content: '确认已收到货物吗？'
			})
			if (!confirmed) {
				return
			}
			this.pendingOrderActionKey = this.getOrderActionKey(orderId, 'confirm')
			try {
				const result = await requestPromise({
					url: orderApi.confirm + orderId,
					method: 'PUT',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('确认收货成功')
					this.refreshCurrentList()
					return
				}

				showError((result.data && result.data.msg) || '操作失败')
			} catch (error) {
				showError('操作失败')
			} finally {
				this.pendingOrderActionKey = ''
			}
		},

		async urgeOrder(orderId) {
			if (this.pendingOrderActionKey) {
				return
			}
			this.pendingOrderActionKey = this.getOrderActionKey(orderId, 'urge')
			try {
				const result = await requestPromise({
					url: scanOrderApi.urge + orderId,
					method: 'POST',
					header: this.authHeader()
				})
				if (isSuccessResponse(result)) {
					showSuccess('已催单')
					this.refreshCurrentList()
					return
				}
				showError((result.data && result.data.msg) || '催单失败')
			} catch (error) {
				showError('催单失败')
			} finally {
				this.pendingOrderActionKey = ''
			}
		},

		async payOrder(order) {
			if (this.pendingOrderActionKey) {
				return
			}
			const orderId = order && order.orderId
			const payType = this.orderType === 'scan' ? (order.payType || 'shouqianba') : ''
			const confirmed = await showConfirm({
				title: '确认支付',
				content: this.orderType === 'scan' && payType === 'balance'
					? '确认使用余额支付该点单订单吗？'
					: '确认继续支付该订单吗？'
			})
			if (!confirmed) {
				return
			}

			this.pendingOrderActionKey = this.getOrderActionKey(orderId, 'pay')
			try {
				const res = await requestPromise({
					url: this.orderType === 'scan' ? scanOrderApi.pay + orderId + '?payType=' + payType : orderApi.pay + orderId,
					method: 'PUT',
					header: this.authHeader()
				})
				if (isSuccessResponse(res)) {
					this.paidOrderId = orderId
					this.paySuccessVisible = true
					this.refreshCurrentList()
					return
				}

				showError((res.data && res.data.msg) || '支付失败')
			} catch (error) {
				showError('支付失败')
			} finally {
				this.pendingOrderActionKey = ''
			}
		},

		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/me/me'
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

.order-type-switch {
	margin: 0 $space-page 18rpx;
	padding: 6rpx;
	display: flex;
	gap: 6rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
	border: 2rpx solid $border-subtle;
	box-sizing: border-box;
}

.order-type-item {
	flex: 1;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 10rpx;
}

.order-type-item.active {
	background: $bg-card;
	box-shadow: 0 8rpx 20rpx rgba(32, 26, 23, 0.06);
}

.order-type-item text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-secondary;
}

.order-type-item.active text {
	color: $text-primary;
}

.tabs-scroll {
	padding: 0 $space-page 18rpx;
	white-space: nowrap;
	flex-shrink: 0;
}

.tabs {
	display: inline-flex;
	gap: 14rpx;
}

.tab-item {
	height: 68rpx;
	padding: 0 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: rgba(255, 255, 255, 0.92);
	border: 2rpx solid $border-subtle;
}

.tab-item.active {
	background: $accent-primary-soft;
	border-color: rgba(111, 78, 55, 0.12);
}

.tab-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.tab-item.active .tab-text {
	color: $text-primary;
	font-weight: 600;
}

.order-list {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 24rpx;
	box-sizing: border-box;
}

.order-card {
	@include card(24rpx);
	margin-bottom: 20rpx;
}

.order-card-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
	margin-bottom: 18rpx;
}

.order-title-group {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.order-no {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.order-type-tag {
	width: fit-content;
	padding: 4rpx 12rpx;
	border-radius: 999rpx;
	background: $accent-primary-soft;
	font-family: $font-family;
	font-size: 19rpx;
	font-weight: 700;
	color: $text-primary;
}

.order-time {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.order-status {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
}

.product-row {
	display: flex;
	gap: 18rpx;
}

.product-row + .product-row {
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 2rpx solid $border-light;
}

.product-image {
	width: 132rpx;
	height: 132rpx;
	flex-shrink: 0;
	border-radius: $radius-sm;
	background: $accent-surface;
}

.product-image-empty {
	background: $bg-muted;
}

.product-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.product-name {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 1.4;
	color: $text-primary;
}

.product-spec,
.product-quantity {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
}

.product-meta {
	margin-top: auto;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.product-price {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.order-footer {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
	height: 60rpx;
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 2rpx solid $border-light;
	box-sizing: content-box;
}

.summary-price-wrap {
	flex: 1;
	min-width: 0;
	display: flex;
	align-items: center;
	height: 60rpx;
}

.summary-price {
	display: block;
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 60rpx;
	color: $text-primary;
}

.summary-extra {
	display: flex;
	flex-direction: column;
	gap: 6rpx;
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 2rpx solid $border-light;
}

.summary-text {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-secondary;
	line-height: 1.5;
}

.order-actions {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	gap: 12rpx;
	height: 60rpx;
	flex-shrink: 0;
}

.action-btn {
	height: 60rpx;
	padding: 0 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	border: 2rpx solid $border-subtle;
	background: $bg-card;
}

.action-btn text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-secondary;
}

.action-btn-primary {
	background: $accent-primary;
	border-color: $accent-primary;
}

.action-btn-primary text {
	color: #FFFFFF;
}

.action-btn-disabled {
	@include disabled-state;
}

.action-btn-light {
	background: $accent-primary-soft;
	border-color: $accent-primary-soft;
}

.action-btn-light text {
	color: $text-primary;
}

.load-more,
.no-more {
	padding: 30rpx 0;
	text-align: center;
}

.load-more text,
.no-more text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.empty-order {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 140rpx 40rpx 0;
}

.empty-image {
	width: 280rpx;
	height: 280rpx;
}

.empty-title {
	margin-top: 20rpx;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.empty-text {
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
	text-align: center;
}
</style>
