-- Coffee decorator V2.1 incremental migration.
-- Phase 0-5: slots, font resources, constrained AI backgrounds and art text.

INSERT INTO component_background_slots
    (slot_key, component_key, display_name, spec_version, output_width, output_height,
     render_mode, safe_area_json, status)
VALUES
    ('skin.aboutImage.background', 'aboutImage', '关于我们图片', 1, 800, 1421,
     'STRETCH', '{"layoutLocked":true}', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    component_key = VALUES(component_key),
    display_name = VALUES(display_name),
    output_width = VALUES(output_width),
    output_height = VALUES(output_height),
    render_mode = VALUES(render_mode),
    safe_area_json = VALUES(safe_area_json),
    status = VALUES(status);

CREATE TABLE IF NOT EXISTS font_resources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    font_code VARCHAR(64) NOT NULL,
    font_name VARCHAR(128) NOT NULL,
    family_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    merchant_id BIGINT NULL,
    storage_key VARCHAR(512) NOT NULL,
    preview_storage_key VARCHAR(512) NULL,
    mime_type VARCHAR(80) NOT NULL,
    font_weight INT NOT NULL DEFAULT 400,
    font_style VARCHAR(16) NOT NULL DEFAULT 'normal',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_font_code (font_code),
    KEY idx_font_scope_status (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台字体资源';

ALTER TABLE component_background_slots
    ADD COLUMN logical_width INT NULL AFTER spec_version,
    ADD COLUMN logical_height INT NULL AFTER logical_width,
    ADD COLUMN upload_min_width INT NULL AFTER render_mode,
    ADD COLUMN upload_min_height INT NULL AFTER upload_min_width,
    ADD COLUMN max_file_size BIGINT NULL AFTER upload_min_height,
    ADD COLUMN ai_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER max_file_size;

UPDATE component_background_slots SET
    logical_width = output_width DIV 2,
    logical_height = output_height DIV 2,
    upload_min_width = output_width DIV 2,
    upload_min_height = output_height DIV 2,
    max_file_size = 10485760,
    ai_enabled = CASE WHEN component_key = 'homeBanner' THEN 1 ELSE 0 END
WHERE slot_key LIKE 'skin.%';

ALTER TABLE assets
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'UPLOAD' AFTER status,
    ADD COLUMN slot_key VARCHAR(64) NULL AFTER source_type,
    ADD COLUMN slot_spec_version INT NULL AFTER slot_key,
    ADD COLUMN generation_result_id BIGINT NULL AFTER slot_spec_version,
    ADD KEY idx_assets_slot_source (merchant_id, slot_key, source_type);

ALTER TABLE ai_generation_tasks
    ADD COLUMN generation_type VARCHAR(16) NOT NULL DEFAULT 'BACKGROUND' AFTER reference_asset_id,
    ADD COLUMN slot_key VARCHAR(64) NOT NULL DEFAULT '' AFTER slot_id,
    ADD COLUMN prompt_version VARCHAR(32) NOT NULL DEFAULT 'decorator-v1' AFTER prompt_text,
    ADD COLUMN text_content VARCHAR(80) NULL AFTER prompt_version,
    ADD COLUMN style_preset VARCHAR(32) NULL AFTER text_content,
    ADD COLUMN placement_preset VARCHAR(32) NULL AFTER style_preset,
    ADD COLUMN size_preset VARCHAR(16) NULL AFTER placement_preset,
    ADD COLUMN candidate_count INT NOT NULL DEFAULT 2 AFTER size_preset,
    ADD COLUMN transparent_background TINYINT(1) NOT NULL DEFAULT 0 AFTER candidate_count;

ALTER TABLE ai_generation_results
    ADD COLUMN mime_type VARCHAR(80) NOT NULL DEFAULT 'image/png' AFTER storage_key,
    ADD COLUMN byte_size BIGINT NOT NULL DEFAULT 0 AFTER height,
    ADD COLUMN checksum_sha256 CHAR(64) NOT NULL DEFAULT '' AFTER byte_size,
    ADD COLUMN has_alpha TINYINT(1) NOT NULL DEFAULT 0 AFTER checksum_sha256,
    ADD COLUMN post_processed TINYINT(1) NOT NULL DEFAULT 0 AFTER has_alpha,
    ADD COLUMN accepted_at DATETIME NULL AFTER accepted_asset_id,
    ADD COLUMN expires_at DATETIME NULL AFTER accepted_at;
