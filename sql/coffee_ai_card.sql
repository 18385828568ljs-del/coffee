-- AI card activity: persistent generation tasks, dynamic card data and idempotent draws.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `t_card_campaign` (
  `campaign_id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(120) NOT NULL,
  `subtitle` VARCHAR(255) DEFAULT NULL,
  `cover_image` VARCHAR(500) DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft,1=published,2=ended',
  `sort_order` INT NOT NULL DEFAULT 0,
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`campaign_id`), KEY `idx_card_campaign_active` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI card campaigns';

CREATE TABLE IF NOT EXISTS `t_ai_card` (
  `card_id` BIGINT NOT NULL AUTO_INCREMENT, `campaign_id` BIGINT NOT NULL,
  `title` VARCHAR(80) NOT NULL, `english_title` VARCHAR(80) DEFAULT NULL,
  `left_product_type` VARCHAR(16) DEFAULT NULL, `left_product_id` BIGINT DEFAULT NULL,
  `left_product_name` VARCHAR(80) NOT NULL, `left_description` VARCHAR(160) DEFAULT NULL,
  `left_product_image` VARCHAR(500) DEFAULT NULL,
  `right_product_type` VARCHAR(16) DEFAULT NULL, `right_product_id` BIGINT DEFAULT NULL,
  `right_product_name` VARCHAR(80) DEFAULT NULL, `right_description` VARCHAR(160) DEFAULT NULL,
  `right_product_image` VARCHAR(500) DEFAULT NULL,
  `brand_name` VARCHAR(80) DEFAULT NULL, `logo_url` VARCHAR(500) DEFAULT NULL,
  `theme_prompt` VARCHAR(1000) NOT NULL, `template_code` VARCHAR(32) NOT NULL DEFAULT 'retro-combo',
  `palette_code` VARCHAR(32) NOT NULL DEFAULT 'candy', `artwork_url` VARCHAR(500) DEFAULT NULL,
  `final_image_url` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft,1=generating,2=ready,3=published,4=failed,5=disabled',
  `weight` INT NOT NULL DEFAULT 1, `version` INT NOT NULL DEFAULT 1, `last_error` VARCHAR(1000) DEFAULT NULL,
  `create_by` VARCHAR(64) DEFAULT '', `create_time` DATETIME DEFAULT NULL,
  `update_by` VARCHAR(64) DEFAULT '', `update_time` DATETIME DEFAULT NULL, `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`card_id`), KEY `idx_ai_card_campaign_status` (`campaign_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI generated cards';

CREATE TABLE IF NOT EXISTS `t_card_generation_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT, `task_no` VARCHAR(64) NOT NULL, `card_id` BIGINT NOT NULL,
  `status` VARCHAR(24) NOT NULL, `stage` VARCHAR(32) NOT NULL, `attempt_count` INT NOT NULL DEFAULT 0,
  `max_attempts` INT NOT NULL DEFAULT 3, `artwork_url` VARCHAR(500) DEFAULT NULL,
  `final_image_url` VARCHAR(500) DEFAULT NULL, `error_message` VARCHAR(1000) DEFAULT NULL,
  `next_retry_time` DATETIME DEFAULT NULL, `started_time` DATETIME DEFAULT NULL, `finished_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL, `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`task_id`), UNIQUE KEY `uk_card_task_no` (`task_no`),
  KEY `idx_card_task_runnable` (`status`,`next_retry_time`), KEY `idx_card_task_card` (`card_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Persistent AI card generation tasks';

CREATE TABLE IF NOT EXISTS `t_card_draw_record` (
  `draw_id` BIGINT NOT NULL AUTO_INCREMENT, `request_no` VARCHAR(64) NOT NULL,
  `campaign_id` BIGINT NOT NULL, `card_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
  `draw_time` DATETIME NOT NULL,
  PRIMARY KEY (`draw_id`), UNIQUE KEY `uk_card_draw_request` (`request_no`),
  UNIQUE KEY `uk_card_draw_campaign_user` (`campaign_id`,`user_id`), KEY `idx_card_draw_user_time` (`user_id`,`draw_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Card draw records';

INSERT IGNORE INTO `sys_menu` VALUES
(2300,'AI卡片',2000,13,'coffee/cardCampaign','','C','0','1','coffee:card:view','fa fa-picture-o','admin',NOW(),'',NULL,'AI卡片活动'),
(2301,'卡片查询',2300,1,'#','','F','0','1','coffee:card:list','#','admin',NOW(),'',NULL,''),
(2302,'卡片新增',2300,2,'#','','F','0','1','coffee:card:add','#','admin',NOW(),'',NULL,''),
(2303,'卡片修改',2300,3,'#','','F','0','1','coffee:card:edit','#','admin',NOW(),'',NULL,''),
(2304,'卡片删除',2300,4,'#','','F','0','1','coffee:card:remove','#','admin',NOW(),'',NULL,''),
(2305,'AI生成',2300,5,'#','','F','0','1','coffee:card:generate','#','admin',NOW(),'',NULL,''),
(2306,'卡片发布',2300,6,'#','','F','0','1','coffee:card:publish','#','admin',NOW(),'',NULL,'');
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1,menu_id FROM sys_menu WHERE menu_id BETWEEN 2300 AND 2399;
