<template>
	<view class="profile-header" :class="`profile-header-${variant}`" @tap="$emit('action')">
		<view class="avatar"><image v-if="avatar" :src="avatar" mode="aspectFill" /><text v-else>{{ fallback }}</text></view>
		<view class="profile-copy"><text class="profile-name">{{ name }}</text><text class="profile-desc">{{ description }}</text></view>
		<button v-if="!isLogin" class="login-button" :disabled="loading" @tap.stop="$emit('login')">{{ loading ? '登录中...' : '登录' }}</button>
	</view>
</template>

<script>
export default {
	name: 'ThemeProfileHeader',
	props: { isLogin: Boolean, avatar: { type: String, default: '' }, name: { type: String, default: '点击登录' }, description: { type: String, default: '' }, fallback: { type: String, default: '?' }, loading: Boolean },
	computed: { variant() { return this.themeComponent('profileHeader').variant || 'brand' } }
}
</script>

<style scoped>
.profile-header { padding: 28rpx; display: flex; align-items: center; gap: 22rpx; border-radius: var(--theme-card-radius); border: 2rpx solid var(--theme-border); background: var(--theme-surface); box-shadow: var(--theme-card-shadow); }
.profile-header-brand { color: var(--theme-button-text); border-color: var(--theme-primary); background: var(--theme-primary); }
.avatar { width: 104rpx; height: 104rpx; flex: 0 0 auto; overflow: hidden; display: flex; align-items: center; justify-content: center; border-radius: 50%; background: var(--theme-border); font-size: 36rpx; font-weight: 700; }
.avatar image { width: 100%; height: 100%; }
.profile-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 7rpx; }
.profile-name { font-size: 34rpx; font-weight: 700; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.profile-desc { font-size: 22rpx; opacity: .74; }
.login-button { margin: 0; padding: 0 24rpx; height: 64rpx; color: var(--theme-button-text); background: var(--theme-button); border: 2rpx solid currentColor; border-radius: var(--theme-button-radius); font-size: 24rpx; line-height: 60rpx; }
.login-button::after { border: 0; }
</style>
