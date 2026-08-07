DROP TABLE IF EXISTS t_cart;
DROP TABLE IF EXISTS t_user_behavior_event;
DROP TABLE IF EXISTS t_user_profile;
DROP TABLE IF EXISTS t_address;
DROP TABLE IF EXISTS t_product_image;
DROP TABLE IF EXISTS t_product;
DROP TABLE IF EXISTS t_category;
DROP TABLE IF EXISTS t_scan_cart;
DROP TABLE IF EXISTS t_scan_table_qrcode;
DROP TABLE IF EXISTS t_scan_product_spec_option;
DROP TABLE IF EXISTS t_scan_product_spec;
DROP TABLE IF EXISTS t_scan_order_item;
DROP TABLE IF EXISTS t_scan_order;
DROP TABLE IF EXISTS t_scan_product;
DROP TABLE IF EXISTS t_scan_category;
DROP TABLE IF EXISTS t_member;
DROP TABLE IF EXISTS t_wallet;
DROP TABLE IF EXISTS t_wallet_log;
DROP TABLE IF EXISTS t_payment_log;
DROP TABLE IF EXISTS t_recharge_record;
DROP TABLE IF EXISTS t_recharge_template;
DROP TABLE IF EXISTS t_order_item;
DROP TABLE IF EXISTS t_order;
DROP TABLE IF EXISTS t_marketing_activity_scope;
DROP TABLE IF EXISTS t_marketing_activity;
DROP TABLE IF EXISTS t_offline_activity_signup;
DROP TABLE IF EXISTS t_offline_activity;
DROP TABLE IF EXISTS t_wxuser;

DROP ALIAS IF EXISTS DATE_FORMAT;
CREATE ALIAS DATE_FORMAT FOR "com.ruoyi.project.coffee.support.H2MysqlFunctions.dateFormat";

CREATE TABLE t_product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255),
    origin VARCHAR(255),
    processing_method VARCHAR(255),
    roast_level VARCHAR(255),
    flavor_notes VARCHAR(500),
    description VARCHAR(1000),
    image_url VARCHAR(500),
    remark VARCHAR(500),
    price DECIMAL(10, 2),
    status INT,
    stock BIGINT,
    category_id BIGINT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL
);

CREATE TABLE t_product_image (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT DEFAULT 0,
    is_main INT DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_category (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255),
    sort_order INT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_cart (
    cart_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    product_id BIGINT,
    quantity BIGINT,
    spec VARCHAR(255),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_user_behavior_event (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    scene VARCHAR(16) NOT NULL,
    product_id BIGINT,
    category_id BIGINT,
    source_id BIGINT,
    search_keyword VARCHAR(50),
    source VARCHAR(32),
    dedup_key VARCHAR(128),
    event_time TIMESTAMP NOT NULL,
    CONSTRAINT uk_behavior_dedup_key UNIQUE (dedup_key)
);

CREATE TABLE t_address (
    address_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(32),
    province VARCHAR(64),
    city VARCHAR(64),
    district VARCHAR(64),
    detail_address VARCHAR(500),
    is_default INT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_scan_category (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255),
    icon VARCHAR(500),
    sort_order INT,
    status INT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_scan_product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    product_name VARCHAR(255),
    sub_title VARCHAR(255),
    image_url VARCHAR(500),
    video_url VARCHAR(500),
    price DECIMAL(10, 2),
    month_sales BIGINT,
    tag VARCHAR(64),
    status INT,
    sort_order INT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_scan_product_spec (
    spec_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT,
    spec_name VARCHAR(255),
    spec_type VARCHAR(32),
    required INT,
    sort_order INT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_scan_product_spec_option (
    option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spec_id BIGINT,
    product_id BIGINT,
    option_name VARCHAR(255),
    extra_price DECIMAL(10, 2),
    is_default INT,
    sort_order INT,
    create_time TIMESTAMP NULL
);

CREATE TABLE t_scan_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    openid VARCHAR(128),
    shop_id BIGINT,
    table_no VARCHAR(64),
    product_id BIGINT,
    product_name VARCHAR(255),
    product_image VARCHAR(500),
    price DECIMAL(10, 2),
    quantity INT,
    spec_text VARCHAR(255),
    spec_json VARCHAR(1000),
    selected INT,
    status INT,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    del_flag INT DEFAULT 0
);

CREATE TABLE t_scan_table_qrcode (
    table_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT,
    shop_name VARCHAR(255),
    table_no VARCHAR(64),
    scene VARCHAR(32),
    qr_url VARCHAR(500),
    status INT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_scan_order (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64),
    user_id BIGINT,
    openid VARCHAR(128),
    shop_id BIGINT,
    shop_name VARCHAR(255),
    table_no VARCHAR(64),
    scene VARCHAR(32),
    total_amount DECIMAL(10, 2),
    pay_amount DECIMAL(10, 2),
    discount_amount DECIMAL(10, 2),
    member_discount DECIMAL(10, 2),
    activity_summary VARCHAR(500),
    status INT,
    pickup_no VARCHAR(32),
    estimated_wait_minutes INT,
    pay_type VARCHAR(64),
    pay_time TIMESTAMP NULL,
    accept_time TIMESTAMP NULL,
    making_time TIMESTAMP NULL,
    call_time TIMESTAMP NULL,
    finish_time TIMESTAMP NULL,
    cancel_time TIMESTAMP NULL,
    urge_count INT,
    last_urge_time TIMESTAMP NULL,
    refund_status INT,
    refund_reason VARCHAR(200),
    refund_reject_reason VARCHAR(200),
    refund_apply_time TIMESTAMP NULL,
    refund_time TIMESTAMP NULL,
    refund_amount DECIMAL(10, 2),
    remark VARCHAR(500),
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL
);

CREATE TABLE t_scan_order_item (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    product_id BIGINT,
    product_name VARCHAR(255),
    product_image VARCHAR(500),
    spec VARCHAR(255),
    price DECIMAL(10, 2),
    quantity INT,
    total_price DECIMAL(10, 2),
    create_time TIMESTAMP NULL
);

CREATE TABLE t_member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    level INT,
    level_name VARCHAR(64),
    discount_rate DECIMAL(5, 2),
    total_spending DECIMAL(10, 2),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_wallet (
    wallet_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    balance DECIMAL(10, 2),
    total_recharge DECIMAL(10, 2),
    total_gift DECIMAL(10, 2),
    total_consumed DECIMAL(10, 2),
    frozen_amount DECIMAL(10, 2),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_wallet_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    type INT,
    amount DECIMAL(10, 2),
    balance_before DECIMAL(10, 2),
    balance_after DECIMAL(10, 2),
    related_order_no VARCHAR(64),
    remark VARCHAR(500),
    create_time TIMESTAMP NULL
);

CREATE TABLE t_payment_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_type VARCHAR(32),
    business_no VARCHAR(64),
    user_id BIGINT,
    pay_type VARCHAR(32),
    pay_channel VARCHAR(32),
    amount DECIMAL(10, 2),
    status VARCHAR(32),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_recharge_record (
    record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    recharge_no VARCHAR(64),
    pay_amount DECIMAL(10, 2),
    gift_amount DECIMAL(10, 2),
    total_amount DECIMAL(10, 2),
    template_id BIGINT,
    pay_type VARCHAR(64),
    status INT,
    create_time TIMESTAMP NULL,
    pay_time TIMESTAMP NULL
);

CREATE TABLE t_recharge_template (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_amount DECIMAL(10, 2),
    gift_amount DECIMAL(10, 2),
    total_amount DECIMAL(10, 2),
    sort_order INT,
    status INT,
    create_time TIMESTAMP NULL
);

CREATE TABLE t_order (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64),
    user_id BIGINT,
    total_amount DECIMAL(10, 2),
    pay_amount DECIMAL(10, 2),
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(32),
    receiver_address VARCHAR(500),
    status INT,
    pay_type VARCHAR(64),
    create_time TIMESTAMP NULL,
    pay_time TIMESTAMP NULL,
    ship_time TIMESTAMP NULL,
    finish_time TIMESTAMP NULL,
    cancel_time TIMESTAMP NULL,
    express_no VARCHAR(128),
    discount_amount DECIMAL(10, 2),
    freight_amount DECIMAL(10, 2),
    activity_summary VARCHAR(500),
    refund_status INT,
    refund_reason VARCHAR(200),
    refund_reject_reason VARCHAR(200),
    refund_apply_time TIMESTAMP NULL,
    refund_time TIMESTAMP NULL,
    refund_amount DECIMAL(10, 2),
    remark VARCHAR(500)
);

CREATE TABLE t_order_item (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    product_id BIGINT,
    product_name VARCHAR(255),
    product_image VARCHAR(500),
    price DECIMAL(10, 2),
    spec VARCHAR(255),
    quantity BIGINT,
    total_price DECIMAL(10, 2)
);

CREATE TABLE t_marketing_activity (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    type INT,
    target_type VARCHAR(32),
    scope_type INT,
    condition_min_amount DECIMAL(10, 2),
    condition_min_quantity BIGINT,
    condition_new_user_only INT,
    effect_mode VARCHAR(64),
    effect_value DECIMAL(10, 2),
    gift_product_id BIGINT,
    gift_quantity BIGINT,
    shipping_base_freight DECIMAL(10, 2),
    status INT,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_marketing_activity_scope (
    scope_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT,
    scope_type INT,
    scope_target_id BIGINT
);

CREATE TABLE t_wxuser (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(128) UNIQUE,
    nickname VARCHAR(128),
    avatar VARCHAR(500),
    create_time TIMESTAMP NULL
);

CREATE TABLE t_user_profile (
    user_id BIGINT PRIMARY KEY,
    order_count INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    avg_order_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    preferred_price_min DECIMAL(10, 2),
    preferred_price_max DECIMAL(10, 2),
    last_order_time TIMESTAMP NULL,
    last_active_time TIMESTAMP NULL,
    evidence_count INT NOT NULL DEFAULT 0,
    profile_status VARCHAR(16) NOT NULL DEFAULT 'EMPTY',
    profile_data CLOB,
    calculate_time TIMESTAMP NULL,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE t_offline_activity (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    cover_image VARCHAR(500),
    summary VARCHAR(500),
    content VARCHAR(2000),
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    signup_deadline TIMESTAMP NULL,
    location VARCHAR(255),
    quota INT,
    sort_order INT,
    status INT,
    create_by VARCHAR(64),
    create_time TIMESTAMP NULL,
    update_by VARCHAR(64),
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);

CREATE TABLE t_offline_activity_signup (
    signup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT,
    user_id BIGINT,
    status INT,
    signup_time TIMESTAMP NULL,
    cancel_time TIMESTAMP NULL,
    checkin_time TIMESTAMP NULL,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL,
    remark VARCHAR(500)
);
