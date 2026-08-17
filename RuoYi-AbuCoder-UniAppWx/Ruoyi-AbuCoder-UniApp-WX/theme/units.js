export function themeRpx(value) {
	const numericValue = Number(value)
	if (!Number.isFinite(numericValue)) return '0'
	// #ifdef H5
	return `${numericValue / 2}px`
	// #endif
	// #ifndef H5
	return `${numericValue}rpx`
	// #endif
}
