package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.theme.domain.DecoratorTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.PublishedStoreTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.StoreThemeSummary;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeDraft;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemeVersion;
import com.ruoyi.project.coffee.decorator.theme.domain.SystemThemeTemplate;
import com.ruoyi.project.coffee.decorator.theme.domain.PreviewTheme;
import com.ruoyi.project.coffee.decorator.theme.domain.ThemePreviewSession;

public interface DecoratorThemeMapper
{
    DecoratorTheme selectTheme(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    DecoratorTheme selectMasterTheme(@Param("merchantId") Long merchantId);
    List<DecoratorTheme> selectThemes(@Param("merchantId") Long merchantId,
            @Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);
    List<DecoratorTheme> selectDraftThemes(@Param("merchantId") Long merchantId,
            @Param("storeIds") List<Long> storeIds);
    DecoratorTheme selectActiveTheme(@Param("merchantId") Long merchantId,
            @Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);
    List<SystemThemeTemplate> selectTemplates();
    SystemThemeTemplate selectTemplate(@Param("templateId") Long templateId);
    ThemeDraft selectDraft(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    ThemeDraft selectDraftForUpdate(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    int updateDraft(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("revision") Integer revision, @Param("configJson") String configJson,
            @Param("schemaVersion") String schemaVersion, @Param("userId") Long userId,
            @Param("basedOnVersionId") Long basedOnVersionId);
    int insertDraft(ThemeDraft draft);
    int insertDraftIfAbsent(ThemeDraft draft);
    int insertTheme(DecoratorTheme theme);
    int softDeleteTheme(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("userId") Long userId);
    ThemeVersion selectVersion(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("versionId") Long versionId);
    ThemeVersion selectVersionByIdempotencyKey(@Param("merchantId") Long merchantId,
            @Param("themeId") Long themeId, @Param("idempotencyKey") String idempotencyKey);
    ThemeVersion selectLatestVersion(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    List<ThemeVersion> selectVersions(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    int selectNextVersionNo(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId);
    int insertVersion(ThemeVersion version);
    List<StoreThemeSummary> selectStores(@Param("merchantId") Long merchantId,
            @Param("memberId") Long memberId, @Param("storeScope") String storeScope);
    StoreThemeSummary selectStore(@Param("merchantId") Long merchantId, @Param("storeId") Long storeId);
    int bindFollowingStores(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("versionId") Long versionId, @Param("userId") Long userId);
    int bindIndependentStore(@Param("merchantId") Long merchantId, @Param("storeId") Long storeId,
            @Param("themeId") Long themeId, @Param("versionId") Long versionId, @Param("userId") Long userId);
    int bindStoreToMaster(@Param("merchantId") Long merchantId, @Param("storeId") Long storeId,
            @Param("themeId") Long themeId, @Param("versionId") Long versionId, @Param("userId") Long userId);
    PublishedStoreTheme selectPublishedStoreTheme(@Param("storeCode") String storeCode);
    int countUsableAssets(@Param("merchantId") Long merchantId, @Param("assetIds") List<Long> assetIds);
    int insertPreviewSession(ThemePreviewSession session);
    ThemePreviewSession selectPreviewSessionByTokenHash(@Param("tokenHash") String tokenHash);
    int revokePreviewSession(@Param("merchantId") Long merchantId, @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);
    PreviewTheme selectPreviewTheme(@Param("tokenHash") String tokenHash);
}
