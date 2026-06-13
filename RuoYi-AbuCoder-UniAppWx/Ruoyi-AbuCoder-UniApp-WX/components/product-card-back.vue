<template>
	<view class="card-back-shell" @tap.stop>
		<view class="back-head">
			<view v-if="productImage" class="back-thumb-wrap">
				<image class="back-thumb-image" :src="productImage" mode="aspectFill" />
			</view>
			<view class="back-head-copy">
				<text class="back-title">{{ headTitle }}</text>
				<text v-if="headSubtitle" class="back-subtitle">{{ headSubtitle }}</text>
			</view>
			<view
				v-if="hasVideo"
				class="head-video-btn"
				hover-class="head-video-btn-press"
				@tap.stop="onPlayVideo"
			>
				<view class="head-video-triangle"></view>
				<text class="head-video-text">讲解视频</text>
			</view>
			<view class="back-close" hover-class="back-close-press" @tap.stop="onClose">
				<text class="back-close-text">×</text>
			</view>
		</view>

		<scroll-view class="back-body" scroll-y>
			<view v-if="loading" class="back-loading">
				<text>加载详情中...</text>
			</view>

			<view v-if="hasSpecs" class="back-section">
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
							@tap.stop="toggleOption(spec, option)"
						>
							<text class="spec-chip-name">{{ option.optionName }}</text>
							<text v-if="toNumber(option.extraPrice) > 0" class="spec-chip-price">
								+¥{{ formatMoney(option.extraPrice) }}
							</text>
						</view>
					</view>
				</view>
			</view>

			<view v-else-if="!loading && detailProduct" class="back-section">
				<view class="spec-block">
					<view class="spec-head">
						<text class="spec-title">温度</text>
						<text class="spec-rule">必选 · 单选</text>
					</view>
					<view class="spec-options">
						<view
							v-for="option in fallbackTemps"
							:key="option.key"
							class="spec-chip"
							:class="{ 'spec-chip-active': fallback.temperature === option.value }"
							@tap.stop="selectFallback('temperature', option.value)"
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
							:key="option.key"
							class="spec-chip"
							:class="{ 'spec-chip-active': fallback.sugar === option.value }"
							@tap.stop="selectFallback('sugar', option.value)"
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
							:key="option.key"
							class="spec-chip"
							:class="{ 'spec-chip-active': fallback.cup === option.value }"
							@tap.stop="selectFallback('cup', option.value)"
						>
							<text class="spec-chip-name">{{ option.label }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="combinedSelectedText" class="selected-spec-box">
				<text class="selected-spec-label">已选：{{ combinedSelectedText }}</text>
			</view>

			<view v-if="detailDescription" class="back-section">
				<text class="description-text">{{ detailDescription }}</text>
			</view>

			<view class="back-bottom-padding"></view>
		</scroll-view>

		<view class="back-bottom">
			<view class="bottom-total">
				<text class="bottom-total-symbol">¥</text>
				<text class="bottom-total-value">{{ formatMoney(totalPrice) }}</text>
			</view>
			<view class="bottom-actions">
				<view v-if="cartAdded" class="cart-inline-stepper">
					<view
						class="quantity-btn"
						:class="{ 'quantity-btn-disabled': quantity <= 1 }"
						@tap.stop="decreaseQuantity"
					><text class="quantity-btn-text">-</text></view>
					<text class="quantity-value">{{ quantity }}</text>
				</view>
				<view
					class="bottom-btn bottom-btn-secondary cart-add-btn"
					:class="{ 'bottom-btn-disabled': submitting || !detailProduct }"
					@tap.stop="addToCart"
				>
					<text class="bottom-btn-text">{{ addButtonText }}</text>
				</view>
				<view
					class="bottom-btn"
					:class="{ 'bottom-btn-disabled': submitting || !detailProduct }"
					@tap.stop="buyNow"
				>
					<text class="bottom-btn-text">{{ submitting ? '处理中...' : '立即购买' }}</text>
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
import { resolveProductVideo } from '@/utils/productVideoMap.js'

function toNumber(value) {
	const n = Number(value || 0)
	return Number.isFinite(n) ? n : 0
}

const FALLBACK_TEMPS = [
	{ key: 'temp-hot', label: '热', value: 'hot' },
	{ key: 'temp-warm', label: '温', value: 'warm' },
	{ key: 'temp-iced', label: '冰', value: 'iced' }
]
const FALLBACK_SUGARS = [
	{ key: 'sugar-normal', label: '正常糖', value: 'normal' },
	{ key: 'sugar-less', label: '少糖', value: 'less' },
	{ key: 'sugar-half', label: '半糖', value: 'half' },
	{ key: 'sugar-none', label: '无糖', value: 'none' }
]
const FALLBACK_CUPS = [
	{ key: 'cup-medium', label: '中杯', value: 'medium' },
	{ key: 'cup-large', label: '大杯', value: 'large' }
]

export default {
	name: 'ProductCardBack',
	props: {
		product: { type: Object, default: function () { return null } },
		shopId: { type: [Number, String], default: 1 },
		tableNo: { type: String, default: '' },
		active: { type: Boolean, default: false }
	},
	data: function () {
		return {
			detailProduct: null,
			loading: false,
			submitting: false,
			quantity: 1,
			cartAdded: false,
			cartItem: null,
			singleSelections: {},
			multiSelections: {},
			fallback: { temperature: 'hot', sugar: 'normal', cup: 'medium' },
			fallbackTemps: FALLBACK_TEMPS,
			fallbackSugars: FALLBACK_SUGARS,
			fallbackCups: FALLBACK_CUPS,
			currentDetailKey: null
		}
	},
	watch: {
		active: function (val) {
			if (val) this.ensureDetailLoaded()
		},
		product: function () {
			if (this.active) this.ensureDetailLoaded()
		}
	},
	mounted: function () {
		if (this.active) this.ensureDetailLoaded()
	},
	computed: {
		headTitle: function () {
			return (this.detailProduct && this.detailProduct.productName)
				|| (this.product && this.product.productName) || '商品详情'
		},
		headSubtitle: function () {
			const source = this.detailProduct || this.product || {}
			return source.subTitle || source.flavorNotes || source.description || source.remark || ''
		},
		hasVideo: function () {
			return !!resolveProductVideo(this.detailProduct || this.product)
		},
		hasSpecs: function () {
			return !!(this.detailProduct && Array.isArray(this.detailProduct.specs) && this.detailProduct.specs.length)
		},
		selectedOptions: function () {
			if (!this.hasSpecs) return []
			const result = []
			const self = this
			this.detailProduct.specs.forEach(function (spec) {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (self.isMultipleSpec(spec)) {
					const ids = self.multiSelections[spec.specId] || []
					options.forEach(function (option) {
						if (ids.indexOf(option.optionId) !== -1) result.push({ spec: spec, option: option })
					})
					return
				}
				const id = self.singleSelections[spec.specId]
				options.forEach(function (option) {
					if (option.optionId === id) result.push({ spec: spec, option: option })
				})
			})
			return result
		},
		selectedSpecText: function () {
			return this.selectedOptions.map(function (entry) { return entry.option.optionName }).join(' / ')
		},
		fallbackSelectedText: function () {
			if (this.hasSpecs) return ''
			const t = FALLBACK_TEMPS.find(function (i) { return i.value === this.fallback.temperature }, this)
			const s = FALLBACK_SUGARS.find(function (i) { return i.value === this.fallback.sugar }, this)
			const c = FALLBACK_CUPS.find(function (i) { return i.value === this.fallback.cup }, this)
			return [t && t.label, s && s.label, c && c.label].filter(Boolean).join(' / ')
		},
		combinedSelectedText: function () {
			return this.selectedSpecText || this.fallbackSelectedText
		},
		detailDescription: function () {
			const source = this.detailProduct || this.product || {}
			return source.description || source.remark || source.flavorNotes || ''
		},
		productImage: function () {
			const source = this.detailProduct || this.product || {}
			return resolveImageUrl(source.imageUrl || source.productImage || '')
		},
		currentUnitPrice: function () {
			const base = this.detailProduct
				? toNumber(this.detailProduct.price)
				: (this.product ? toNumber(this.product.price) : 0)
			const extra = this.selectedOptions.reduce(function (sum, entry) {
				return sum + toNumber(entry.option.extraPrice)
			}, 0)
			return base + extra
		},
		totalPrice: function () {
			return this.currentUnitPrice * this.quantity
		},
		addButtonText: function () {
			if (this.submitting) return this.cartAdded ? '加购中...' : '加入中...'
			return this.cartAdded ? '加购' : '加入购物车'
		}
	},
	methods: {
		toNumber: toNumber,
		formatMoney: function (value) { return toNumber(value).toFixed(2) },
		authHeader: function () {
			const token = getToken()
			const header = {}
			if (token) {
				header.Authorization = 'Bearer ' + token
				header['X-Wx-Token'] = token
			}
			return header
		},
		productKey: function () {
			const p = this.product
			if (!p) return null
			return p.productId || p.id || p.product_id || null
		},
		ensureDetailLoaded: function () {
			const key = this.productKey()
			if (!key) return
			if (this.currentDetailKey === key && this.detailProduct) return
			this.currentDetailKey = key
			this.loadProductDetail(key)
		},
		onClose: function () {
			this.$emit('close')
		},
		onPlayVideo: function () {
			const url = resolveProductVideo(this.detailProduct || this.product)
			if (!url) {
				showError('该商品暂无讲解视频')
				return
			}
			this.$emit('play-video', { product: this.detailProduct || this.product, videoUrl: url })
		},
		loadProductDetail: function (productId) {
			const self = this
			self.loading = true
			self.detailProduct = null
			self.quantity = 1
			self.cartAdded = false
			self.cartItem = null
			self.singleSelections = {}
			self.multiSelections = {}
			self.fallback = { temperature: 'hot', sugar: 'normal', cup: 'medium' }
			requestPromise({
				url: scanMenuApi.productDetail + productId,
				method: 'GET',
				header: self.authHeader()
			}).then(function (res) {
				if (!isSuccessResponse(res)) {
					self.detailProduct = self.product ? Object.assign({}, self.product) : null
					return
				}
				self.detailProduct = (res.data && res.data.data)
					|| (self.product ? Object.assign({}, self.product) : null)
				self.initSpecSelections()
			}).catch(function () {
				self.detailProduct = self.product ? Object.assign({}, self.product) : null
			}).then(function () {
				self.loading = false
			})
		},
		initSpecSelections: function () {
			const single = {}
			const multi = {}
			const specs = (this.detailProduct && this.detailProduct.specs) || []
			const self = this
			specs.forEach(function (spec) {
				const options = Array.isArray(spec.options) ? spec.options : []
				if (self.isMultipleSpec(spec)) {
					multi[spec.specId] = options
						.filter(function (option) { return Number(option.isDefault) === 1 })
						.map(function (option) { return option.optionId })
					return
				}
				const def = options.find(function (option) { return Number(option.isDefault) === 1 })
					|| options[0] || null
				single[spec.specId] = def ? def.optionId : null
			})
			this.singleSelections = single
			this.multiSelections = multi
		},
		isMultipleSpec: function (spec) {
			return String((spec && spec.specType) || 'single') === 'multiple'
		},
		specRuleText: function (spec) {
			const required = Number(spec && spec.required) === 1 ? '必选' : '可选'
			return this.isMultipleSpec(spec) ? required + ' · 多选' : required + ' · 单选'
		},
		isOptionSelected: function (spec, option) {
			if (!spec || !option) return false
			if (this.isMultipleSpec(spec)) {
				const ids = this.multiSelections[spec.specId] || []
				return ids.indexOf(option.optionId) !== -1
			}
			return this.singleSelections[spec.specId] === option.optionId
		},
		toggleOption: function (spec, option) {
			if (!spec || !option) return
			this.cartAdded = false
			this.cartItem = null
			this.quantity = 1
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
		selectFallback: function (key, value) {
			this.cartAdded = false
			this.cartItem = null
			this.quantity = 1
			this.$set(this.fallback, key, value)
		},
		decreaseQuantity: function () {
			if (this.quantity <= 1 || this.submitting) return
			if (this.cartAdded && this.cartItem && this.cartItem.id) {
				this.updateAddedCartQuantity(this.quantity - 1)
				return
			}
			this.quantity -= 1
		},
		buildSpecJson: function () {
			if (this.hasSpecs) {
				const specs = this.detailProduct.specs || []
				const self = this
				const result = specs.map(function (spec) {
					const entries = self.selectedOptions.filter(function (entry) {
						return entry.spec.specId === spec.specId
					})
					return {
						specId: spec.specId,
						specName: spec.specName,
						specType: spec.specType,
						optionIds: entries.map(function (e) { return e.option.optionId }),
						optionNames: entries.map(function (e) { return e.option.optionName })
					}
				}).filter(function (item) { return item.optionIds.length > 0 })
				return JSON.stringify(result)
			}
			return JSON.stringify([
				{ specName: '温度', value: this.fallback.temperature },
				{ specName: '糖度', value: this.fallback.sugar },
				{ specName: '杯型', value: this.fallback.cup }
			])
		},
		buildCartPayload: function () {
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
		getAddButtonPoint: function () {
			return new Promise((resolve) => {
				uni.createSelectorQuery()
					.in(this)
					.select('.cart-add-btn')
					.boundingClientRect(function (rect) {
						if (!rect) {
							resolve(null)
							return
						}
						resolve({
							x: rect.left + rect.width / 2,
							y: rect.top + rect.height / 2
						})
					})
					.exec()
			})
		},
		submitCart: function (quantity, flyFrom) {
			const self = this
			const payload = self.buildCartPayload()
			payload.quantity = quantity || payload.quantity
			return requestPromise({
				url: scanCartApi.add,
				method: 'POST',
				header: Object.assign({ 'Content-Type': 'application/json' }, self.authHeader()),
				data: payload
			}).then(function (res) {
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '加入购物车失败')
					return false
				}
				const saved = (res.data && res.data.data) || null
				self.$emit('added', {
					product: self.detailProduct || self.product,
					cart: saved,
					action: 'add',
					flyFrom: flyFrom || null
				})
				return saved || true
			})
		},
		updateAddedCartQuantity: function (quantity) {
			if (!this.cartItem || !this.cartItem.id || this.submitting) return
			const self = this
			self.submitting = true
			requestPromise({
				url: scanCartApi.update,
				method: 'PUT',
				header: Object.assign({ 'Content-Type': 'application/json' }, self.authHeader()),
				data: { id: self.cartItem.id, quantity: quantity }
			}).then(function (res) {
				if (!isSuccessResponse(res)) {
					showError((res && res.data && res.data.msg) || '更新失败')
					return
				}
				self.quantity = quantity
				self.cartItem.quantity = quantity
				self.$emit('added', { product: self.detailProduct || self.product, cart: self.cartItem, action: 'decrease' })
			}).catch(function () {
				showError('更新失败')
			}).then(function () {
				self.submitting = false
			})
		},
		addToCart: function () {
			const target = this.detailProduct || this.product
			if (!target || this.submitting) return
			if (!ensureLocalLogin('请先登录后再加入购物车')) return
			const self = this
			const wasAdded = self.cartAdded
			self.submitting = true
			self.getAddButtonPoint().then(function (flyFrom) {
				return self.submitCart(wasAdded ? 1 : self.quantity, flyFrom)
			}).then(function (saved) {
				if (saved) {
					self.cartAdded = true
					if (saved !== true) {
						self.cartItem = saved
						self.quantity = saved.quantity || self.quantity
					}
					showSuccess(wasAdded ? '已加购' : '已加入购物车')
				}
			}).catch(function () {
				showError('加入购物车失败')
			}).then(function () {
				self.submitting = false
			})
		},
		buyNow: function () {
			const target = this.detailProduct || this.product
			if (!target || this.submitting) return
			if (!ensureLocalLogin('请先登录后再购买')) return
			const self = this
			self.submitting = true
			self.submitCart().then(function (ok) {
				if (ok) {
					self.$emit('checkout')
					self.$emit('close')
				}
			}).catch(function () {
				showError('购买失败，请稍后重试')
			}).then(function () {
				self.submitting = false
			})
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.card-back-shell {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	background:
		linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(251, 247, 242, 0.98) 100%);
	border-radius: 30rpx;
	border: 2rpx solid rgba(255, 255, 255, 0.82);
	box-shadow: 0 22rpx 50rpx rgba(32, 26, 23, 0.2);
	overflow: hidden;
	box-sizing: border-box;
}

.back-head {
	position: relative;
	padding: 24rpx 24rpx 20rpx;
	border-bottom: 2rpx solid rgba(232, 224, 215, 0.52);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	flex-shrink: 0;
}

.back-thumb-wrap {
	width: 104rpx;
	height: 104rpx;
	flex-shrink: 0;
	border-radius: 20rpx;
	overflow: hidden;
	background: $accent-surface;
	border: 2rpx solid rgba(232, 224, 215, 0.86);
	box-shadow: 0 8rpx 18rpx rgba(32, 26, 23, 0.08);
}

.back-thumb-image {
	display: block;
	width: 100%;
	height: 100%;
}

.back-head-copy {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}

.back-title {
	font-family: $font-family;
	font-size: 34rpx;
	font-weight: 800;
	color: $text-primary;
	line-height: 1.2;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.back-subtitle {
	font-family: $font-family;
	font-size: 23rpx;
	color: $text-secondary;
	line-height: 1.5;
	display: -webkit-box;
	-webkit-line-clamp: 1;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.head-video-btn {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 10rpx 18rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #7b5138 0%, #b9822e 100%);
	box-shadow: 0 10rpx 20rpx rgba(111, 78, 55, 0.2);
}

.head-video-btn-press {
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

.back-close {
	width: 56rpx;
	height: 56rpx;
	flex-shrink: 0;
	border-radius: 50%;
	background: rgba(111, 78, 55, 0.08);
	border: 2rpx solid rgba(111, 78, 55, 0.1);
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-close-press { opacity: 0.65; }

.back-close-text {
	font-size: 38rpx;
	line-height: 1;
	color: $text-secondary;
	margin-top: -4rpx;
}

.back-body {
	flex: 1;
	min-height: 0;
	padding: 22rpx 0 0;
	box-sizing: border-box;
}

.back-loading {
	padding: 40rpx 0;
	text-align: center;
	color: $text-tertiary;
	font-size: 26rpx;
}

.back-section {
	margin: 0 24rpx 22rpx;
	padding: 22rpx 20rpx;
	border-radius: 22rpx;
	background: rgba(255, 255, 255, 0.74);
	border: 2rpx solid rgba(232, 224, 215, 0.72);
	box-sizing: border-box;
}

.spec-block + .spec-block {
	margin-top: 22rpx;
}

.spec-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
	margin-bottom: 16rpx;
}

.spec-title {
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
	flex: 1;
	min-width: 0;
}

.spec-rule {
	font-family: $font-family;
	font-size: 22rpx;
	color: $text-tertiary;
	white-space: nowrap;
	flex-shrink: 0;
	text-align: right;
}

.spec-options {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 14rpx;
}

.spec-chip {
	min-height: 64rpx;
	padding: 12rpx 22rpx;
	border-radius: 999rpx;
	background: $bg-card;
	border: 2rpx solid rgba(111, 78, 55, 0.12);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
}

.spec-chip-active {
	background: $accent-primary;
	border-color: $accent-primary;
	box-shadow: 0 10rpx 20rpx rgba(111, 78, 55, 0.14);
}

.spec-chip-name {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $text-primary;
	line-height: 1.2;
	text-align: center;
}

.spec-chip-active .spec-chip-name,
.spec-chip-active .spec-chip-price {
	color: #ffffff;
}

.spec-chip-price {
	margin-top: 2rpx;
	font-family: $font-family;
	font-size: 20rpx;
	color: $text-secondary;
	line-height: 1.1;
	text-align: center;
}

.cart-inline-stepper {
	display: flex;
	align-items: center;
	gap: 10rpx;
	flex-shrink: 0;
}

.quantity-btn {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background: $bg-card;
	border: 2rpx solid rgba(111, 78, 55, 0.14);
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
}

.quantity-btn-text {
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
	margin-top: -4rpx;
}

.quantity-btn-disabled { opacity: 0.4; }

.quantity-value {
	min-width: 36rpx;
	text-align: center;
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 600;
}

.selected-spec-box {
	margin: 0 24rpx 22rpx;
	padding: 16rpx 20rpx;
	border-radius: $radius-sm;
	background: rgba(111, 78, 55, 0.06);
	border: 2rpx solid rgba(111, 78, 55, 0.08);
	box-sizing: border-box;
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

.back-bottom-padding { height: 24rpx; }

.back-bottom {
	padding: 16rpx 24rpx 22rpx;
	background: rgba(255, 255, 255, 0.92);
	border-top: 2rpx solid rgba(232, 224, 215, 0.72);
	backdrop-filter: blur(18rpx);
	-webkit-backdrop-filter: blur(18rpx);
	display: flex;
	flex-direction: column;
	gap: 14rpx;
	flex-shrink: 0;
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
	font-size: 42rpx;
	font-weight: 700;
	color: $text-primary;
	line-height: 1;
}

.bottom-actions {
	width: 100%;
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.bottom-btn {
	height: 76rpx;
	flex: 1;
	padding: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: $radius-sm;
	background: $accent-primary;
	min-width: 0;
	box-sizing: border-box;
	box-shadow: 0 10rpx 20rpx rgba(111, 78, 55, 0.16);
}

.bottom-btn-secondary {
	background: rgba(111, 78, 55, 0.08);
	border: 2rpx solid rgba(111, 78, 55, 0.14);
	box-shadow: none;
}

.bottom-btn-text {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: #ffffff;
}

.bottom-btn-secondary .bottom-btn-text {
	color: $accent-primary;
}

.bottom-btn-disabled { opacity: 0.5; }
</style>
