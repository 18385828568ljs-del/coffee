<template>
	<view />
</template>

<script>
export default {
	onLoad(options = {}) {
		const query = Object.keys(options)
			.filter((key) => key !== 'decoratorPreview')
			.map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
		query.push('decoratorPreview=1')
		uni.reLaunch({ url: `/pages/index/index?${query.join('&')}` })
	}
}
</script>
