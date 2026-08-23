-- V4.0 Product image AI task metadata hotfix.
-- Safe to run repeatedly after the decorator V2/V3 migrations.

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_generation_tasks'
          AND COLUMN_NAME = 'product_id'
    ),
    'SELECT 1',
    'ALTER TABLE ai_generation_tasks ADD COLUMN product_id BIGINT NULL AFTER status'
);
PREPARE decorator_stmt FROM @ddl;
EXECUTE decorator_stmt;
DEALLOCATE PREPARE decorator_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_generation_tasks'
          AND COLUMN_NAME = 'target_width'
    ),
    'SELECT 1',
    'ALTER TABLE ai_generation_tasks ADD COLUMN target_width INT NULL AFTER product_id'
);
PREPARE decorator_stmt FROM @ddl;
EXECUTE decorator_stmt;
DEALLOCATE PREPARE decorator_stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_generation_tasks'
          AND COLUMN_NAME = 'target_height'
    ),
    'SELECT 1',
    'ALTER TABLE ai_generation_tasks ADD COLUMN target_height INT NULL AFTER target_width'
);
PREPARE decorator_stmt FROM @ddl;
EXECUTE decorator_stmt;
DEALLOCATE PREPARE decorator_stmt;
