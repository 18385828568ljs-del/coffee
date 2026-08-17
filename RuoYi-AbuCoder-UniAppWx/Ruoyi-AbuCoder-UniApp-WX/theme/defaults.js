const baseTokens = {
	colors: {
		primary: '#6F4E37',
		pageBackground: '#F8F3ED',
		surface: '#FFFFFF',
		textPrimary: '#2B2118',
		textSecondary: '#74685E',
		buttonBackground: '#6F4E37',
		buttonText: '#FFFFFF',
		border: '#E8DED4'
	},
	radius: { card: 12, button: 20, image: 8 },
	shadow: { card: 'soft' }
}

const baseComponents = {
	shopHeader: { variant: 'centered' },
	activityBanner: { visible: true, variant: 'single' },
	categoryNav: { variant: 'icon-grid' },
	productCard: { variant: 'vertical' },
	tabBar: { variant: 'standard' },
	profileHeader: { variant: 'brand' }
}

const baseContent = {
	homeBanner: { visible: true, title: '一杯好咖啡，从这里开始', subtitle: '现点现做，认真对待每一杯' }
}

export const THEME_TEMPLATES = Object.freeze({
	coffee: {
		name: '经典咖啡',
		config: { schemaVersion: '1.0.0', tokens: baseTokens, brand: {}, components: baseComponents }
	},
	cobalt: {
		name: '清晰钴蓝',
		config: {
			schemaVersion: '1.0.0',
			tokens: {
				colors: { primary: '#155EEF', pageBackground: '#F4F7FB', surface: '#FFFFFF', textPrimary: '#111827', textSecondary: '#536174', buttonBackground: '#155EEF', buttonText: '#FFFFFF', border: '#D7DEE8' },
				radius: { card: 6, button: 10, image: 4 }, shadow: { card: 'medium' }
			},
			brand: {},
			components: {
				shopHeader: { variant: 'compact' }, activityBanner: { visible: true, variant: 'carousel' },
				categoryNav: { variant: 'text-row' }, productCard: { variant: 'horizontal' },
				tabBar: { variant: 'brand' }, profileHeader: { variant: 'minimal' }
			}
		}
	},
	forest: {
		name: '深林鼠尾草',
		config: {
			schemaVersion: '1.0.0',
			tokens: {
				colors: { primary: '#24543B', pageBackground: '#F2F5F0', surface: '#FFFFFF', textPrimary: '#17251D', textSecondary: '#5A6B61', buttonBackground: '#24543B', buttonText: '#FFFFFF', border: '#CED9D1' },
				radius: { card: 16, button: 16, image: 12 }, shadow: { card: 'soft' }
			},
			brand: {},
			components: {
				shopHeader: { variant: 'centered' }, activityBanner: { visible: true, variant: 'single' },
				categoryNav: { variant: 'icon-grid' }, productCard: { variant: 'vertical' },
				tabBar: { variant: 'brand' }, profileHeader: { variant: 'brand' }
			}
		}
	}
})

export const DEFAULT_SKIN_CONFIG = Object.freeze({
	schemaVersion: 1,
	themeVersion: 1,
	page: { backgroundColor: '#FFFFFF', background: { type: 'solid', color: '#FFFFFF' }, textColor: '#332C28', secondaryTextColor: '#8A7D74' },
	colors: {
		primary: '#6F4E37', pageBackground: '#FFFFFF', cardBackground: '#FFFFFF',
		textPrimary: '#332C28', textSecondary: '#8A7D74'
	},
	content: baseContent,
	assets: SKIN_COMPONENT_KEYS.reduce((assets, key) => { assets[key] = key === 'homeBanner' ? [] : null; return assets }, {}),
	productImages: {},
	typography: cloneDefaultTypography(),
	slots: {
		heroBanner: { backgroundType: 'color', backgroundColor: '#6F4E37', backgroundImage: null, fit: 'cover' },
		orderCard: { backgroundType: 'color', backgroundColor: '#FFFFFF', backgroundImage: null, iconColor: '#745848', textColor: '#302720', secondaryTextColor: '#C28B62', radius: 20, shadow: 'light' },
		shopCard: { backgroundType: 'color', backgroundColor: '#FFFFFF', backgroundImage: null, iconColor: '#332C28', textColor: '#302720', secondaryTextColor: '#8A7D74', radius: 20, shadow: 'light' },
		welcomeBanner: { backgroundType: 'color', backgroundColor: '#E8D4C3', backgroundImage: null, fit: 'cover', radius: 8 },
		aboutSection: { backgroundColor: '#FFFFFF', titleColor: '#332C28', image: '' },
		tabBar: { backgroundColor: '#FFFFFF', textColor: '#777777', activeTextColor: '#44352C', iconColor: '#999999', activeIconColor: '#44352C', activeBackgroundColor: '#F3E4D6' }
	}
})

// First-phase local skins. Business content and component geometry stay outside
// these objects; only visual tokens and background slots are swapped at runtime.
export const LOCAL_SKINS = Object.freeze({
	vintage: {
		schemaVersion: 1,
		themeVersion: 1,
		page: { backgroundColor: '#F4EBDD', textColor: '#38291F', secondaryTextColor: '#846B59' },
		colors: { primary: '#6D4A2E', pageBackground: '#F4EBDD', cardBackground: '#FFF9EF', textPrimary: '#38291F', textSecondary: '#846B59' },
		content: baseContent,
		assets: {
			homeBanner: ['/static/skin/vintage/home-banner.png'],
			actionCard: '/static/skin/vintage/action-card.png',
			sectionBanner: '/static/skin/vintage/section-banner.png',
			aboutImage: '/static/banner/about-us.jpg',
			productCard: '/static/skin/vintage/product-card.png',
			specPanel: '/static/skin/vintage/spec-panel.png',
			emptyCart: '/static/skin/vintage/empty-cart.png',
			cartPanel: '/static/skin/vintage/cart-panel.png',
			checkoutBar: '/static/skin/vintage/checkout-bar.png',
			memberCard: '/static/skin/vintage/member-card.png',
			tabBar: '/static/skin/vintage/tab-bar.png'
		},
		productImages: {},
		typography: {
			...cloneDefaultTypography(),
			productTitle: { color: '#38271F', fontSize: 18, fontWeight: 700, lineHeight: 1.3 },
			price: { color: '#875629', fontSize: 22, fontWeight: 800, lineHeight: 1.2 },
			buttonPrimary: { color: '#FFFFFF', fontSize: 15, fontWeight: 700, lineHeight: 1.2 },
			buttonSecondary: { color: '#6D4A2E', fontSize: 14, fontWeight: 600, lineHeight: 1.2 },
			memberTitle: { color: '#4D301E', fontSize: 16, fontWeight: 700, lineHeight: 1.3 },
			memberValue: { color: '#4D301E', fontSize: 22, fontWeight: 800, lineHeight: 1.2 },
			metaText: { color: '#846B59', fontSize: 11, fontWeight: 400, lineHeight: 1.4 },
			tabTextActive: { color: '#6D4A2E', fontSize: 11, fontWeight: 700, lineHeight: 1.2 }
		},
		slots: {
			heroBanner: { backgroundType: 'color', backgroundColor: '#D6C5A5', backgroundImage: null, fit: 'cover' },
			orderCard: { backgroundType: 'color', backgroundColor: '#FFF9EF', backgroundImage: null, iconColor: '#6D4A2E', textColor: '#38291F', secondaryTextColor: '#A77A50', radius: 18, shadow: 'light' },
			shopCard: { backgroundType: 'color', backgroundColor: '#FFF9EF', backgroundImage: null, iconColor: '#6D4A2E', textColor: '#38291F', secondaryTextColor: '#846B59', radius: 18, shadow: 'light' },
			welcomeBanner: { backgroundType: 'color', backgroundColor: '#DCC8A8', backgroundImage: null, fit: 'cover', radius: 8 },
			aboutSection: { backgroundColor: '#FFF9EF', titleColor: '#38291F', image: '' },
			tabBar: { backgroundColor: '#F8EEDF', textColor: '#9B8068', activeTextColor: '#4D301E', iconColor: '#B29980', activeIconColor: '#4D301E', activeBackgroundColor: '#E7D1B2' }
		}
	},
	midnight: {
		schemaVersion: 1,
		themeVersion: 1,
		page: { backgroundColor: '#18211D', textColor: '#F5F0E8', secondaryTextColor: '#B5B9A9' },
		colors: { primary: '#D4B57D', pageBackground: '#18211D', cardBackground: '#26352D', textPrimary: '#F5F0E8', textSecondary: '#B5B9A9' },
		content: baseContent,
		assets: SKIN_COMPONENT_KEYS.reduce((assets, key) => { assets[key] = key === 'homeBanner' ? [] : null; return assets }, {}),
		productImages: {},
		typography: Object.keys(cloneDefaultTypography()).reduce((tokens, key) => {
			const source = cloneDefaultTypography()[key]
			tokens[key] = { ...source, color: ['price', 'buttonSecondary', 'tabTextActive'].includes(key) ? '#E2C58D' : (key === 'buttonPrimary' ? '#18211D' : '#F5F0E8') }
			return tokens
		}, {}),
		slots: {
			heroBanner: { backgroundType: 'color', backgroundColor: '#31483A', backgroundImage: null, fit: 'cover' },
			orderCard: { backgroundType: 'color', backgroundColor: '#26352D', backgroundImage: null, iconColor: '#D4B57D', textColor: '#F5F0E8', secondaryTextColor: '#C7A36D', radius: 18, shadow: 'medium' },
			shopCard: { backgroundType: 'color', backgroundColor: '#26352D', backgroundImage: null, iconColor: '#D4B57D', textColor: '#F5F0E8', secondaryTextColor: '#B5B9A9', radius: 18, shadow: 'medium' },
			welcomeBanner: { backgroundType: 'color', backgroundColor: '#31483A', backgroundImage: null, fit: 'cover', radius: 8 },
			aboutSection: { backgroundColor: '#26352D', titleColor: '#F5F0E8', image: '' },
			tabBar: { backgroundColor: '#202B25', textColor: '#9BA89E', activeTextColor: '#E2C58D', iconColor: '#819085', activeIconColor: '#E2C58D', activeBackgroundColor: '#34473B' }
		}
	}
})

export function cloneDefaultSkin() {
	return JSON.parse(JSON.stringify(DEFAULT_SKIN_CONFIG))
}

export function cloneLocalSkin(key = 'vintage') {
	return JSON.parse(JSON.stringify(LOCAL_SKINS[key] || LOCAL_SKINS.vintage))
}

export function cloneDefaultTheme(templateKey = 'coffee') {
	const template = THEME_TEMPLATES[templateKey] || THEME_TEMPLATES.coffee
	return JSON.parse(JSON.stringify(template.config))
}
import { SKIN_COMPONENT_KEYS } from './skin-registry.js'
import { cloneDefaultTypography } from './typography-registry.js'
