<template>
	<view class="page">
		<app-nav title="个人资料" fallback-url="/pages/me/me" />

		<view class="content">
			<view class="profile-card">
				<button open-type="chooseAvatar" @chooseavatar="chooseaAatar" class="avatar-button">
					<view class="avatar">
						<image v-if="src" class="avatar-image" :src="src" mode="aspectFill"></image>
						<view v-else class="avatar-fallback">
							<text>{{ avatarFallbackText }}</text>
						</view>
					</view>
					<text class="avatar-action">更换头像</text>
				</button>

				<view class="form-card">
					<text class="form-label">昵称</text>
					<input
						class="name-input"
						v-model="username"
						type="nickname"
						placeholder="请输入昵称"
						placeholder-class="name-placeholder"
					/>
				</view>
			</view>

			<view class="bottom-actions">
				<button class="save-btn" @click="saveInfo">保存</button>
				<button class="cancel-btn" @click="cancel">取消</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { getUserInfo, getToken } from '@/utils/auth'
	import { loginApi, wxAvatarUploadUrl } from '@/utils/apiconfig.js'
	import { requestPromise, isSuccessResponse } from '@/utils/request-helper.js'
	import { normalizeWxUserInfo, setLocalSession } from '@/utils/session.js'

	export default {
		data() {
			return {
				src: '',
				username:'',
				userinfo:{},
				savepth:'',//真正保存的文件路径
				avatarUploading: false
			};
		},
		onShow() {
			this.userinfo = getUserInfo() || {};
			this.src = this.normalizeAvatar(this.userinfo.avatar)
			this.username = this.userinfo.nickname || this.userinfo.nickName || ''
		},
		computed: {
			avatarFallbackText() {
				const name = this.username || ''
				if (!name) return '?'
				const arr = [...name]
				return arr[0] || '?'
			}
		},
		methods:{
			normalizeAvatar(value) {
				const avatar = String(value || '').trim()
				if (!avatar) return ''
				const normalized = avatar.replace(/\\/g, '/')
				return /^\/?static\//i.test(normalized) ? '' : avatar
			},
			chooseaAatar(e){
				const avatarUrl = e && e.detail && e.detail.avatarUrl
				if (!avatarUrl) {
					return
				}
				this.src = avatarUrl
				this.savepth = ''
				this.uploadFile(avatarUrl);//选好了就开始上传
			},
			isTempAvatar(value) {
				return /^(https?:\/\/tmp\/|wxfile:\/\/|file:\/\/)/i.test(String(value || '').trim())
			},
			uploadFile(src){
				this.avatarUploading = true
				uni.showLoading({
					title:'头像上传中...'
				})
				uni.uploadFile({
					url: wxAvatarUploadUrl,//图片上传路径
					fileType:'image',//图片类型,
					name:'file',//对应接口的文件名称
					filePath:src,
					header:{//请求头
						Authorization: `Bearer ${getToken()}`,
						'X-Wx-Token': getToken()
					},
					success:(res)=>{
						//成功的回调
						//一般用于重新获取数据渲染页面
						let r = {}
						try {
							r = JSON.parse(res.data || '{}')
						} catch (e) {
							r = {}
						}
						if(r.code == 0 && r.url){
							this.savepth = r.url
						} else {
							uni.showToast({
								icon: 'none',
								title: (r && r.msg) || '头像上传失败，请重新选择',
								duration: 2000
							})
						}
					},
					fail:(err)=>{
					//失败的回调
						uni.showToast({
							icon: 'none',
							title: '头像上传失败，请检查网络',
							duration: 2000
						})
					},
					complete: () => {
						this.avatarUploading = false
						uni.hideLoading()
					}
				
				})
			},
			//保存数据
			saveInfo(){
				if (this.avatarUploading) {
					uni.showToast({
						icon: 'none',
						title: '头像上传中，请稍后保存',
						duration: 2000
					})
					return
				}
				if (this.isTempAvatar(this.src) && !this.savepth) {
					uni.showToast({
						icon: 'none',
						title: '头像还没有上传成功，请重新选择',
						duration: 2000
					})
					return
				}
				const form = {}
				const savedAvatar = this.savepth || (this.isTempAvatar(this.src) ? '' : this.src)
				form.nickName = this.username
				form.avatarUrl = savedAvatar
				uni.showLoading({
					title:'保存中...'
				})
				requestPromise({
					url: loginApi.wxLogin.replace('/wxlogin', '/saveUserInfo'),
					method: 'POST',
					data: form,
					header: {
						Authorization: `Bearer ${getToken()}`
					}
				}).then(res=>{
					if(isSuccessResponse(res)){
						uni.hideLoading()
						const userData = normalizeWxUserInfo(res.data.data || {}, {
							nickName: this.username,
							avatar: savedAvatar
						})
						setLocalSession(res.data.token, userData)
						uni.navigateBack({
							delta:1//返回上一页
						})
					}else{
						uni.hideLoading()
						uni.showToast({
							icon:"none",
							title: (res.data && res.data.msg) || "保存失败，请稍后试试！",
							duration:2000
						})
					}
				}).catch(() => {
					uni.hideLoading()
					uni.showToast({
						icon:"none",
						title: "保存失败，请稍后试试！",
						duration:2000
					})
				})
			},
			//取消保存
			cancel(){
				uni.navigateBack({
					delta:1,//返回上一层
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
@import '@/styles/design-tokens.scss';

.page {
	@include page-shell;
}

.content {
	padding: 0 $space-page 32rpx;
	box-sizing: border-box;
}

.profile-card {
	@include card(34rpx 30rpx, $radius-md);
	display: flex;
	flex-direction: column;
	gap: 34rpx;
	background:
		linear-gradient(135deg, rgba(255, 250, 245, 0.98) 0%, rgba(244, 238, 232, 0.98) 100%);
}

.avatar-button {
	margin: 0;
	padding: 0;
	background: transparent;
	border-radius: 0;
	line-height: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 18rpx;
}

.avatar-button::after {
	border: 0;
}

.avatar {
	width: 168rpx;
	height: 168rpx;
	border-radius: 50%;
	overflow: hidden;
	background: $accent-primary-soft;
	box-shadow:
		0 16rpx 34rpx rgba(111, 78, 55, 0.18),
		inset 0 0 0 4rpx rgba(255, 255, 255, 0.8);
	display: flex;
	align-items: center;
	justify-content: center;
}

.avatar-image,
.avatar-fallback {
	width: 100%;
	height: 100%;
}

.avatar-image {
	display: block;
}

.avatar-fallback {
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(180deg, #A27A5C 0%, #6F4E37 100%);
	color: #fff;
	font-size: 48rpx;
	font-weight: 700;
}

.avatar-action {
	font-family: $font-family;
	font-size: 24rpx;
	font-weight: 600;
	color: $accent-primary;
}

.form-card {
	@include scan-surface-card(24rpx, $radius-sm);
	display: flex;
	align-items: center;
	gap: 22rpx;
}

.form-label {
	flex-shrink: 0;
	font-family: $font-family;
	font-size: 26rpx;
	font-weight: 600;
	color: $text-primary;
}

.name-input {
	flex: 1;
	min-width: 0;
	height: 64rpx;
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 500;
	color: $text-primary;
}

.name-placeholder {
	color: $text-tertiary;
}

.bottom-actions {
	margin-top: 34rpx;
	display: flex;
	gap: 18rpx;
}

.save-btn,
.cancel-btn {
	flex: 1;
	height: 88rpx;
	border-radius: $radius-sm;
	font-family: $font-family;
	font-size: 28rpx;
	font-weight: 700;
	line-height: 88rpx;
}

.save-btn::after,
.cancel-btn::after {
	border: 0;
}

.save-btn {
	background: $accent-primary;
	color: #fff;
}

.cancel-btn {
	background: rgba(255, 255, 255, 0.96);
	color: $accent-primary;
	border: 2rpx solid rgba(111, 78, 55, 0.2);
}
</style>

