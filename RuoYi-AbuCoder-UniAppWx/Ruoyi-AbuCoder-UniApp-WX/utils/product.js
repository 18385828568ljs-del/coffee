/**
 * 商品相关工具函数
 */

/**
 * 解析商品ID（兼容多种字段名）
 * @param {Object} item 商品对象
 * @returns {Number|null} 商品ID
 */
export function resolveProductId(item) {
	if (!item) {
		return null
	}
	return item.productId || item.id || null
}

/**
 * 获取商品图片URL
 * @param {Object} item 商品对象
 * @returns {String} 图片URL
 */
export function getProductImage(item) {
	if (!item) {
		return ''
	}
	const images = getProductImages(item)
	return images[0] || ''
}

/**
 * 获取商品图片列表
 * @param {Object} item 商品对象
 * @returns {Array<String>} 图片URL列表
 */
export function getProductImages(item) {
	if (!item) {
		return []
	}
	const result = []
	const pushUrl = (url) => {
		if (typeof url !== 'string') {
			return
		}
		const normalized = url.trim()
		if (normalized && result.indexOf(normalized) === -1) {
			result.push(normalized)
		}
	}
	if (Array.isArray(item.imageUrls)) {
		item.imageUrls.forEach(pushUrl)
	}
	if (Array.isArray(item.productImages)) {
		item.productImages.forEach((image) => {
			if (typeof image === 'string') {
				pushUrl(image)
				return
			}
			if (image && typeof image === 'object') {
				pushUrl(image.imageUrl || image.url)
			}
		})
	}
	pushUrl(item.productImg)
	pushUrl(item.imageUrl)
	pushUrl(item.image)
	pushUrl(item.productImage)
	return result.slice(0, 6)
}

/**
 * 获取商品名称
 * @param {Object} item 商品对象
 * @returns {String} 商品名称
 */
export function getProductName(item) {
	if (!item) {
		return ''
	}
	return item.productName || item.name || ''
}

/**
 * 获取商品价格
 * @param {Object} item 商品对象
 * @returns {Number} 商品价格
 */
export function getProductPrice(item) {
	if (!item) {
		return 0
	}
	return item.salePrice || item.price || 0
}

/**
 * 获取商品原价
 * @param {Object} item 商品对象
 * @returns {Number|null} 商品原价
 */
export function getProductOriginalPrice(item) {
	if (!item) {
		return null
	}
	return item.originalPrice || null
}

/**
 * 检查商品是否有促销活动
 * @param {Object} item 商品对象
 * @returns {Boolean} 是否有促销
 */
export function hasPromotion(item) {
	if (!item) {
		return false
	}
	return !!(item.activityTags && item.activityTags.length > 0)
}

/**
 * 获取商品促销标签
 * @param {Object} item 商品对象
 * @returns {Array} 促销标签数组
 */
export function getPromotionTags(item) {
	if (!item || !item.activityTags) {
		return []
	}
	return item.activityTags
}
