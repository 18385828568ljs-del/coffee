<template>
	<view v-if="visible" class="video-root" :class="{ 'video-visible': animating }">
		<view class="video-mask" @tap="close"></view>

		<view class="video-frame">
			<video
				v-if="videoUrl"
				:id="videoId"
				class="video-player"
				:src="videoUrl"
				:autoplay="true"
				:loop="false"
				:controls="true"
				:enable-progress-gesture="true"
				:show-center-play-btn="false"
				:show-fullscreen-btn="true"
				object-fit="contain"
				@ended="onEnded"
				@error="onError"
			>
				<cover-view class="video-close" @tap="close">
					<cover-view class="video-close-text">×</cover-view>
				</cover-view>
			</video>
		</view>
	</view>
</template>

<script>
let counter = 0

export default {
	name: 'ProductVideoPlayer',
	data() {
		return {
			visible: false,
			animating: false,
			videoUrl: '',
			videoId: 'product-video-' + (++counter)
		}
	},
	methods: {
		open(url) {
			if (!url) return
			this.videoUrl = url
			this.visible = true
			setTimeout(() => {
				this.animating = true
				this.$nextTick(() => {
					this.playSafely()
				})
			}, 30)
		},
		playSafely() {
			try {
				const ctx = uni.createVideoContext(this.videoId, this)
				if (ctx && typeof ctx.play === 'function') {
					ctx.play()
				}
			} catch (error) {}
		},
		stop() {
			try {
				const ctx = uni.createVideoContext(this.videoId, this)
				if (ctx) {
					if (typeof ctx.pause === 'function') ctx.pause()
					if (typeof ctx.stop === 'function') ctx.stop()
				}
			} catch (error) {}
		},
		close() {
			this.stop()
			this.animating = false
			setTimeout(() => {
				this.visible = false
				this.videoUrl = ''
				this.$emit('close')
			}, 220)
		},
		onEnded() {
			this.$emit('ended')
		},
		onError() {
			this.stop()
			this.animating = false
			setTimeout(() => {
				this.visible = false
				this.videoUrl = ''
				this.$emit('error')
			}, 220)
			uni.showToast({ title: '视频加载失败', icon: 'none' })
		}
	}
}
</script>

<style lang="scss" scoped>
.video-root {
	position: fixed;
	inset: 0;
	z-index: 1200;
	display: flex;
	align-items: center;
	justify-content: center;
}

.video-mask {
	position: absolute;
	inset: 0;
	background: rgba(0, 0, 0, 0.86);
	opacity: 0;
	transition: opacity 0.22s ease;
}

.video-visible .video-mask {
	opacity: 1;
}

.video-frame {
	position: relative;
	width: 100vw;
	height: 100vh;
	display: flex;
	align-items: center;
	justify-content: center;
	opacity: 0;
	transition: opacity 0.22s ease;
}

.video-visible .video-frame {
	opacity: 1;
}

.video-close {
	position: absolute;
	top: calc(env(safe-area-inset-top) + 24rpx);
	right: 24rpx;
	width: 72rpx;
	height: 72rpx;
	border-radius: 50%;
	background-color: rgba(0, 0, 0, 0.55);
	border: 2rpx solid rgba(255, 255, 255, 0.32);
	z-index: 5;
	box-sizing: border-box;
	text-align: center;
}

.video-close-text {
	display: block;
	color: #ffffff;
	font-size: 44rpx;
	line-height: 68rpx;
	text-align: center;
	width: 100%;
	height: 100%;
}

.video-player {
	width: 100vw;
	height: 80vh;
	position: relative;
}
</style>
