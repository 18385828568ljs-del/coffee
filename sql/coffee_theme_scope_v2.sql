-- Theme scope migration for the merchant装修方案 model.
-- Existing installations may have a unique active-scope index from the first
-- decorator schema; multiple named themes must be allowed in one scope.
SET @has_theme_scope_unique = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'themes'
      AND index_name = 'uk_theme_active_scope'
);
SET @drop_theme_scope_unique = IF(@has_theme_scope_unique > 0,
    'ALTER TABLE themes DROP INDEX uk_theme_active_scope', 'SELECT 1');
PREPARE drop_theme_scope_unique_stmt FROM @drop_theme_scope_unique;
EXECUTE drop_theme_scope_unique_stmt;
DEALLOCATE PREPARE drop_theme_scope_unique_stmt;

SET @has_theme_scope_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'themes'
      AND index_name = 'idx_theme_active_scope'
);
SET @add_theme_scope_index = IF(@has_theme_scope_index = 0,
    'ALTER TABLE themes ADD KEY idx_theme_active_scope (merchant_id, active_scope_key)', 'SELECT 1');
PREPARE add_theme_scope_index_stmt FROM @add_theme_scope_index;
EXECUTE add_theme_scope_index_stmt;
DEALLOCATE PREPARE add_theme_scope_index_stmt;
