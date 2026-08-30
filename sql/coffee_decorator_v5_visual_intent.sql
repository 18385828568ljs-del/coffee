-- V5.0 Module-background sketch metadata.
-- Safe to run repeatedly after the decorator V2/V3/V4 migrations.

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_generation_tasks'
          AND COLUMN_NAME = 'visual_intent_json'
    ),
    'SELECT 1',
    'ALTER TABLE ai_generation_tasks ADD COLUMN visual_intent_json MEDIUMTEXT NULL AFTER target_height'
);
PREPARE decorator_stmt FROM @ddl;
EXECUTE decorator_stmt;
DEALLOCATE PREPARE decorator_stmt;
