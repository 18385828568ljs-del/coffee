<template>
	<view v-if="visible" class="sheet-root" :class="{ 'sheet-visible': animating }">
		<view class="sheet-mask" @tap="close"></view>

		<view class="sheet-panel">
			<view class="sheet-head">
				<view class="sheet-head-copy">
					<text class="sheet-title">{{ headTitle }}</text>
					<text v-if="headSubtitle" class="sheet-subtitle">{{ headSubtitle }}</text>
				</view>
				<view
					v-if="hasVideo"
					class="head-video-btn"
					hover-class="head-video-btn-press"
					@tap="onPlayVideo"
				>
					<view class="head-video-triangle"></view>
					<text class="head-video-text">讲解视频</text>
				</view>
				<view class="sheet-close" @tap="close">
					<text>×</text>
				</view>
			</view>

			<scroll-view class="sheet-body" scroll-y>
				<view v-if="loading" class="sheet-loading">
					<text>加载详情中…</text>
				</view>

				<view v-if="hasSpecs" class="sheet-section">
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

				<view v-else-if="!loading && detailProduct" class="sheet-section sheet-fallback">
					<view class="spec-block">
						<view class="spec-head">
							<text class="spec-title">温度</text>
							<text class="spec-rule">必选 · 单选</text>
						</view>
						<view class="spec-options">
							<view
								v-for="option in fallbackTemps"
								:key="'t-' + option.value"
								class="spec-chip"
								:class="{ 'spec-chip-active': fallback.temperature === option.value }"
								@tap="fallback.temperature = option.value"
							>
								<text class="spec-chip-name">{{ option.label }}</text>
							</view>
						</view>
					</view>
					<view class="spec-block">
						<view class="spec-head">
							<text class="spec-title">糖度</text>
							<text class="spec-rule">必选 · 单选</text>
						</view>
						<view class="spec-options">
							<view
								v-for="option in fallbackSugars"
								:key="'s-' + option.value"
								class="spec-chip"
								:class="{ 'spec-chip-active': fallback.sugar === option.value }"
								@tap="fallback.sugar = option.value"
							>
								<text class="spec-chip-name">{{ option.label }}</text>
							</view>
						</view>
					</view>
					<view class="spec-block">
						<view class="spec-head">
							<text class="spec-title">杯型</text>
							<text class="spec-rule">必选 · 单选</text>
						</view>
						<view class="spec-options">
							<view
								v-for="option in fallbackCups"
								:key="'c-' + option.value"
								class="spec-chip"
								:class="{ 'spec-chip-active': fallback.cup === option.value }"
								@tap="fallback.cup = option.value"
							>
								<text class="spec-chip-name">{{ option.label }}</text>
							</view>
						</view>
					</view>
				</view>

				<view v-if="detailProduct" class="sheet-section">
					<view class="info-row">
						<text class="info-label">数量</text>
						<view class="quantity-stepper">
							<view
								class="quantity-btn"
								:class="{ 'quantity-btn-disabled': quantity <= 1 }"
								@tap="decreaseQuantity"
							><text>-</text></view>
							<text class="quantity-value">{{ quantity }}</text>
							<view class="quantity-btn" @tap="increaseQuantity"><text>+</text></view>
						</view>
					</view>
					<view v-if="combinedSelectedText" class="selected-spec-box">
						<text class="selected-spec-label">已选：{{ combinedSelectedText }}</text>
					</view>
				</view>

				<view v-if="detailDescription" class="sheet-section">
					<text class="description-text">{{ detailDescription }}</text>
				</view>

				<view class="safe-bottom-padding"></view>
			</scroll-view>

			<view class="sheet-bottom">
				<view class="bottom-total">
					<text class="bottom-total-symbol">¥</text>
					<text class="bottom-total-value">{{ formatMoney(totalPrice) }}</text>
				</view>
				<view class="bottom-actions">
					<view
						class="bottom-btn bottom-btn-secondary"
						:class="{ 'bottom-btn-disabled': submitting || !detailProduct }"
						@tap="addToCart"
					>
						<text>{{ submitting ? '加入中…' : '加入购物车' }}</text>
					</view>
					<view
						class="bottom-btn"
						:class="{ 'bottom-btn-disabled': submitting || !detailProduct }"
						@tap="buyNow"
					>
						<text>{{ submitting ? '处理中…' : '立即购买' }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { scanMenuApi, scanCartApi } from '@/utils/apiconfig.js'
import { ensureLocalLogin } from '@/utils/session.js'
import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
import { getToken } from '@/utils/auth.js'
import { showError, showSuccess } from '@/utils/ui-feedback.js'
import { resolveProductVideo } from '@/utils/productVideoMap.js'

function toNumber(value) {
	const n = Number(value || 0)
	return Number.isFinite(n) ? n : 0
}

const FALLBACK_TEMPS = [
	{ label: '热', value: 'hot' },
	{ label: '温', value: 'warm' },
	{ label: '冰', value: 'iced' }
]
const FALLBACK_SUGARS = [
	{ label: '正常糖', value: 'normal' },
	{ label: '少糖', value: 'less' },
	{ label: '半糖', value: 'half' },
	{ label: '无糖', value: 'none' }
]
const FALLBACK_CUPS = [
	{ label: '中杯', value: 'medium' },
	{ label: '大杯', value: 'large' }
]

export default {
	name: 'ProductSpecSheet',
	props: {
		shopId: { type: [Number, String], default: 1 },
		tableNo: { type: String, default: '' }
	},
	data() {
		return {
			visible: false,
			animating: false,
			product: null,
			detailProduct: null,
			loading: false,
			submitting: false,
			quantity: 1,
			singleSelections: {},
			multiSelections: {},
			fallback: {
				temperature: 'hot',
				sugar: 'normal',
				cup: 'medium'
			},
			fallbackTemps: FALLBACK_TEMPS,
			fallbackSugars: FALLBACK_SUGARS,
			fallbackCups: FALLBACK_CUPS
		}
	},
	computed: {
		headTitle() {
			return (this.detailProduct && this.detailProduct.productName)
				|| (this.product && this.product.productName)
				|| '商品详情'
		},
		headSubtitle() {
			const source = this.detailProduct || this.product || {}
			return source.subTitle || source.flavorNotes || source.description || source.remark || ''
		},
		hasVideo() {
			return !!resolveProductVideo(this.detailProduct || this.product)
		},
		hasSpecs() {
			return !!(this.detailProduct && Array.isArray(this.detailProduct.specs) && this.detailProduct.specs.length)
		},
		selectedOptions() {
			if (!this.hasSpecs) return []
			const result = []
			this.detailProduct.specs.forEach((spec) => {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (this.isMultipleSpec(spec)) {
					const ids = this.multiSelections[spec.specId] || []
					options.forEach((option) => {
						if (ids.indexOf(option.optionId) !== -1) {
							result.push({ spec, option })
						}
					})
					return
				}
				const id = this.singleSelections[spec.specId]
				options.forEach((option) => {
					if (option.optionId === id) {
						result.push({ spec, option })
					}
				})
			})
			return result
		},
		selectedSpecText() {
			return this.selectedOptions.map((entry) => entry.option.optionName).join(' / ')
		},
		fallbackSelectedText() {
			if (this.hasSpecs) return ''
			const t = FALLBACK_TEMPS.find((item) => item.value === this.fallback.temperature)
			const s = FALLBACK_SUGARS.find((item) => item.value === this.fallback.sugar)
			const c = FALLBACK_CUPS.find((item) => item.value === this.fallback.cup)
			return [t && t.label, s && s.label, c && c.label].filter(Boolean).join(' / ')
		},
		combinedSelectedText() {
			return this.selectedSpecText || this.fallbackSelectedText
		},
		detailDescription() {
			const source = this.detailProduct || this.product || {}
			return source.description || source.remark || source.flavorNotes || ''
		},
		currentUnitPrice() {
			const base = this.detailProduct ? toNumber(this.detailProduct.price) : (this.product ? toNumber(this.product.price) : 0)
			const extra = this.selectedOptions.reduce((sum, entry) => sum + toNumber(entry.option.extraPrice), 0)
			return base + extra
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
			this.detailProduct = null
			this.loading = false
			this.submitting = false
			this.quantity = 1
			this.singleSelections = {}
			this.multiSelections = {}
			this.fallback = { temperature: 'hot', sugar: 'normal', cup: 'medium' }
			this.visible = true
			setTimeout(() => {
				this.animating = true
			}, 30)
			this.loadProductDetail(basicProduct.productId)
		},
		close() {
			this.animating = false
			setTimeout(() => {
				this.visible = false
				this.product = null
				this.detailProduct = null
				this.$emit('close')
			}, 380)
		},
		onPlayVideo() {
			const url = resolveProductVideo(this.detailProduct || this.product)
			if (!url) {
				showError('该商品暂无讲解视频')
				return
			}
			this.$emit('play-video', {
				product: this.detailProduct || this.product,
				videoUrl: url
			})
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
					this.detailProduct = this.product ? Object.assign({}, this.product) : null
					return
				}
				this.detailProduct = (res.data && res.data.data) || (this.product ? Object.assign({}, this.product) : null)
				this.initSpecSelections()
			} catch (error) {
				this.detailProduct = this.product ? Object.assign({}, this.product) : null
			} finally {
				this.loading = false
			}
		},
		initSpecSelections() {
			const single = {}
			const multi = {}
			const specs = (this.detailProduct && this.detailProduct.specs) || []
			specs.forEach((spec) => {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (this.isMultipleSpec(spec)) {
					multi[spec.specId] = options
						.filter((option) => Number(option.isDefault) === 1)
						.map((option) => option.optionId)
					return
				}
				const def = options.find((option) => Number(option.isDefault) === 1) || options[0] || null
				single[spec.specId] = def ? def.optionId : null
			})
			this.singleSelections = single
			this.multiSelections = multi
		},
		isMultipleSpec(spec) {
			return String((spec && spec.specType) || 'single') === 'multiple'
		},
		specRuleText(spec) {
			const required = Number(spec && spec.required) === 1 ? '必选' : '可选'
			return this.isMultipleSpec(spec) ? required + ' · 多选' : required + ' · 单选'
		},
		isOptionSelected(spec, option) {
			if (!spec || !option) return false
			if (this.isMultipleSpec(spec)) {
				const ids = this.multiSelections[spec.specId] || []
				return ids.indexOf(option.optionId) !== -1
			}
			return this.singleSelections[spec.specId] === option.optionId
		},
		toggleOption(spec, option) {
			if (!spec || !option) return
			if (this.isMultipleSpec(spec)) {
				const ids = (this.multiSelections[spec.specId] || []).slice()
				const idx = ids.indexOf(option.optionId)
				if (idx === -1) ids.push(option.optionId)
				else ids.splice(idx, 1)
				this.$set(this.multiSelections, spec.specId, ids)
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
			if (this.hasSpecs) {
				const specs = this.detailProduct.specs || []
				const result = specs.map((spec) => {
					const entries = this.selectedOptions.filter((entry) => entry.spec.specId === spec.specId)
					return {
						specId: spec.specId,
						specName: spec.specName,
						specType: spec.specType,
						optionIds: entries.map((entry) => entry.option.optionId),
						optionNames: entries.map((entry) => entry.option.optionName)
					}
				}).filter((item) => item.optionIds.length > 0)
				return JSON.stringify(result)
			}
			return JSON.stringify([
				{ specName: '温度', value: this.fallback.temperature },
				{ specName: '糖度', value: this.fallback.sugar },
				{ specName: '杯型', value: this.fallback.cup }
			])
		},
		buildCartPayload() {
			const source = this.detailProduct || this.product || {}
			return {
				shopId: Number(this.shopId) || 1,
				tableNo: this.tableNo || '',
				productId: source.productId,
				productName: source.productName,
				productImage: source.imageUrl || source.productImage || '',
				price: this.currentUnitPrice,
				quantity: this.quantity,
				specText: this.combinedSelectedText || source.remark || source.description || '',
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
			const target = this.detailProduct || this.product
			if (!target || this.submitting) return
			if (!ensureLocalLogin('请先登录后再加入购物车')) return
			this.submitting = true
			try {
				const ok = await this.submitCart()
				if (!ok) return
				showSuccess('已加入购物车')
				this.close()
			} catch (error) {
				showError('加入购物车失败')
			} finally {
				this.submitting = false
			}
		},
		async buyNow() {
			const target = this.detailProduct || this.product
			if (!target || this.submitting) return
			if (!ensureLocalLogin('请先登录后再购买')) return
			this.submitting = true
			try {
				const ok = await this.submitCart()
				if (!ok) return
				this.$emit('checkout')
				this.close()
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

.sheet-root {
	position: fixed;
	inset: 0;
	z-index: 998;
}

.sheet-mask {
	position: absolute;
	inset: 0;
	background: rgba(32, 26, 23, 0.46);
	opacity: 0;
	transition: opacity 0.26s ease;
}

.sheet-panel {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	max-height: 84vh;
	display: flex;
	flex-direction: column;
	background: $bg-card;
	border-radius: 28rpx 28rpx 0 0;
	box-shadow: 0 -22rpx 56rpx rgba(32, 26, 23, 0.18);
	transform-origin: right center;
	transform: perspective(1200rpx) translateX(80rpx) rotateY(28deg) scale(0.94);
	opacity: 0;
	transition: transform 0.38s cubic-bezier(0.2, 0.8, 0.2, 1),
		opacity 0.32s ease;
	will-change: transform, opacity;
}

.sheet-visible .sheet-mask {
	opacity: 1;
}

.sheet-visible .sheet-panel {
	transform: perspective(1200rpx) translateX(0) rotateY(0deg) scale(1);
	opacity: 1;
}

.sheet-head {
	position: relative;
	padding: 32rpx 32rpx 16rpx;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.6);
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
}

.sheet-head-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.sheet-title {
	font-family: $font-family;
	font-size: 36rpx;
	font-weight: 700;
	color: $text-primary;
}

.sheet-subtitle {
	font-family: $font-family;
	font-size: 24rpx;
	color: $text-secondary;
	line-height: 1.5;
}

.sheet-close {
	width: 56rpx;
	height: 56rpx;
	flex-shrink: 0;
	border-radius: 50%;
	background: $bg-muted;
	border: 2rpx solid $border-subtle;
	display: flex;
	align-items: center;
	justify-content: center;
	@include active-press;
}

.sheet-close text {
	font-size: 38rpx;
	line-height: 1;
	color: $text-secondary;
	margin-top: -4rpx;
}

.sheet-body {
	flex: 1;
	min-height: 0;
	padding: 24rpx 32rpx 0;
}

.head-video-btn {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 10rpx 18rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, rgba(111, 78, 55, 0.94) 0%, rgba(154, 108, 69, 0.92) 100%);
	box-shadow: 0 8rpx 18rpx rgba(111, 78, 55, 0.24);
}

.head-video-btn-press {
	transform: scale(0.96);
	opacity: 0.92;
}

.head-video-triangle {
	width: 0;
	height: 0;
	border-left: 14rpx solid #ffffff;
	border-top: 9rpx solid transparent;
	border-bottom: 9rpx solid transparent;
	margin-left: 2rpx;
}

.head-video-text {
	font-family: $font-family;
	font-size: 22rpx;
	font-weight: 700;
	color: #ffffff;
}

.sheet-loading {
	padding: 40rpx 0;
	text-align: center;
	color: $text-tertiary;
	font-size: 26rpx;
}

.sheet-section {
	margin-top: 24rpx;
	@include card(24rpx, $radius-sm);
}

.sheet-fallback {
	padding-bottom: 8rpx;
}

.spec-block + .spec-block {
	margin-top: 24rpx;
}

.spec-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 14rpx;
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
	gap: 14rpx;
}

.spec-chip {
	padding: 12rpx 22rpx;
	border-radius: 999rpx;
	background: $bg-muted;
	border: 2rpx solid rgba(111, 78, 55, 0.08);
	display: flex;
	flex-direction: column;
	align-items: center;
	box-sizing: border-box;
	@include active-press;
}

.spec-chip-active {
	background: $accent-primary-soft;
	border-color: rgba(111, 78, 55, 0.32);
}

.spec-chip-name {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
}

.spec-chip-price {
	margin-top: 2rpx;
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
	border-radius: 50%;
	background: $bg-muted;
	border: 2rpx solid $border-subtle;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	@include active-press;
}

.quantity-btn text {
	font-size: 32rpx;
	font-weight: 600;
	color: $text-primary;
	margin-top: -4rpx;
}

.quantity-btn-disabled { opacity: 0.4; }

.quantity-value {
	min-width: 48rpx;
	text-align: center;
	font-family: $font-family;
	font-size: 30rpx;
	font-weight: 600;
}

.selected-spec-box {
	margin-top: 18rpx;
	padding: 14rpx 18rpx;
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

.sheet-bottom {
	padding: 20rpx 32rpx calc(env(safe-area-inset-bottom) + 24rpx);
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
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
	margin-right: 4rpx;
	margin-bottom: 4rpx;
}

.bottom-total-value {
	font-family: $font-family;
	font-size: 44rpx;
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
	padding: 0 26rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 999rpx;
	background: $accent-primary;
	min-width: 180rpx;
	box-sizing: border-box;
	@include active-press;
}

.bottom-btn-secondary {
	background: $bg-card;
	border: 2rpx solid rgba(111, 78, 55, 0.2);
}

.bottom-btn text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #ffffff;
}

.bottom-btn-secondary text {
	color: $accent-primary;
}

.bottom-btn-disabled { opacity: 0.5; }
</style>
