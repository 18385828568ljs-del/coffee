-- V3.0 Component AI Profile registry. Run after coffee_skin_v1.sql and V2.1 migration.
CREATE TABLE IF NOT EXISTS component_ai_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_key VARCHAR(64) NOT NULL,
    profile_version INT NOT NULL DEFAULT 1,
    enabled TINYINT NOT NULL DEFAULT 1,
    generation_modes VARCHAR(255) NOT NULL,
    allowed_text_modes VARCHAR(255) NOT NULL,
    default_text_mode VARCHAR(32) NOT NULL DEFAULT 'NO_TEXT',
    prompt_template_code VARCHAR(64) NOT NULL,
    safe_area_preset VARCHAR(64) NULL,
    visual_density VARCHAR(32) NOT NULL DEFAULT 'LOW',
    required_fields_json TEXT NULL,
    optional_fields_json TEXT NULL,
    allowed_elements_json TEXT NULL,
    forbidden_elements_json TEXT NULL,
    validation_rules_json TEXT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot_profile_version (slot_key, profile_version),
    KEY idx_slot_status (slot_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装修组件 AI 生成语义规格';

-- Keep the physical slot registry in sync with the AI profile registry. This
-- upsert also repairs installations that ran V2/V3 without coffee_skin_v1.sql.
INSERT INTO component_background_slots
    (slot_key, component_key, display_name, spec_version, logical_width, logical_height,
     output_width, output_height, render_mode, upload_min_width, upload_min_height,
     max_file_size, ai_enabled, safe_area_json, status)
VALUES
    ('skin.shopHeader.background', 'shopHeader', '店铺头部背景', 1, 750, 128, 1500, 256, 'STRETCH', 750, 128, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.homeBanner.background', 'homeBanner', '头部 Banner 背景', 1, 750, 360, 1500, 720, 'COVER', 750, 360, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.actionCard.background', 'actionCard', '功能按钮卡片背景', 1, 335, 180, 670, 360, 'STRETCH', 335, 180, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.sectionBanner.background', 'sectionBanner', '欢迎牌 / 分区横幅背景', 1, 686, 144, 1372, 288, 'STRETCH', 686, 144, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.aboutImage.background', 'aboutImage', '关于我们图片', 1, 686, 1000, 800, 1421, 'STRETCH', 686, 1000, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.productCard.background', 'productCard', '商品卡片背景', 1, 686, 320, 1372, 640, 'STRETCH', 686, 320, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.specPanel.background', 'specPanel', '商品规格弹窗背景', 1, 686, 720, 1372, 1440, 'STRETCH', 686, 720, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.emptyCart.background', 'emptyCart', '空购物车面板背景', 1, 686, 560, 1372, 1120, 'STRETCH', 686, 560, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.cartPanel.background', 'cartPanel', '购物车结算面板背景', 1, 686, 180, 1372, 360, 'STRETCH', 686, 180, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.checkoutBar.background', 'checkoutBar', '确认订单底部操作条背景', 1, 750, 124, 1500, 248, 'STRETCH', 750, 124, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.meProfileHeader.background', 'meProfileHeader', '个人信息区域背景', 1, 686, 160, 1372, 320, 'STRETCH', 686, 160, 10485760, 0, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.memberCard.background', 'memberCard', '会员卡片背景', 1, 686, 224, 1372, 448, 'STRETCH', 686, 224, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.meOrderCenter.background', 'meOrderCenter', '订单中心背景', 1, 686, 220, 1372, 440, 'STRETCH', 686, 220, 10485760, 0, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.meAddressCard.background', 'meAddressCard', '地址菜单区域背景', 1, 686, 180, 1372, 360, 'STRETCH', 686, 180, 10485760, 0, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.tabBar.background', 'tabBar', '底部 TabBar 背景', 1, 750, 112, 1500, 224, 'STRETCH', 750, 112, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    component_key = VALUES(component_key),
    display_name = VALUES(display_name),
    logical_width = VALUES(logical_width),
    logical_height = VALUES(logical_height),
    output_width = VALUES(output_width),
    output_height = VALUES(output_height),
    render_mode = VALUES(render_mode),
    upload_min_width = VALUES(upload_min_width),
    upload_min_height = VALUES(upload_min_height),
    max_file_size = VALUES(max_file_size),
    ai_enabled = VALUES(ai_enabled),
    safe_area_json = VALUES(safe_area_json),
    status = VALUES(status);

-- The built-in about-us.jpg is 800x1421. Keep generated candidates at the
-- same canvas ratio so the widthFix image does not become a letterboxed card.
UPDATE component_background_slots
SET output_width = 800,
    output_height = 1421,
    render_mode = 'STRETCH'
WHERE component_key = 'aboutImage'
  AND slot_key = 'skin.aboutImage.background'
  AND status = 'ACTIVE';

INSERT INTO component_ai_profiles
    (slot_key, profile_version, enabled, generation_modes, allowed_text_modes, default_text_mode,
     prompt_template_code, safe_area_preset, visual_density, required_fields_json, optional_fields_json,
     allowed_elements_json, forbidden_elements_json, validation_rules_json, status)
VALUES
('shopHeader',1,1,'["BACKGROUND","ART_TEXT","BACKGROUND_WITH_TEXT"]','["NO_TEXT","ART_TEXT_LAYER","EMBEDDED_TEXT"]','NO_TEXT','SHOP_HEADER_V1','CONTENT_SIDE','LOW','["stylePreset"]','["referenceAssetId","merchantDescription","mainElements","colorTone","textMode"]','["brand_mark","coffee_beans","paper_texture"]','["fake_button","price","qr_code","navigation","random_text"]','{}','ACTIVE'),
('homeBanner',1,1,'["BACKGROUND","ART_TEXT","BACKGROUND_WITH_TEXT"]','["NO_TEXT","ART_TEXT_LAYER","EMBEDDED_TEXT"]','NO_TEXT','HOME_BANNER_V3','CONTENT_SIDE','MEDIUM','["stylePreset"]','["referenceAssetId","merchantDescription","mainElements","colorTone","textMode"]','["coffee_beans","plants","paper_texture"]','["fake_button","price","qr_code","navigation","random_text"]','{}','ACTIVE'),
('actionCard',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','ACTION_CARD_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","merchantDescription","colorTone"]','["texture","abstract_shapes"]','["button","label","price","qr_code","random_text"]','{}','ACTIVE'),
('sectionBanner',1,1,'["BACKGROUND","ART_TEXT","BACKGROUND_WITH_TEXT"]','["NO_TEXT","ART_TEXT_LAYER","EMBEDDED_TEXT"]','NO_TEXT','SECTION_BANNER_V3','CONTENT_CENTER','LOW','["stylePreset"]','["referenceAssetId","merchantDescription","mainElements","colorTone","textMode"]','["coffee_beans","paper_texture","botanical"]','["price","button","qr_code","random_text"]','{}','ACTIVE'),
('aboutImage',1,1,'["BACKGROUND","ART_TEXT","BACKGROUND_WITH_TEXT"]','["NO_TEXT","ART_TEXT_LAYER","EMBEDDED_TEXT"]','NO_TEXT','ABOUT_IMAGE_V3','CONTENT_CENTER','MEDIUM','["stylePreset"]','["referenceAssetId","merchantDescription","mainElements","colorTone","textMode"]','["brand_story","coffee_origin","botanical"]','["price","button","qr_code","random_text"]','{}','ACTIVE'),
('productCard',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','PRODUCT_CARD_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","merchantDescription","colorTone","texturePreference"]','["subtle_texture","low_contrast"]','["product_name","price","button","promotion_text","qr_code","logo","random_text"]','{}','ACTIVE'),
('specPanel',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','SPEC_PANEL_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","colorTone"]','["subtle_texture","soft_gradient"]','["product_name","price","button","option_text","random_text"]','{}','ACTIVE'),
('emptyCart',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','EMPTY_CART_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","merchantDescription","colorTone"]','["simple_illustration","subtle_texture"]','["empty_state_text","button","price","random_text"]','{}','ACTIVE'),
('cartPanel',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','CART_PANEL_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","colorTone"]','["subtle_texture","low_contrast"]','["price","button","order_status","random_text"]','{}','ACTIVE'),
('checkoutBar',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','CHECKOUT_BAR_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","colorTone"]','["subtle_texture","low_contrast"]','["price","button","order_status","random_text"]','{}','ACTIVE'),
('memberCard',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','MEMBER_CARD_BACKGROUND_V1','CONTENT_SIDE','LOW','["stylePreset"]','["referenceAssetId","merchantDescription","colorTone"]','["subtle_texture","badge_shape"]','["member_level","balance","points","button","random_text"]','{}','ACTIVE'),
('tabBar',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','TAB_BAR_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["referenceAssetId","colorTone"]','["subtle_texture","low_contrast"]','["tab_label","tab_icon","button","random_text"]','{}','ACTIVE')
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), generation_modes=VALUES(generation_modes), allowed_text_modes=VALUES(allowed_text_modes), default_text_mode=VALUES(default_text_mode), prompt_template_code=VALUES(prompt_template_code), forbidden_elements_json=VALUES(forbidden_elements_json), status=VALUES(status), updated_at=now();

UPDATE component_background_slots SET ai_enabled=1 WHERE component_key IN
('shopHeader','homeBanner','actionCard','sectionBanner','aboutImage','productCard','specPanel','emptyCart','cartPanel','checkoutBar','memberCard','tabBar');

ALTER TABLE ai_generation_tasks
    MODIFY COLUMN prompt_text TEXT NULL,
    MODIFY COLUMN generation_type VARCHAR(32) NOT NULL DEFAULT 'BACKGROUND',
    ADD COLUMN ai_profile_version INT NULL AFTER slot_spec_version,
    ADD COLUMN text_mode VARCHAR(32) NULL AFTER text_content;

ALTER TABLE ai_generation_tasks
    ADD COLUMN product_id BIGINT NULL AFTER status,
    ADD COLUMN target_width INT NULL AFTER product_id,
    ADD COLUMN target_height INT NULL AFTER target_width;

ALTER TABLE ai_generation_results
    ADD COLUMN source_width INT NULL AFTER height,
    ADD COLUMN source_height INT NULL AFTER source_width,
    ADD COLUMN final_width INT NULL AFTER source_height,
    ADD COLUMN final_height INT NULL AFTER final_width,
    ADD COLUMN text_validation_status VARCHAR(16) NOT NULL DEFAULT 'NOT_CHECKED' AFTER post_processed,
    ADD COLUMN text_validation_detail VARCHAR(512) NULL AFTER text_validation_status;
