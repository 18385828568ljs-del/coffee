-- Componentized SkinConfig V1 migration. Run after coffee_theme_decorator.sql.

INSERT INTO component_background_slots
    (slot_key, component_key, display_name, spec_version, output_width, output_height, render_mode, safe_area_json, status)
VALUES
    ('skin.homeBanner.background', 'homeBanner', '头部 Banner 背景', 1, 1500, 720, 'COVER', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.actionCard.background', 'actionCard', '功能按钮卡片背景', 1, 670, 360, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.sectionBanner.background', 'sectionBanner', '欢迎牌 / 分区横幅背景', 1, 1372, 288, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.productCard.background', 'productCard', '商品卡片背景', 1, 1372, 640, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.specPanel.background', 'specPanel', '商品规格弹窗背景', 1, 1372, 1440, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.emptyCart.background', 'emptyCart', '空购物车面板背景', 1, 1372, 1120, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.cartPanel.background', 'cartPanel', '购物车结算面板背景', 1, 1372, 360, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.checkoutBar.background', 'checkoutBar', '确认订单底部操作条背景', 1, 1500, 248, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.memberCard.background', 'memberCard', '会员卡片背景', 1, 1372, 448, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.tabBar.background', 'tabBar', '底部 TabBar 背景', 1, 1500, 224, 'STRETCH', '{"layoutLocked":true}', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    component_key = VALUES(component_key),
    display_name = VALUES(display_name),
    output_width = VALUES(output_width),
    output_height = VALUES(output_height),
    render_mode = VALUES(render_mode),
    safe_area_json = VALUES(safe_area_json),
    status = VALUES(status);

SET @skin_typography = JSON_OBJECT(
    'pageTitle', JSON_OBJECT('color','#332C28','fontSize',18,'fontWeight',700,'lineHeight',1.3),
    'sectionTitle', JSON_OBJECT('color','#332C28','fontSize',16,'fontWeight',700,'lineHeight',1.35),
    'bannerTitle', JSON_OBJECT('color','#332C28','fontSize',26,'fontWeight',800,'lineHeight',1.2),
    'bannerSubtitle', JSON_OBJECT('color','#88776A','fontSize',12,'fontWeight',500,'lineHeight',1.5),
    'actionTitle', JSON_OBJECT('color','#38271F','fontSize',15,'fontWeight',700,'lineHeight',1.3),
    'actionSubtitle', JSON_OBJECT('color','#88776A','fontSize',10,'fontWeight',500,'lineHeight',1.4),
    'productTitle', JSON_OBJECT('color','#38271F','fontSize',18,'fontWeight',700,'lineHeight',1.3),
    'price', JSON_OBJECT('color','#875629','fontSize',22,'fontWeight',800,'lineHeight',1.2),
    'metaText', JSON_OBJECT('color','#88776A','fontSize',11,'fontWeight',400,'lineHeight',1.4),
    'bodyText', JSON_OBJECT('color','#57483E','fontSize',13,'fontWeight',400,'lineHeight',1.55),
    'panelTitle', JSON_OBJECT('color','#332C28','fontSize',17,'fontWeight',700,'lineHeight',1.35),
    'optionTitle', JSON_OBJECT('color','#38271F','fontSize',14,'fontWeight',700,'lineHeight',1.35),
    'optionText', JSON_OBJECT('color','#57483E','fontSize',13,'fontWeight',500,'lineHeight',1.35),
    'buttonPrimary', JSON_OBJECT('color','#FFFFFF','fontSize',15,'fontWeight',700,'lineHeight',1.2),
    'buttonSecondary', JSON_OBJECT('color','#6F4E37','fontSize',14,'fontWeight',600,'lineHeight',1.2),
    'memberTitle', JSON_OBJECT('color','#4D301E','fontSize',16,'fontWeight',700,'lineHeight',1.3),
    'memberValue', JSON_OBJECT('color','#4D301E','fontSize',22,'fontWeight',800,'lineHeight',1.2),
    'emptyTitle', JSON_OBJECT('color','#38271F','fontSize',17,'fontWeight',700,'lineHeight',1.35),
    'emptyDescription', JSON_OBJECT('color','#88776A','fontSize',12,'fontWeight',400,'lineHeight',1.55),
    'tabText', JSON_OBJECT('color','#8A7B70','fontSize',11,'fontWeight',400,'lineHeight',1.2),
    'tabTextActive', JSON_OBJECT('color','#875629','fontSize',11,'fontWeight',700,'lineHeight',1.2)
);

SET @skin_content = JSON_OBJECT(
    'homeBanner', JSON_OBJECT(
        'visible', TRUE,
        'title', '一杯好咖啡，从这里开始',
        'subtitle', '现点现做，认真对待每一杯'
    )
);

SET @skin_assets_vintage = JSON_OBJECT(
    'homeBanner','/static/skin/vintage/home-banner.png',
    'actionCard','/static/skin/vintage/action-card.png',
    'sectionBanner','/static/skin/vintage/section-banner.png',
    'productCard','/static/skin/vintage/product-card.png',
    'specPanel','/static/skin/vintage/spec-panel.png',
    'emptyCart','/static/skin/vintage/empty-cart.png',
    'cartPanel','/static/skin/vintage/cart-panel.png',
    'checkoutBar','/static/skin/vintage/checkout-bar.png',
    'memberCard','/static/skin/vintage/member-card.png',
    'tabBar','/static/skin/vintage/tab-bar.png'
);

SET @skin_assets_empty = JSON_OBJECT(
    'homeBanner',NULL,'actionCard',NULL,'sectionBanner',NULL,'productCard',NULL,'specPanel',NULL,
    'emptyCart',NULL,'cartPanel',NULL,'checkoutBar',NULL,'memberCard',NULL,'tabBar',NULL
);

SET @skin_vintage = JSON_OBJECT(
    'schemaVersion',1,'themeVersion',1,
    'page',JSON_OBJECT('backgroundColor','#F4EBDD','textColor','#38291F','secondaryTextColor','#846B59'),
    'colors',JSON_OBJECT('primary','#6D4A2E','pageBackground','#F4EBDD','cardBackground','#FFF9EF','textPrimary','#38291F','textSecondary','#846B59'),
    'content',@skin_content,
    'assets',@skin_assets_vintage,
    'typography',@skin_typography
);

SET @skin_typography_midnight = JSON_SET(@skin_typography,
    '$.pageTitle.color','#F5F0E8','$.sectionTitle.color','#F5F0E8','$.bannerTitle.color','#F5F0E8',
    '$.bannerSubtitle.color','#B5B9A9','$.actionTitle.color','#F5F0E8','$.actionSubtitle.color','#B5B9A9',
    '$.productTitle.color','#F5F0E8','$.price.color','#E2C58D','$.metaText.color','#B5B9A9',
    '$.bodyText.color','#F5F0E8','$.panelTitle.color','#F5F0E8','$.optionTitle.color','#F5F0E8',
    '$.optionText.color','#F5F0E8','$.buttonPrimary.color','#18211D','$.buttonSecondary.color','#E2C58D',
    '$.memberTitle.color','#F5F0E8','$.memberValue.color','#F5F0E8','$.emptyTitle.color','#F5F0E8',
    '$.emptyDescription.color','#B5B9A9','$.tabText.color','#9BA89E','$.tabTextActive.color','#E2C58D'
);

SET @skin_midnight = JSON_OBJECT(
    'schemaVersion',1,'themeVersion',1,
    'page',JSON_OBJECT('backgroundColor','#18211D','textColor','#F5F0E8','secondaryTextColor','#B5B9A9'),
    'colors',JSON_OBJECT('primary','#D4B57D','pageBackground','#18211D','cardBackground','#26352D','textPrimary','#F5F0E8','textSecondary','#B5B9A9'),
    'content',@skin_content,
    'assets',@skin_assets_empty,
    'typography',@skin_typography_midnight
);

INSERT INTO system_theme_templates
    (name, schema_version, config_json, status, revision, created_at, updated_at)
VALUES
    ('复古纸张', '1', @skin_vintage, 'ACTIVE', 1, now(), now()),
    ('午夜森林', '1', @skin_midnight, 'ACTIVE', 1, now(), now())
ON DUPLICATE KEY UPDATE
    schema_version = VALUES(schema_version),
    config_json = VALUES(config_json),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
