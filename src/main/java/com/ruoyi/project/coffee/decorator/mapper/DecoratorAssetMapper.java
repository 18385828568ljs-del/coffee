package com.ruoyi.project.coffee.decorator.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.decorator.asset.domain.DecoratorAsset;

public interface DecoratorAssetMapper
{
    List<DecoratorAsset> selectAssets(@Param("merchantId") Long merchantId,
            @Param("assetType") String assetType);
    DecoratorAsset selectAsset(@Param("merchantId") Long merchantId, @Param("assetId") Long assetId);
    int insertAsset(DecoratorAsset asset);
    int archiveAsset(@Param("merchantId") Long merchantId, @Param("assetId") Long assetId);
    int countPublishedReferences(@Param("merchantId") Long merchantId, @Param("assetId") Long assetId);
    int deleteDraftReferences(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("draftId") Long draftId);
    int deleteVersionReferences(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("versionId") Long versionId);
    int insertReference(@Param("merchantId") Long merchantId, @Param("themeId") Long themeId,
            @Param("draftId") Long draftId, @Param("versionId") Long versionId,
            @Param("assetId") Long assetId, @Param("usageKey") String usageKey);
    List<DecoratorAsset> selectVersionAssetUrls(@Param("merchantId") Long merchantId,
            @Param("versionId") Long versionId);
    List<DecoratorAsset> selectUsableAssetUrls(@Param("merchantId") Long merchantId,
            @Param("assetIds") List<Long> assetIds);
    List<DecoratorAsset> selectUsableAssets(@Param("merchantId") Long merchantId,
            @Param("assetIds") List<Long> assetIds);
}
