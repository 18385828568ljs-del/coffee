<template>
	<view class="overlay-root" v-if="visible" :class="{ 'overlay-visible': animating }">
		<!-- 背景遮罩 -->
		<view class="overlay-mask" @tap="closeOverlay"></view>

		<!-- 居中的 3D 容器 -->
		<view class="overlay-container">
			<view class="overlay-flipper" :class="{ 'overlay-flipper-flipped': flipped }">
				
				<!-- 正面：放大的原卡片样式 -->
				<view class="overlay-face overlay-front">
					<view class="overlay-image-wrap">
						<image
							v-if="productImage"
							class="overlay-image"
							:src="productImage"
							mode="aspectFill"
						/>
						<view v-else class="overlay-image-empty">
							<text>{{ shortName }}</text>
						</view>
					</view>
					<view class="overlay-main">
						<view class="overlay-top">
							<view v-if="product.tag" class="overlay-tag">
								<text>{{ product.tag }}</text>
							</view>
							<text v-if="showMonthlySales" class="overlay-sales">月售 {{ product.monthSales }}</text>
						</view>
						<text class="overlay-title">{{ product.productName }}</text>
						<text class="overlay-subtitle">{{ product.subTitle || product.flavorNotes || '点击查看商品详情' }}</text>
						<view class="overlay-footer">
							<view class="overlay-price-wrap">
								<text class="overlay-price-symbol">¥</text>
								<text class="overlay-price">{{ displayPrice }}</text>
							</view>
						</view>
					</view>
				</view>

				<!-- 背面：详情与交互 -->
				<view class="overlay-face overlay-back">
					<!-- 关闭按钮 -->
					<view class="overlay-close-btn" @tap.stop="closeOverlay">
						<text>×</text>
					</view>
					
					<scroll-view class="overlay-back-scroll" scroll-y>
						<view class="back-content">
							<view class="back-head">
								<text class="back-title">{{ product.productName }}</text>
								<text class="back-subtitle">{{ product.subTitle || product.flavorNotes || '精品咖啡商品详情' }}</text>
							</view>

							<!-- 加载中 -->
							<view v-if="loading" class="back-loading">
								<text>加载详情中...</text>
							</view>

							<!-- 规格选择 -->
							<view v-if="detailProduct && hasSpecs" class="back-section">
								<view class="section-head">
									<text class="section-title">规格选择</text>
								</view>
								<view v-for="spec in detailProduct.specs" :key="spec.specId" class="spec-block">
									<view class="spec-head">
										<text class="spec-title">{{ spec.specName }}</text>
										<text class="spec-rule">{{ specRuleText(spec) }}</text>
									</view>
									<view class="spec-options">
										<view
											v-for="option in spec.options"
											:key="option.optionId"
											class="spec-chip"
											:class="{ 'spec-chip-active': isOptionSelected(spec, option) }"
											@tap="toggleOption(spec, option)"
										>
											<text class="spec-chip-name">{{ option.optionName }}</text>
											<text v-if="toNumber(option.extraPrice) > 0" class="spec-chip-price">
												+¥{{ formatMoney(option.extraPrice) }}
											</text>
										</view>
									</view>
								</view>
							</view>

							<!-- 数量 -->
							<view v-if="detailProduct" class="back-section">
								<view class="info-row">
									<text class="info-label">数量</text>
									<view class="quantity-stepper">
										<view class="quantity-btn" :class="{ 'quantity-btn-disabled': quantity <= 1 }" @tap="decreaseQuantity">
											<text>-</text>
										</view>
										<text class="quantity-value">{{ quantity }}</text>
										<view class="quantity-btn" @tap="increaseQuantity">
											<text>+</text>
										</view>
									</view>
								</view>
								<view v-if="selectedSpecText" class="selected-spec-box">
									<text class="selected-spec-label">已选：{{ selectedSpecText }}</text>
								</view>
							</view>
							
							<view v-if="detailProduct && detailDescription" class="back-section">
								<text class="description-text">{{ detailDescription }}</text>
							</view>
							
							<!-- 留白用于底部按钮 -->
							<view class="safe-bottom-padding"></view>
						</view>
					</scroll-view>

					<!-- 底部操作栏 -->
					<view class="back-bottom-bar">
						<view class="bottom-total">
							<text class="bottom-total-symbol">¥</text>
							<text class="bottom-total-value">{{ formatMoney(totalPrice) }}</text>
						</view>
						<view class="bottom-actions">
							<view class="bottom-btn bottom-btn-secondary" :class="{ 'bottom-btn-disabled': submitting || !detailProduct }" @tap="addToCart">
								<text>{{ submitting ? '加入中...' : '加入购物车' }}</text>
							</view>
							<view class="bottom-btn" :class="{ 'bottom-btn-disabled': submitting || !detailProduct }" @tap="buyNow">
								<text>{{ submitting ? '处理中...' : '立刻购买' }}</text>
							</view>
						</view>
					</view>
				</view>

			</view>
		</view>
	</view>
</template>

<script>
import { scanMenuApi, scanCartApi, resolveImageUrl } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'
import { showError, showSuccess } from '@/utils/ui-feedback.js'

function toNumber(value) {
	const numberValue = Number(value || 0)
	return Number.isFinite(numberValue) ? numberValue : 0
}

export default {
	name: 'ScanProductOverlay',
	props: {
		shopId: {
			type: [Number, String],
			default: 1
		},
		tableNo: {
			type: String,
			default: ''
		}
	},
	data() {
		return {
			visible: false,
			animating: false,
			flipped: false,
			product: null,
			
			// Detail data
			detailProduct: null,
			loading: false,
			submitting: false,
			quantity: 1,
			singleSelections: {},
			multiSelections: {}
		}
	},
	computed: {
		productImage() {
			return resolveImageUrl(this.product && (this.product.imageUrl || this.product.productImage || ''))
		},
		shortName() {
			return String((this.product && this.product.productName) || '咖啡').slice(0, 2)
		},
		showMonthlySales() {
			return !!(this.product && this.product.monthSales !== undefined && this.product.monthSales !== null && this.product.monthSales !== '')
		},
		displayPrice() {
			const amount = Number((this.product && this.product.price) || 0)
			return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
		},
		
		// Detail Computed
		hasSpecs() {
			return !!(this.detailProduct && Array.isArray(this.detailProduct.specs) && this.detailProduct.specs.length)
		},
		selectedOptions() {
			if (!this.detailProduct || !Array.isArray(this.detailProduct.specs)) return []
			const result = []
			this.detailProduct.specs.forEach((spec) => {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (this.isMultipleSpec(spec)) {
					const selectedIds = this.multiSelections[spec.specId] || []
					options.forEach((option) => {
						if (selectedIds.indexOf(option.optionId) !== -1) {
							result.push({ spec, option })
						}
					})
					return
				}
				const selectedId = this.singleSelections[spec.specId]
				options.forEach((option) => {
					if (option.optionId === selectedId) {
						result.push({ spec, option })
					}
				})
			})
			return result
		},
		selectedSpecText() {
			return this.selectedOptions.map((entry) => entry.option.optionName).join(' / ')
		},
		detailDescription() {
			if (!this.detailProduct) return ''
			return this.detailProduct.description || this.detailProduct.remark || this.detailProduct.flavorNotes || ''
		},
		currentUnitPrice() {
			const basePrice = this.detailProduct ? toNumber(this.detailProduct.price) : (this.product ? toNumber(this.product.price) : 0)
			const extraPrice = this.selectedOptions.reduce((sum, entry) => sum + toNumber(entry.option.extraPrice), 0)
			return basePrice + extraPrice
		},
		totalPrice() {
			return this.currentUnitPrice * this.quantity
		}
	},
	methods: {
		toNumber,
		formatMoney(value) {
			return toNumber(value).toFixed(2)
		},
		authHeader() {
			const token = getToken()
			const header = {}
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},
		
		open(basicProduct) {
			if (!basicProduct) return
			this.product = basicProduct
			this.visible = true
			this.detailProduct = null
			this.loading = false
			this.submitting = false
			this.quantity = 1
			this.singleSelections = {}
			this.multiSelections = {}
			
			// 触发入场动画
			setTimeout(() => {
				this.animating = true
				// 稍微延迟后开始翻转，增加层次感
				setTimeout(() => {
					this.flipped = true
					this.loadProductDetail(basicProduct.productId)
				}, 150)
			}, 50)
		},
		
		closeOverlay() {
			this.flipped = false
			setTimeout(() => {
				this.animating = false
				setTimeout(() => {
					this.visible = false
					this.product = null
					this.detailProduct = null
				}, 300) // 等待透明度缩小动画结束
			}, 150) // 等待翻转回正
		},

		async loadProductDetail(productId) {
			this.loading = true
			try {
				const res = await requestPromise({
					url: scanMenuApi.productDetail + productId,
					method: 'GET',
					header: this.authHeader()
				})
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '商品详情加载失败')
					return
				}
				this.detailProduct = (res.data && res.data.data) || null
				this.initSpecSelections()
			} catch (error) {
				showError('商品详情加载失败')
			} finally {
				this.loading = false
			}
		},

		initSpecSelections() {
			const singleSelections = {}
			const multiSelections = {}
			const specs = (this.detailProduct && this.detailProduct.specs) || []
			specs.forEach((spec) => {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (this.isMultipleSpec(spec)) {
					multiSelections[spec.specId] = options
						.filter((option) => Number(option.isDefault) === 1)
						.map((option) => option.optionId)
					return
				}
				const defaultOption = options.find((option) => Number(option.isDefault) === 1) || options[0] || null
				singleSelections[spec.specId] = defaultOption ? defaultOption.optionId : null
			})
			this.singleSelections = singleSelections
			this.multiSelections = multiSelections
		},

		isMultipleSpec(spec) {
			return String((spec && spec.specType) || 'single') === 'multiple'
		},

		specRuleText(spec) {
			if (!spec) return ''
			const requiredText = Number(spec.required) === 1 ? '必选' : '可选'
			return this.isMultipleSpec(spec) ? requiredText + ' · 多选' : requiredText + ' · 单选'
		},

		isOptionSelected(spec, option) {
			if (!spec || !option) return false
			if (this.isMultipleSpec(spec)) {
				const selectedIds = this.multiSelections[spec.specId] || []
				return selectedIds.indexOf(option.optionId) !== -1
			}
			return this.singleSelections[spec.specId] === option.optionId
		},

		toggleOption(spec, option) {
			if (!spec || !option) return
			if (this.isMultipleSpec(spec)) {
				const selectedIds = (this.multiSelections[spec.specId] || []).slice()
				const index = selectedIds.indexOf(option.optionId)
				if (index === -1) {
					selectedIds.push(option.optionId)
				} else {
					selectedIds.splice(index, 1)
				}
				this.$set(this.multiSelections, spec.specId, selectedIds)
				return
			}
			this.$set(this.singleSelections, spec.specId, option.optionId)
		},

		decreaseQuantity() {
			if (this.quantity <= 1) return
			this.quantity -= 1
		},

		increaseQuantity() {
			this.quantity += 1
		},

		buildSpecJson() {
			const specs = (this.detailProduct && this.detailProduct.specs) || []
			const result = specs.map((spec) => {
				const selectedEntries = this.selectedOptions.filter((entry) => entry.spec.specId === spec.specId)
				return {
					specId: spec.specId,
					specName: spec.specName,
					specType: spec.specType,
					optionIds: selectedEntries.map((entry) => entry.option.optionId),
					optionNames: selectedEntries.map((entry) => entry.option.optionName)
				}
			}).filter((item) => item.optionIds.length > 0)
			return JSON.stringify(result)
		},

		buildCartPayload() {
			return {
				shopId: Number(this.shopId) || 1,
				tableNo: this.tableNo || '',
				productId: this.detailProduct.productId,
				productName: this.detailProduct.productName,
				productImage: this.detailProduct.imageUrl || this.detailProduct.productImage || '',
				price: this.currentUnitPrice,
				quantity: this.quantity,
				specText: this.selectedSpecText || this.detailProduct.remark || this.detailProduct.description || '',
				specJson: this.buildSpecJson(),
				selected: 1,
				status: 1
			}
		},

		async submitCart() {
			const res = await requestPromise({
				url: scanCartApi.add,
				method: 'POST',
				header: Object.assign({ 'Content-Type': 'application/json' }, this.authHeader()),
				data: this.buildCartPayload()
			})
			if (!isSuccessResponse(res)) {
				showError((res && res.data && res.data.msg) || '加入购物车失败')
				return false
			}
			this.$emit('added')
			return true
		},

		async addToCart() {
			if (!this.detailProduct || this.submitting) return
			if (!ensureLocalLogin('请先登录后再加入购物车')) return
			this.submitting = true
			try {
				const success = await this.submitCart()
				if (!success) {
					return
				}
				showSuccess('已加入购物车')
				this.closeOverlay()
			} catch (error) {
				showError('加入购物车失败')
			} finally {
				this.submitting = false
			}
		},

		async buyNow() {
			if (!this.detailProduct || this.submitting) return
			if (!ensureLocalLogin('请先登录后再购买')) return
			this.submitting = true
			try {
				const success = await this.submitCart()
				if (!success) {
					return
				}
				this.$emit('checkout')
				this.closeOverlay()
			} catch (error) {
				showError('购买失败，请稍后重试')
			} finally {
				this.submitting = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.overlay-root {
	position: fixed;
	inset: 0;
	z-index: 999;
	display: flex;
	align-items: center;
	justify-content: center;
}

.overlay-mask {
	position: absolute;
	inset: 0;
	background: rgba(32, 26, 23, 0.42);
	backdrop-filter: blur(10px);
	-webkit-backdrop-filter: blur(10px);
	opacity: 0;
	transition: opacity 0.3s ease;
}

.overlay-container {
	position: relative;
	width: 640rpx;
	height: 900rpx;
	perspective: 2000rpx;
	opacity: 0;
	transform: scale(0.8) translateY(40rpx);
	transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
	z-index: 1;
}

.overlay-visible .overlay-mask {
	opacity: 1;
}

.overlay-visible .overlay-container {
	opacity: 1;
	transform: scale(1) translateY(0);
}

.overlay-flipper {
	position: relative;
	width: 100%;
	height: 100%;
	transform-style: preserve-3d;
	transition: transform 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.overlay-flipper-flipped {
	transform: rotateY(180deg);
}

.overlay-face {
	position: absolute;
	inset: 0;
	backface-visibility: hidden;
	-webkit-backface-visibility: hidden;
	border-radius: $radius-md;
	background: $bg-card;
	border: 2rpx solid rgba(232, 224, 215, 0.92);
	box-shadow: 0 24rpx 60rpx rgba(32, 26, 23, 0.16);
	overflow: hidden;
	display: flex;
	flex-direction: column;
}

/* 正面样式：尽量和列表卡片保持一致但放大 */
.overlay-front {
	padding: 30rpx;
	display: flex;
	flex-direction: row;
	align-items: stretch;
}

.overlay-image-wrap {
	width: 240rpx;
	height: 100%;
	border-radius: 26rpx;
	overflow: hidden;
	flex-shrink: 0;
	background: $accent-surface;
}

.overlay-image, .overlay-image-empty {
	width: 100%;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.overlay-image-empty text {
	font-size: 64rpx;
	font-weight: 700;
	color: rgba(111, 78, 55, 0.72);
}

.overlay-main {
	flex: 1;
	min-width: 0;
	margin-left: 24rpx;
	display: flex;
	flex-direction: column;
}

.overlay-top {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.overlay-tag {
	padding: 4rpx 12rpx;
	border-radius: 999rpx;
	background: $accent-surface;
	border: 1rpx solid rgba(111, 78, 55, 0.1);
	font-family: $font-family;
	font-size: 20rpx;
	font-weight: 600;
	line-height: 1.2;
	color: $text-primary;
}

.overlay-sales {
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-tertiary;
}

.overlay-title {
	margin-top: 24rpx;
	font-family: $font-family;
	font-size: 40rpx;
	font-weight: 700;
	color: $text-primary;
}

.overlay-subtitle {
	margin-top: 12rpx;
	font-family: $font-family;
	font-size: 26rpx;
	color: $text-secondary;
}

.overlay-footer {
	margin-top: auto;
	display: flex;
	align-items: flex-end;
}

.overlay-price-symbol {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
	margin-right: 4rpx;
}

.overlay-price {
	font-family: $font-family;
	font-size: 52rpx;
	font-weight: 700;
	color: $text-primary;
}


/* 背面样式 */
.overlay-back {
	transform: rotateY(180deg);
	background: $bg-muted;
}

.overlay-close-btn {
	position: absolute;
	top: 24rpx;
	right: 24rpx;
	width: 60rpx;
	height: 60rpx;
	border-radius: $radius-sm;
	background: $bg-card;
	border: 2rpx solid $border-subtle;
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 10;
}

.overlay-close-btn text {
	font-size: 40rpx;
	color: $text-secondary;
	line-height: 1;
	margin-top: -4rpx;
}

.overlay-back-scroll {
	flex: 1;
	height: 0; /* 让 scroll-view 生效 */
}

.back-content {
	padding: 40rpx 32rpx;
}

.back-head {
	padding-right: 60rpx; /* 避开关闭按钮 */
	margin-bottom: 30rpx;
}

.back-title {
	display: block;
	font-family: $font-family;
	font-size: 44rpx;
	font-weight: 700;
	color: $text-primary;
}

.back-subtitle {
	display: block;
	margin-top: 10rpx;
	font-family: $font-family;
	font-size: 26rpx;
	color: $text-secondary;
}

.back-loading {
	padding: 60rpx 0;
	text-align: center;
	color: $text-tertiary;
	font-size: 26rpx;
}

.back-section {
	margin-top: 36rpx;
	@include card(24rpx, $radius-sm);
}

.section-head {
	margin-bottom: 20rpx;
}

.section-title {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.spec-block + .spec-block {
	margin-top: 28rpx;
}

.spec-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 16rpx;
}

.spec-title {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.spec-rule {
	font-family: $font-family;
	font-size: 22rpx;
	color: $text-tertiary;
}

.spec-options {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.spec-chip {
	padding: 14rpx 24rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
	border: 2rpx solid rgba(111, 78, 55, 0.08);
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
}

.spec-chip-active {
	background: $accent-primary-soft;
	border-color: rgba(111, 78, 55, 0.14);
}

.spec-chip-name {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.spec-chip-price {
	margin-top: 4rpx;
	font-family: $font-family;
	font-size: 20rpx;
	color: $text-secondary;
}

.info-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.info-label {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

.quantity-stepper {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.quantity-btn {
	width: 56rpx;
	height: 56rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
	border: 2rpx solid $border-subtle;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
}

.quantity-btn text {
	font-family: $font-family;
	font-size: 32rpx;
	font-weight: 600;
	color: $text-primary;
	margin-top: -4rpx;
}

.quantity-btn-disabled {
	opacity: 0.4;
}

.quantity-value {
	min-width: 40rpx;
	text-align: center;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
}

.selected-spec-box {
	margin-top: 20rpx;
	padding: 16rpx;
	border-radius: $radius-sm;
	background: $bg-muted;
}

.selected-spec-label {
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-secondary;
}

.description-text {
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-secondary;
	line-height: 1.6;
}

.safe-bottom-padding {
	height: 40rpx;
}

.back-bottom-bar {
	padding: 24rpx 32rpx;
	background: $bg-bottom;
	border-top: 2rpx solid rgba(232, 224, 215, 0.84);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18rpx;
}

.bottom-total {
	display: flex;
	align-items: flex-end;
}

.bottom-total-symbol {
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
	margin-right: 4rpx;
	margin-bottom: 4rpx;
}

.bottom-total-value {
	font-family: $font-family;
	font-size: 48rpx;
	font-weight: 700;
	color: $text-primary;
	line-height: 1;
}

.bottom-actions {
	display: flex;
	align-items: center;
	gap: 14rpx;
	flex-shrink: 0;
}

.bottom-btn {
	height: 80rpx;
	padding: 0 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	min-width: 166rpx;
	box-sizing: border-box;
	@include active-press;
}

.bottom-btn-secondary {
	background: $bg-card;
	border: 2rpx solid rgba(111, 78, 55, 0.18);
}

.bottom-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #FFFFFF;
}

.bottom-btn-secondary text {
	color: $accent-primary;
}

.bottom-btn-disabled {
	opacity: 0.5;
}
</style>
