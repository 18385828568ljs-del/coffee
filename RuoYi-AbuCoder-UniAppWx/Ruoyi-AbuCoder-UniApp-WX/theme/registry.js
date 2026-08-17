export const COMPONENT_REGISTRY = Object.freeze({
	shopHeader: ['centered', 'compact'],
	activityBanner: ['single', 'carousel'],
	categoryNav: ['icon-grid', 'text-row'],
	productCard: ['vertical', 'horizontal'],
	tabBar: ['standard', 'brand'],
	profileHeader: ['brand', 'minimal']
})

export function isAllowedVariant(componentKey, variant) {
	const variants = COMPONENT_REGISTRY[componentKey]
	return Array.isArray(variants) && variants.includes(variant)
}
