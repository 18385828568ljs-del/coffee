<template>
	<view v-if="visible" class="refund-overlay" @tap="handleClose">
		<view class="refund-sheet" @tap.stop>
			<view class="sheet-header">
				<view>
					<text class="sheet-title">申请退款</text>
					<text class="sheet-subtitle">退款将按原支付方式返回账户</text>
				</view>
				<view class="amount-pill">
					<text>¥{{ formattedAmount }}</text>
				</view>
			</view>

			<view class="reason-panel">
				<text class="section-title">退款原因</text>
				<radio-group class="reason-list" @change="handleReasonChange">
					<label
						v-for="item in reasons"
						:key="item"
						class="reason-item"
						:class="{ active: localReason === item }"
					>
						<radio :value="item" :checked="localReason === item" color="#7A4F2D" />
						<text>{{ item }}</text>
					</label>
				</radio-group>
			</view>

			<view class="sheet-actions">
				<view class="action-btn action-btn-ghost" @tap="handleClose">
					<text>取消</text>
				</view>
				<view class="action-btn action-btn-primary" :class="{ disabled: pending }" @tap="handleConfirm">
					<text>{{ pending ? '提交中...' : '确认退款' }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'RefundDialog',
	props: {
		visible: {
			type: Boolean,
			default: false
		},
		amount: {
			type: [Number, String],
			default: 0
		},
		reasons: {
			type: Array,
			default: () => ['不想要了', '下错单了', '等待时间太长', '其他原因']
		},
		selectedReason: {
			type: String,
			default: ''
		},
		pending: {
			type: Boolean,
			default: false
		}
	},
	data() {
		return {
			localReason: this.selectedReason || this.reasons[0] || ''
		}
	},
	computed: {
		formattedAmount() {
			return Number(this.amount || 0).toFixed(2)
		}
	},
	watch: {
		visible(value) {
			if (value) {
				this.localReason = this.selectedReason || this.reasons[0] || ''
			}
		},
		selectedReason(value) {
			this.localReason = value || this.reasons[0] || ''
		}
	},
	methods: {
		handleReasonChange(event) {
			this.localReason = event.detail.value
			this.$emit('reason-change', this.localReason)
		},
		handleClose() {
			if (this.pending) return
			this.$emit('close')
		},
		handleConfirm() {
			if (this.pending) return
			this.$emit('confirm', this.localReason)
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.refund-overlay {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 999;
	display: flex;
	align-items: flex-end;
	justify-content: center;
	background: rgba(32, 26, 23, 0.46);
	box-sizing: border-box;
}

.refund-sheet {
	width: 100%;
	padding: 30rpx $space-page calc(28rpx + env(safe-area-inset-bottom));
	border-radius: 28rpx 28rpx 0 0;
	background: $bg-card;
	box-shadow: 0 -16rpx 40rpx rgba(36, 24, 19, 0.16);
	box-sizing: border-box;
}

.sheet-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
	padding-bottom: 24rpx;
	border-bottom: 2rpx solid $border-light;
}

.sheet-title,
.section-title {
	display: block;
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 700;
	color: $text-primary;
}

.sheet-subtitle {
	display: block;
	margin-top: 8rpx;
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 500;
	color: $text-secondary;
}

.amount-pill {
	min-width: 164rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 18rpx;
	border-radius: $radius-sm;
	background: $accent-surface;
	box-sizing: border-box;
}

.amount-pill text {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 700;
	color: $accent-primary;
}

.reason-panel {
	padding: 26rpx 0 18rpx;
}

.section-title {
	font-size: 26rpx;
}

.reason-list {
	margin-top: 14rpx;
}

.reason-item {
	height: 88rpx;
	display: flex;
	align-items: center;
	gap: 18rpx;
	border-bottom: 2rpx solid $border-light;
	box-sizing: border-box;
}

.reason-item:last-child {
	border-bottom: 0;
}

.reason-item text {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 500;
	color: $text-primary;
}

.reason-item.active text {
	color: $accent-primary;
	font-weight: 700;
}

.sheet-actions {
	display: flex;
	gap: 16rpx;
	padding-top: 8rpx;
}

.action-btn {
	flex: 1;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	box-sizing: border-box;
}

.action-btn text {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
}

.action-btn-ghost {
	background: $bg-muted;
}

.action-btn-ghost text {
	color: $text-secondary;
}

.action-btn-primary {
	background: $accent-primary;
}

.action-btn-primary text {
	color: #FFFFFF;
}

.disabled {
	@include disabled-state;
}
</style>
