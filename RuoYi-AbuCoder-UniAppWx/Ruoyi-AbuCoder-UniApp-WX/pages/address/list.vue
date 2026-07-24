<template>
	<view class="page">
		<app-nav title="地址管理" fallback-url="/pages/me/me" />

		<scroll-view class="address-list" scroll-y>
			<view class="tip-card">
				<text class="tip-text">请选择常用地址作为默认地址，下单时可直接回填，减少重复填写。</text>
			</view>

			<view
				v-for="(address, index) in addressList"
				:key="address.addressId || index"
				class="address-item"
				:class="{ active: currentAddressId && String(currentAddressId) === String(address.addressId) }"
				@click="selectAddress(address)"
			>
				<view class="address-header">
					<view class="address-user">
						<text class="receiver-name">{{ address.receiverName }}</text>
						<text class="receiver-phone">{{ address.receiverPhone }}</text>
					</view>
					<text v-if="address.isDefault === 1" class="default-badge">默认地址</text>
				</view>
				<text class="address-detail">
					{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
				</text>
				<view class="address-actions">
					<text class="address-note">{{ address.isDefault === 1 ? '下单时优先使用' : '可在下单时切换使用' }}</text>
					<view class="action-group">
						<text
							v-if="address.isDefault !== 1"
							class="action-link"
							@click.stop="setDefaultAddress(address)"
						>
							设为默认
						</text>
						<text class="action-link" @click.stop="editAddress(address)">编辑</text>
						<text class="action-link danger" @click.stop="deleteAddress(address.addressId)">删除</text>
					</view>
				</view>
			</view>

			<view v-if="addressList.length === 0" class="empty-address">
				<text class="empty-title">暂无收货地址</text>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view class="bottom-bar">
			<view class="add-btn" @click="addAddress">
				<text>新增收货地址</text>
			</view>
		</view>
	</view>
</template>

<script>
import { addressApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin, getLocalUserId } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showConfirm, showError, showSuccess } from '@/utils/ui-feedback.js'

export default {
	data() {
		return {
			addressList: [],
			fromPage: '',
			currentAddressId: ''
		}
	},

	onLoad(options) {
		if (options.from) {
			this.fromPage = options.from
		}
		if (options.currentAddressId) {
			this.currentAddressId = options.currentAddressId
		}
		this.loadAddressList()
	},

	onShow() {
		this.loadAddressList()
	},

	methods: {
		loadAddressList() {
			const userId = getLocalUserId()
			if (!userId) {
				this.addressList = []
				return
			}

			showBusy('加载中...')

			requestPromise({
				url: addressApi.list,
				method: 'GET',
				data: {
					userId
				}
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						this.addressList = res.data.data || []
						return
					}
					showError((res.data && res.data.msg) || '加载失败')
				})
				.catch(() => {
					showError('加载失败')
				})
				.finally(() => {
					hideBusy()
				})
		},

		selectAddress(address) {
			if (this.fromPage === 'order') {
				uni.setStorageSync('selectedOrderAddress', address)
				uni.navigateBack()
			}
		},

		setDefaultAddress(address) {
			if (!ensureLocalLogin()) {
				return
			}
			if (!address || !address.addressId || address.isDefault === 1) {
				return
			}

			showBusy('设置中...')
			requestPromise({
				url: `${addressApi.setDefault + address.addressId}?userId=${getLocalUserId()}`,
				method: 'PUT'
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						showSuccess('已设为默认地址')
						this.loadAddressList()
						return
					}
					showError((res.data && res.data.msg) || '设置失败')
				})
				.catch(() => {
					showError('设置失败')
				})
				.finally(() => {
					hideBusy()
				})
		},

		addAddress() {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: '/pages/address/edit'
			})
		},

		editAddress(address) {
			if (!ensureLocalLogin()) {
				return
			}
			uni.navigateTo({
				url: `/pages/address/edit?id=${address.addressId}`
			})
		},

		async deleteAddress(addressId) {
			const confirmed = await showConfirm({
				title: '提示',
				content: '确定要删除该地址吗？'
			})
			if (!confirmed) {
				return
			}

			showBusy('删除中...')
			requestPromise({
				url: `${addressApi.delete + addressId}?userId=${getLocalUserId()}`,
				method: 'DELETE'
			})
				.then((deleteRes) => {
					if (isSuccessResponse(deleteRes)) {
						showSuccess('删除成功')
						this.loadAddressList()
						return
					}
					showError((deleteRes.data && deleteRes.data.msg) || '删除失败')
				})
				.catch(() => {
					showError('删除失败')
				})
				.finally(() => {
					hideBusy()
				})
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
	padding-bottom: calc(132rpx + env(safe-area-inset-bottom));
}

.address-list {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 24rpx;
	box-sizing: border-box;
}

.tip-card,
.address-item {
	@include card(24rpx);
	margin-bottom: 20rpx;
}

.tip-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.address-item.active {
	border-color: rgba(111, 78, 55, 0.18);
	background: $accent-surface;
}

.address-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 16rpx;
}

.address-user {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 14rpx;
}

.receiver-name {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.receiver-phone {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.default-badge {
	height: 40rpx;
	padding: 0 12rpx;
	display: flex;
	align-items: center;
	border-radius: $radius-sm;
	background: $accent-surface;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	color: $text-primary;
}

.address-detail {
	display: block;
	margin-top: 14rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 500;
	line-height: 1.6;
	color: $text-secondary;
}

.address-actions {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	margin-top: 18rpx;
}

.address-note {
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	color: $text-tertiary;
}

.action-group {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.action-link {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 600;
	color: $text-primary;
}

.action-link.danger {
	color: $accent-danger;
}

.empty-address {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 140rpx 40rpx 0;
}

.empty-title {
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

.bottom-space {
	height: 132rpx;
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 16rpx $space-page calc(24rpx + env(safe-area-inset-bottom));
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
	box-sizing: border-box;
}

.add-btn {
	min-height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.add-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}
</style>
