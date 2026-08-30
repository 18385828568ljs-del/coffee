-- V6.0 Unified AI background studio. Run after V5 visual-intent migration.

INSERT INTO component_background_slots
    (slot_key, component_key, display_name, spec_version, logical_width, logical_height,
     output_width, output_height, render_mode, upload_min_width, upload_min_height,
     max_file_size, ai_enabled, safe_area_json, status)
VALUES
    ('skin.meProfileHeader.background', 'meProfileHeader', '个人信息区域背景', 1, 686, 160, 1372, 320, 'STRETCH', 686, 160, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.meOrderCenter.background', 'meOrderCenter', '订单中心背景', 1, 686, 220, 1372, 440, 'STRETCH', 686, 220, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE'),
    ('skin.meAddressCard.background', 'meAddressCard', '地址菜单区域背景', 1, 686, 180, 1372, 360, 'STRETCH', 686, 180, 10485760, 1, '{"layoutLocked":true}', 'ACTIVE')
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

INSERT INTO component_ai_profiles
    (slot_key, profile_version, enabled, generation_modes, allowed_text_modes, default_text_mode,
     prompt_template_code, safe_area_preset, visual_density, required_fields_json, optional_fields_json,
     allowed_elements_json, forbidden_elements_json, validation_rules_json, status)
VALUES
('meProfileHeader',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','ME_PROFILE_HEADER_BACKGROUND_V1','CONTENT_SIDE','LOW','["stylePreset"]','["merchantDescription","colorTone"]','["subtle_texture","soft_gradient"]','["avatar","user_name","member_level","button","random_text"]','{}','ACTIVE'),
('meOrderCenter',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','ME_ORDER_CENTER_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["merchantDescription","colorTone"]','["subtle_texture","low_contrast"]','["order_status","order_count","tab_icon","button","random_text"]','{}','ACTIVE'),
('meAddressCard',1,1,'["BACKGROUND"]','["NO_TEXT"]','NO_TEXT','ME_ADDRESS_CARD_BACKGROUND_V1',NULL,'LOW','["stylePreset"]','["merchantDescription","colorTone"]','["subtle_texture","low_contrast"]','["address","balance","menu_icon","button","random_text"]','{}','ACTIVE')
ON DUPLICATE KEY UPDATE
    enabled = VALUES(enabled),
    generation_modes = VALUES(generation_modes),
    allowed_text_modes = VALUES(allowed_text_modes),
    default_text_mode = VALUES(default_text_mode),
    prompt_template_code = VALUES(prompt_template_code),
    forbidden_elements_json = VALUES(forbidden_elements_json),
    status = VALUES(status),
    updated_at = NOW();
