/**
 * 商品讲解视频映射配置
 *
 * 当前后端 /api/scanMenu/products 接口未返回 videoUrl / video_url / productVideo / introVideo
 * 等视频字段，所以前端在这里维护一份映射，按商品 id 取对应视频。
 *
 * 使用方式：
 *   import { resolveProductVideo } from '@/utils/productVideoMap.js'
 *   const url = resolveProductVideo(product)
 *
 * 如果后续后端接口直接返回视频字段，无需修改此文件，调用方会优先使用商品自带的字段。
 *
 * 视频文件存放位置：
 *   /static/videos/  目录下
 *   命名建议：product-{productId}.mp4，例如 /static/videos/product-1.mp4
 *   如果是远程 CDN，也可以直接写 https://... 完整地址。
 *
 * 如果某个商品没有配置视频，UI 会自动隐藏“观看商品讲解视频”入口。
 */

const PRODUCT_VIDEO_MAP = {
	// 示例配置：将下方注释取消并放入真实视频文件即可生效
	// 1: '/static/videos/product-1.mp4',
	// 2: '/static/videos/product-2.mp4',
	// 3: '/static/videos/product-3.mp4'
}

const PRODUCT_VIDEO_FIELDS = ['videoUrl', 'video_url', 'productVideo', 'introVideo', 'introductionVideo']

function pickFromProduct(product) {
	if (!product) return ''
	for (let i = 0; i < PRODUCT_VIDEO_FIELDS.length; i++) {
		const value = product[PRODUCT_VIDEO_FIELDS[i]]
		if (value && typeof value === 'string') {
			return value.trim()
		}
	}
	return ''
}

function pickFromMap(productId) {
	if (productId === undefined || productId === null || productId === '') return ''
	const value = PRODUCT_VIDEO_MAP[productId] || PRODUCT_VIDEO_MAP[String(productId)] || PRODUCT_VIDEO_MAP[Number(productId)]
	return value ? String(value).trim() : ''
}

export function resolveProductVideo(product) {
	const fromProduct = pickFromProduct(product)
	if (fromProduct) return fromProduct
	const productId = product && (product.productId || product.id)
	return pickFromMap(productId)
}

export function hasProductVideo(product) {
	return !!resolveProductVideo(product)
}

export default PRODUCT_VIDEO_MAP
