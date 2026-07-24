<template>
	<view class="page">
		<app-nav :title="addressId ? '编辑地址' : '新增地址'" fallback-url="/pages/me/me" />

		<scroll-view class="content" scroll-y>
			<view class="form-card">
				<view class="form-item">
					<text class="form-label">收货人</text>
					<input
						v-model="formData.receiverName"
						class="form-input"
						:class="{ 'form-input-error': errors.receiverName }"
						:focus="focusField === 'receiverName'"
						placeholder="请输入收货人姓名"
						placeholder-class="input-placeholder"
						@input="handleFieldInput('receiverName', $event)"
						@blur="validateField('receiverName')"
					/>
					<text v-if="errors.receiverName" class="field-error">{{ errors.receiverName }}</text>
				</view>

				<view class="form-item">
					<text class="form-label">手机号</text>
					<input
						v-model="formData.receiverPhone"
						class="form-input"
						:class="{ 'form-input-error': errors.receiverPhone }"
						:focus="focusField === 'receiverPhone'"
						type="number"
						maxlength="11"
						placeholder="请输入手机号"
						placeholder-class="input-placeholder"
						@input="handleFieldInput('receiverPhone', $event)"
						@blur="validateField('receiverPhone')"
					/>
					<text v-if="errors.receiverPhone" class="field-error">{{ errors.receiverPhone }}</text>
				</view>

				<view class="form-item">
					<text class="form-label">所在地区</text>
					<picker
						mode="region"
						:value="regionValue"
						@change="regionChange"
					>
						<view class="form-input form-input-select" :class="{ placeholder: !regionText, 'form-input-error': errors.region }">
							<text>{{ regionText || '请选择省市区' }}</text>
						</view>
					</picker>
					<text v-if="errors.region" class="field-error">{{ errors.region }}</text>
				</view>

				<view class="form-item form-item-textarea">
					<text class="form-label">详细地址</text>
					<textarea
						v-model="formData.detailAddress"
						class="form-textarea"
						:class="{ 'form-input-error': errors.detailAddress }"
						:focus="focusField === 'detailAddress'"
						placeholder="街道、楼牌号等"
						placeholder-class="input-placeholder"
						maxlength="200"
						@input="handleFieldInput('detailAddress', $event)"
						@blur="validateField('detailAddress')"
					/>
					<text v-if="errors.detailAddress" class="field-error">{{ errors.detailAddress }}</text>
				</view>

				<view class="form-item form-item-switch">
					<text class="form-label">设为默认地址</text>
					<switch
						:checked="formData.isDefault === 1"
						@change="switchChange"
						color="#6F4E37"
					/>
				</view>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<view class="bottom-bar">
			<view class="save-btn" :class="{ 'save-btn-disabled': submitting }" @click="saveAddress">
				<text>{{ submitting ? '保存中...' : '保存地址' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import { addressApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin, getLocalUserId } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { hideBusy, showBusy, showError, showSuccess } from '@/utils/ui-feedback.js'

export default {
	data() {
		return {
			addressId: null,
			submitting: false,
			focusField: '',
			formData: {
				receiverName: '',
				receiverPhone: '',
				province: '',
				city: '',
				district: '',
				detailAddress: '',
				isDefault: 0
			},
			regionValue: [],
			errors: {
				receiverName: '',
				receiverPhone: '',
				region: '',
				detailAddress: ''
			}
		}
	},

	computed: {
		regionText() {
			if (this.formData.province && this.formData.city && this.formData.district) {
				return `${this.formData.province} ${this.formData.city} ${this.formData.district}`
			}
			return ''
		}
	},

	onLoad(options) {
		if (!ensureLocalLogin()) {
			setTimeout(() => {
				uni.navigateBack()
			}, 300)
			return
		}
		if (options.id) {
			this.addressId = options.id
			this.loadAddressDetail()
		}
	},

	methods: {
		setFieldError(field, message = '') {
			this.$set(this.errors, field, message)
		},

		handleFieldInput(field, event) {
			const value = event && event.detail ? event.detail.value : event
			this.formData[field] = value
			this.setFieldError(field, '')
		},

		focusInvalidField(field) {
			if (!field || field === 'region') {
				return
			}
			this.focusField = field
			setTimeout(() => {
				if (this.focusField === field) {
					this.focusField = ''
				}
			}, 250)
		},

		validateField(field) {
			const value = this.formData[field]
			let message = ''

			if (field === 'receiverName' && !String(value || '').trim()) {
				message = '请输入收货人姓名'
			}
			if (field === 'receiverPhone') {
				if (!String(value || '').trim()) {
					message = '请输入手机号'
				} else if (!/^1[3-9]\d{9}$/.test(String(value || '').trim())) {
					message = '请输入正确的手机号'
				}
			}
			if (field === 'region' && (!this.formData.province || !this.formData.city || !this.formData.district)) {
				message = '请选择所在地区'
			}
			if (field === 'detailAddress' && !String(value || '').trim()) {
				message = '请输入详细地址'
			}

			this.setFieldError(field, message)
			return !message
		},

		validateForm() {
			const fields = ['receiverName', 'receiverPhone', 'region', 'detailAddress']
			const firstInvalidField = fields.find((field) => !this.validateField(field))
			if (!firstInvalidField) {
				return true
			}
			this.focusInvalidField(firstInvalidField)
			showError(this.errors[firstInvalidField] || '请核对表单信息')
			return false
		},

		async loadAddressDetail() {
			showBusy('加载中...')

			try {
				const res = await requestPromise({
					url: addressApi.detail + this.addressId,
					method: 'GET',
					data: {
						userId: getLocalUserId()
					}
				})
				if (isSuccessResponse(res) && res.data.data) {
					this.formData = res.data.data
					this.regionValue = [
						this.formData.province,
						this.formData.city,
						this.formData.district
					]
					return
				}
				showError((res.data && res.data.msg) || '加载失败')
			} catch (error) {
				showError('加载失败')
			} finally {
				hideBusy()
			}
		},

		regionChange(e) {
			const region = e.detail.value
			this.formData.province = region[0]
			this.formData.city = region[1]
			this.formData.district = region[2]
			this.regionValue = region
			this.setFieldError('region', '')
		},

		switchChange(e) {
			this.formData.isDefault = e.detail.value ? 1 : 0
		},

		saveAddress() {
			if (this.submitting || !this.validateForm()) {
				return
			}

			this.submitting = true
			showBusy('保存中...')

			const url = this.addressId ? addressApi.update : addressApi.add
			const method = this.addressId ? 'PUT' : 'POST'

			requestPromise({
				url,
				method,
				data: {
					...this.formData,
					userId: getLocalUserId(),
					addressId: this.addressId
				}
			})
				.then((res) => {
					if (isSuccessResponse(res)) {
						showSuccess('保存成功')
						setTimeout(() => {
							uni.navigateBack()
						}, 500)
						return
					}
					showError((res.data && res.data.msg) || '保存失败')
				})
				.catch(() => {
					showError('保存失败')
				})
				.finally(() => {
					this.submitting = false
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

.content {
	flex: 1;
	min-height: 0;
	padding: 0 $space-page 24rpx;
	box-sizing: border-box;
}

.form-card {
	@include card(24rpx);
}

.form-item {
	padding: 18rpx 0;
}

.form-item + .form-item {
	border-top: 2rpx solid $border-light;
}

.form-label {
	display: block;
	margin-bottom: 12rpx;
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	line-height: 1.5;
	color: $text-primary;
}

.form-input,
.form-input-select,
.form-textarea {
	width: 100%;
	padding: 18rpx 16rpx;
	box-sizing: border-box;
	border-radius: $radius-sm;
	background: $bg-muted;
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 500;
	color: $text-primary;
}

.form-input-error {
	border: 2rpx solid rgba(200, 110, 96, 0.45);
	background: rgba(255, 247, 245, 0.96);
}

.form-input {
	height: 84rpx;
}

.form-input-select {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 84rpx;
	line-height: 1.5;
}

.form-input-select .iconfont {
	font-size: 20rpx;
	color: $text-tertiary;
}

.form-input.placeholder {
	color: $text-tertiary;
}

.form-textarea {
	min-height: 200rpx;
	line-height: 1.6;
	padding: 18rpx 16rpx;
}

.form-item-textarea {
	padding-bottom: 24rpx;
}

.form-item-switch {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.form-item-switch .form-label {
	margin-bottom: 0;
}

.input-placeholder {
	color: $text-tertiary;
	font-size: 28rpx;
}

.field-error {
	display: block;
	margin-top: 10rpx;
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 500;
	line-height: 1.4;
	color: $accent-danger;
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

.save-btn {
	min-height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	@include active-press;
}

.save-btn-disabled {
	@include disabled-state;
}

.save-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}
</style>
