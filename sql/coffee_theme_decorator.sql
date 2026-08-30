-- 商家小程序装修平台基础表（MySQL 5.7+）
-- 执行前请先备份数据库，并在测试环境验证。

CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_merchants_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装修平台商家';

CREATE TABLE IF NOT EXISTS merchant_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    store_scope VARCHAR(16) NOT NULL DEFAULT 'ALL',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_member_user (merchant_id, user_id),
    KEY idx_merchant_members_user_status (user_id, status),
    CONSTRAINT fk_member_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家后台成员';

CREATE TABLE IF NOT EXISTS stores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    store_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_code (store_code),
    UNIQUE KEY uk_merchant_store_name (merchant_id, name),
    KEY idx_stores_merchant_status (merchant_id, status),
    CONSTRAINT fk_store_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家门店';

CREATE TABLE IF NOT EXISTS merchant_member_stores (
    member_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (member_id, store_id),
    KEY idx_member_stores_tenant (merchant_id, store_id),
    CONSTRAINT fk_member_store_member FOREIGN KEY (member_id) REFERENCES merchant_members (id),
    CONSTRAINT fk_member_store_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_member_store_store FOREIGN KEY (store_id) REFERENCES stores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员可访问门店';

CREATE TABLE IF NOT EXISTS system_theme_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    config_json LONGTEXT NOT NULL,
    preview_asset_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    revision INT NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_name_revision (name, revision),
    KEY idx_theme_templates_status (status, revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统主题模板';

CREATE TABLE IF NOT EXISTS themes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    owner_store_id BIGINT NULL,
    source_template_id BIGINT NULL,
    cloned_from_theme_id BIGINT NULL,
    cloned_from_version_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    active_scope_key VARCHAR(96) GENERATED ALWAYS AS (
        CASE WHEN status = 'ACTIVE' THEN
            CASE WHEN scope_type = 'MERCHANT' THEN 'MERCHANT'
                 ELSE CONCAT('STORE:', owner_store_id) END
        ELSE NULL END
    ) STORED,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_theme_active_scope (merchant_id, active_scope_key),
    KEY idx_themes_tenant_scope (merchant_id, scope_type, owner_store_id, status),
    CONSTRAINT fk_theme_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_theme_store FOREIGN KEY (owner_store_id) REFERENCES stores (id),
    CONSTRAINT fk_theme_template FOREIGN KEY (source_template_id) REFERENCES system_theme_templates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家及门店主题';

CREATE TABLE IF NOT EXISTS theme_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    theme_id BIGINT NOT NULL,
    based_on_version_id BIGINT NULL,
    schema_version VARCHAR(16) NOT NULL,
    config_json LONGTEXT NOT NULL,
    revision INT NOT NULL DEFAULT 1,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_theme_draft (merchant_id, theme_id),
    KEY idx_theme_draft_base_version (based_on_version_id),
    CONSTRAINT fk_draft_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_draft_theme FOREIGN KEY (theme_id) REFERENCES themes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题工作草稿';

CREATE TABLE IF NOT EXISTS theme_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    theme_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    source_draft_revision INT NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    config_json LONGTEXT NOT NULL,
    config_hash CHAR(64) NOT NULL,
    publish_note VARCHAR(255) NULL,
    idempotency_key VARCHAR(96) NOT NULL,
    published_by BIGINT NOT NULL,
    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_theme_version (merchant_id, theme_id, version_no),
    UNIQUE KEY uk_theme_publish_idempotency (merchant_id, theme_id, idempotency_key),
    KEY idx_theme_versions_time (merchant_id, theme_id, published_at),
    CONSTRAINT fk_version_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_version_theme FOREIGN KEY (theme_id) REFERENCES themes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变主题发布版本';

CREATE TABLE IF NOT EXISTS store_theme_bindings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    mode VARCHAR(24) NOT NULL DEFAULT 'FOLLOW_MERCHANT',
    theme_id BIGINT NOT NULL,
    published_version_id BIGINT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_store_theme_binding (merchant_id, store_id),
    KEY idx_binding_version (published_version_id),
    CONSTRAINT fk_binding_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_binding_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_binding_theme FOREIGN KEY (theme_id) REFERENCES themes (id),
    CONSTRAINT fk_binding_version FOREIGN KEY (published_version_id) REFERENCES theme_versions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店当前主题绑定';

CREATE TABLE IF NOT EXISTS assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scope_type VARCHAR(16) NOT NULL,
    merchant_id BIGINT NULL,
    source_store_id BIGINT NULL,
    asset_type VARCHAR(24) NOT NULL,
    name VARCHAR(160) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    mime_type VARCHAR(80) NOT NULL,
    width INT NULL,
    height INT NULL,
    byte_size BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_storage_key (storage_key),
    KEY idx_assets_tenant_type (merchant_id, asset_type, status, audit_status),
    CONSTRAINT fk_asset_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_asset_store FOREIGN KEY (source_store_id) REFERENCES stores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题素材';

CREATE TABLE IF NOT EXISTS theme_asset_refs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    theme_id BIGINT NOT NULL,
    draft_id BIGINT NULL,
    version_id BIGINT NULL,
    asset_id BIGINT NOT NULL,
    usage_key VARCHAR(96) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_theme_asset_ref (merchant_id, theme_id, draft_id, version_id, asset_id, usage_key),
    KEY idx_asset_refs_asset (merchant_id, asset_id),
    CONSTRAINT fk_ref_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_ref_theme FOREIGN KEY (theme_id) REFERENCES themes (id),
    CONSTRAINT fk_ref_draft FOREIGN KEY (draft_id) REFERENCES theme_drafts (id),
    CONSTRAINT fk_ref_version FOREIGN KEY (version_id) REFERENCES theme_versions (id),
    CONSTRAINT fk_ref_asset FOREIGN KEY (asset_id) REFERENCES assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题素材引用快照';

CREATE TABLE IF NOT EXISTS component_background_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_key VARCHAR(96) NOT NULL,
    component_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    spec_version INT NOT NULL DEFAULT 1,
    output_width INT NOT NULL,
    output_height INT NOT NULL,
    render_mode VARCHAR(16) NOT NULL,
    safe_area_json VARCHAR(512) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_background_slot_version (slot_key, spec_version),
    KEY idx_background_slots_component (component_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统组件背景插槽注册表';

CREATE TABLE IF NOT EXISTS ai_generation_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    slot_id BIGINT NOT NULL,
    slot_spec_version INT NOT NULL,
    reference_asset_id BIGINT NULL,
    prompt_text TEXT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_task_id VARCHAR(160) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    requested_by BIGINT NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_tasks_tenant_status (merchant_id, status, created_at),
    CONSTRAINT fk_ai_task_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_ai_task_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_ai_task_slot FOREIGN KEY (slot_id) REFERENCES component_background_slots (id),
    CONSTRAINT fk_ai_task_reference FOREIGN KEY (reference_asset_id) REFERENCES assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受约束背景生成任务';

CREATE TABLE IF NOT EXISTS ai_generation_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    candidate_no INT NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'CANDIDATE',
    accepted_asset_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_result_candidate (merchant_id, task_id, candidate_no),
    CONSTRAINT fk_ai_result_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_ai_result_task FOREIGN KEY (task_id) REFERENCES ai_generation_tasks (id),
    CONSTRAINT fk_ai_result_asset FOREIGN KEY (accepted_asset_id) REFERENCES assets (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成候选图';

CREATE TABLE IF NOT EXISTS theme_preview_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    theme_id BIGINT NOT NULL,
    draft_id BIGINT NOT NULL,
    draft_revision INT NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    config_json LONGTEXT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_by BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_preview_token_hash (token_hash),
    KEY idx_preview_tenant_expiry (merchant_id, store_id, expires_at),
    CONSTRAINT fk_preview_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    CONSTRAINT fk_preview_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_preview_theme FOREIGN KEY (theme_id) REFERENCES themes (id),
    CONSTRAINT fk_preview_draft FOREIGN KEY (draft_id) REFERENCES theme_drafts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短期真机预览凭证';

INSERT INTO component_background_slots
    (slot_key, component_key, display_name, spec_version, output_width, output_height, render_mode, safe_area_json, status)
VALUES
    ('shopHeader.background', 'shopHeader', '店铺头部背景', 1, 1500, 600, 'COVER',
     '{"left":90,"top":70,"right":90,"bottom":70}', 'ACTIVE'),
    ('activityBanner.background', 'activityBanner', '活动横幅背景', 1, 1500, 600, 'COVER',
     '{"left":96,"top":60,"right":96,"bottom":60}', 'ACTIVE'),
    ('productCard.background', 'productCard', '商品卡片平铺纹理', 1, 512, 512, 'REPEAT',
     '{"left":48,"top":48,"right":48,"bottom":48}', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    component_key = VALUES(component_key),
    display_name = VALUES(display_name),
    output_width = VALUES(output_width),
    output_height = VALUES(output_height),
    render_mode = VALUES(render_mode),
    safe_area_json = VALUES(safe_area_json),
    status = VALUES(status);

INSERT INTO system_theme_templates
    (name, schema_version, config_json, status, revision, created_at, updated_at)
VALUES
    ('经典咖啡', '1.0.0',
     '{"schemaVersion":"1.0.0","tokens":{"colors":{"primary":"#6F4E37","pageBackground":"#F8F3ED","surface":"#FFFFFF","textPrimary":"#2B2118","textSecondary":"#74685E","buttonBackground":"#6F4E37","buttonText":"#FFFFFF","border":"#E8DED4"},"radius":{"card":12,"button":20,"image":8},"shadow":{"card":"soft"}},"brand":{},"components":{"shopHeader":{"variant":"centered"},"activityBanner":{"visible":true,"variant":"single"},"categoryNav":{"variant":"icon-grid"},"productCard":{"variant":"vertical"},"tabBar":{"variant":"standard"},"profileHeader":{"variant":"brand"}}}',
     'ACTIVE', 1, now(), now()),
    ('清晰钴蓝', '1.0.0',
     '{"schemaVersion":"1.0.0","tokens":{"colors":{"primary":"#155EEF","pageBackground":"#F4F7FB","surface":"#FFFFFF","textPrimary":"#111827","textSecondary":"#536174","buttonBackground":"#155EEF","buttonText":"#FFFFFF","border":"#D7DEE8"},"radius":{"card":6,"button":10,"image":4},"shadow":{"card":"medium"}},"brand":{},"components":{"shopHeader":{"variant":"compact"},"activityBanner":{"visible":true,"variant":"carousel"},"categoryNav":{"variant":"text-row"},"productCard":{"variant":"horizontal"},"tabBar":{"variant":"brand"},"profileHeader":{"variant":"minimal"}}}',
     'ACTIVE', 1, now(), now()),
    ('深林鼠尾草', '1.0.0',
     '{"schemaVersion":"1.0.0","tokens":{"colors":{"primary":"#24543B","pageBackground":"#F2F5F0","surface":"#FFFFFF","textPrimary":"#17251D","textSecondary":"#5A6B61","buttonBackground":"#24543B","buttonText":"#FFFFFF","border":"#CED9D1"},"radius":{"card":16,"button":16,"image":12},"shadow":{"card":"soft"}},"brand":{},"components":{"shopHeader":{"variant":"centered"},"activityBanner":{"visible":true,"variant":"single"},"categoryNav":{"variant":"icon-grid"},"productCard":{"variant":"vertical"},"tabBar":{"variant":"brand"},"profileHeader":{"variant":"brand"}}}',
     'ACTIVE', 1, now(), now())
ON DUPLICATE KEY UPDATE
    schema_version = VALUES(schema_version),
    config_json = VALUES(config_json),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

-- 本地体验数据：只在数据库尚无商家时创建，并将 admin 加入演示商家。
-- 生产环境可删除本段，改由正式商家入驻流程维护 merchants / stores / merchant_members。
INSERT INTO merchants (name, status, created_at, updated_at)
SELECT '咖啡商城演示', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM merchants);

SET @decorator_demo_merchant_id = (
    SELECT id FROM merchants WHERE name = '咖啡商城演示' ORDER BY id LIMIT 1
);
SET @decorator_admin_user_id = (
    SELECT user_id FROM sys_user WHERE login_name = 'admin' AND del_flag = '0' ORDER BY user_id LIMIT 1
);

INSERT INTO merchant_members
    (merchant_id, user_id, role, store_scope, status, created_at, updated_at)
SELECT @decorator_demo_merchant_id, @decorator_admin_user_id, 'OWNER', 'ALL', 'ACTIVE', now(), now()
WHERE @decorator_demo_merchant_id IS NOT NULL AND @decorator_admin_user_id IS NOT NULL
ON DUPLICATE KEY UPDATE role = 'OWNER', store_scope = 'ALL', status = 'ACTIVE', updated_at = now();

INSERT INTO stores (merchant_id, store_code, name, status, created_at, updated_at)
SELECT @decorator_demo_merchant_id, 'DEMO_CENTRAL', '咖啡商城中心店', 'ACTIVE', now(), now()
WHERE @decorator_demo_merchant_id IS NOT NULL
ON DUPLICATE KEY UPDATE merchant_id = VALUES(merchant_id), name = VALUES(name), status = 'ACTIVE', updated_at = now();

INSERT INTO stores (merchant_id, store_code, name, status, created_at, updated_at)
SELECT @decorator_demo_merchant_id, 'DEMO_RIVERSIDE', '咖啡商城滨江店', 'ACTIVE', now(), now()
WHERE @decorator_demo_merchant_id IS NOT NULL
ON DUPLICATE KEY UPDATE merchant_id = VALUES(merchant_id), name = VALUES(name), status = 'ACTIVE', updated_at = now();

INSERT INTO themes
    (merchant_id, name, scope_type, source_template_id, status, created_by, created_at, updated_at)
SELECT @decorator_demo_merchant_id, t.name, 'MERCHANT', t.id, 'ACTIVE', @decorator_admin_user_id, now(), now()
FROM system_theme_templates t
WHERE t.status = 'ACTIVE' AND @decorator_demo_merchant_id IS NOT NULL AND @decorator_admin_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM themes existing
      WHERE existing.merchant_id = @decorator_demo_merchant_id
        AND existing.scope_type = 'MERCHANT' AND existing.status = 'ACTIVE'
  )
ORDER BY t.id
LIMIT 1;

SET @decorator_demo_theme_id = (
    SELECT id FROM themes
    WHERE merchant_id = @decorator_demo_merchant_id AND scope_type = 'MERCHANT' AND status = 'ACTIVE'
    ORDER BY id LIMIT 1
);

INSERT INTO theme_drafts
    (merchant_id, theme_id, schema_version, config_json, revision, updated_by, updated_at)
SELECT @decorator_demo_merchant_id, @decorator_demo_theme_id, t.schema_version, t.config_json, 1,
       @decorator_admin_user_id, now()
FROM themes theme
INNER JOIN system_theme_templates t ON t.id = theme.source_template_id
WHERE theme.id = @decorator_demo_theme_id
ON DUPLICATE KEY UPDATE theme_id = VALUES(theme_id);

INSERT INTO store_theme_bindings
    (merchant_id, store_id, mode, theme_id, published_version_id, updated_by, updated_at)
SELECT @decorator_demo_merchant_id, s.id, 'FOLLOW_MERCHANT', @decorator_demo_theme_id, NULL,
       @decorator_admin_user_id, now()
FROM stores s
WHERE s.merchant_id = @decorator_demo_merchant_id AND @decorator_demo_theme_id IS NOT NULL
ON DUPLICATE KEY UPDATE mode = 'FOLLOW_MERCHANT', theme_id = VALUES(theme_id),
    updated_by = VALUES(updated_by), updated_at = now();

-- 商家装修入口挂载到现有“咖啡商城”目录；角色授权仍通过 sys_role_menu 管理。
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, is_refresh,
     perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
    (2090, '商家装修', 2000, 13, '/coffee/decorator/workbench', '', 'C', '0', '1',
     'coffee:decorator:view', 'fa fa-paint-brush', 'admin', NOW(), '', NULL, '商家小程序个性化装修入口')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    url = VALUES(url),
    target = VALUES(target),
    menu_type = VALUES(menu_type),
    visible = VALUES(visible),
    is_refresh = VALUES(is_refresh),
    perms = VALUES(perms),
    icon = VALUES(icon),
    remark = VALUES(remark);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 2090);
