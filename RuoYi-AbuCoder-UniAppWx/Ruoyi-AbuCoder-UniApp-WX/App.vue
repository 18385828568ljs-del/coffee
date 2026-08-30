<script>
	import { restoreLocalSession } from '@/utils/session.js'
	import { themeRuntime } from '@/theme/runtime.js'
	import { baseUrl } from '@/utils/apiconfig.js'

	export default {
		onLaunch: async function(e) {
			await restoreLocalSession();
			try {
				if (uni.getSystemInfoSync) themeRuntime.setSystemTheme(uni.getSystemInfoSync().theme)
				if (uni.onThemeChange) uni.onThemeChange((event) => themeRuntime.setSystemTheme(event && event.theme))
			} catch (error) {}
			// #ifdef MP-WEIXIN
			// 检测是否可以调用getUpdateManager检查更新
			if (!uni.canIUse("getUpdateManager")) return;
			const updateManager = uni.getUpdateManager();
			// console.log(updateManager)
			// 获取全局唯一的版本更新管理器，用于管理小程序更新
			updateManager.onCheckForUpdate(function(res) {
				// 监听向微信后台请求检查更新结果事件 
				console.log("是否有新版本：" + res.hasUpdate);
				if (res.hasUpdate) {
					//如果有新版本                
					// 小程序有新版本，会主动触发下载操作        
					updateManager.onUpdateReady(function() {
						//当新版本下载完成，会进行回调          
						uni.showModal({
							title: '更新提示',
							content: '新版本已经准备好，单击确定重启小程序',
							showCancel: false,
							success: function(res) {
								if (res.confirm) {
									// 新的版本已经下载好，调用 applyUpdate 应用新版本并重启小程序               
									updateManager.applyUpdate();
								}
							}
						})
					})
					// 小程序有新版本，会主动触发下载操作       
					updateManager.onUpdateFailed(function() {
						//当新版本下载失败，会进行回调          
						uni.showModal({
							title: '提示',
							content: '检查到有新版本，但下载失败，请稍后尝试',
							showCancel: false,
						})
					})
				}
			});
			// #endif
		},
		onShow: function() {},
		onHide: function() {console.log('App Hide')},
		globalData: {
			userinfo: null,
			token:'',
			apiBaseUrl: baseUrl,
		},
	}
</script>

<style lang="scss">
	/*每个页面公共css */
	@import "@/uni_modules/uview-ui/index.scss";

	$skin-text-roles: (
		pageTitle: page-title, sectionTitle: section-title, bannerTitle: banner-title,
		bannerSubtitle: banner-subtitle, actionTitle: action-title, actionSubtitle: action-subtitle,
		productTitle: product-title, price: price, metaText: meta-text, bodyText: body-text,
		panelTitle: panel-title, optionTitle: option-title, optionText: option-text,
		buttonPrimary: button-primary, buttonSecondary: button-secondary, memberTitle: member-title,
		memberValue: member-value, emptyTitle: empty-title, emptyDescription: empty-description,
		tabText: tab-text, tabTextActive: tab-text-active
	);

	@each $attribute, $variable in $skin-text-roles {
		[data-text-role="#{$attribute}"] {
			font-family: var(--skin-#{$variable}-family, inherit) !important;
			font-style: var(--skin-#{$variable}-style, normal) !important;
			font-size: var(--skin-#{$variable}-size, inherit) !important;
			font-weight: var(--skin-#{$variable}-weight, inherit) !important;
			line-height: var(--skin-#{$variable}-line-height, inherit) !important;
			letter-spacing: var(--skin-#{$variable}-letter-spacing, 0) !important;
			color: var(--skin-#{$variable}-color, inherit) !important;
			text-shadow: var(--skin-#{$variable}-shadow, none) !important;
		}
	}

	/* #ifdef H5 */
	html,
	body,
	#app,
	uni-app,
	uni-page,
	uni-page-wrapper,
	uni-page-body {
		width: 100%;
		min-height: 100%;
		margin: 0;
	}

	body {
		overflow-x: hidden;
		background: var(--theme-page, #FFFFFF);
	}

	/* The project renders its own themed Web tab bar. */
	uni-tabbar.uni-tabbar-bottom {
		display: none;
	}

	/* #endif */
</style>
