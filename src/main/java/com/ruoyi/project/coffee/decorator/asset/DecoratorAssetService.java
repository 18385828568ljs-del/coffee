package com.ruoyi.project.coffee.decorator.asset;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.coffee.decorator.asset.domain.DecoratorAsset;
import com.ruoyi.project.coffee.decorator.asset.domain.BackgroundSlotSpec;
import com.ruoyi.project.coffee.decorator.context.DecoratorPermission;
import com.ruoyi.project.coffee.decorator.context.TenantContext;
import com.ruoyi.project.coffee.decorator.context.TenantContextService;
import com.ruoyi.project.coffee.decorator.mapper.DecoratorAssetMapper;
import com.ruoyi.project.common.storage.FileStorageService;
import com.ruoyi.project.common.storage.StoredFileInfo;

@Service
public class DecoratorAssetService
{
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ASSET_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("LOGO", "HEADER", "COMPONENT_BACKGROUND", "ART_TEXT")));
    private static final Set<String> IMAGE_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("image/jpeg", "image/png", "image/gif")));
    private static final Set<String> SKIN_COMPONENT_KEYS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("shopHeader", "homeBanner", "actionCard", "sectionBanner",
                    "aboutImage", "productCard", "specPanel", "emptyCart", "cartPanel", "checkoutBar",
                    "meProfileHeader", "memberCard", "meOrderCenter", "meAddressCard", "tabBar")));

    @Autowired
    private DecoratorAssetMapper assetMapper;

    @Autowired
    private TenantContextService contextService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private BackgroundSlotService slotService;

    public List<DecoratorAsset> list(TenantContext context, String assetType)
    {
        contextService.requirePermission(context, DecoratorPermission.ASSET_VIEW);
        String type = normalizeType(assetType, false);
        List<DecoratorAsset> result = assetMapper.selectAssets(context.getMerchantId(), type);
        return result == null ? Collections.<DecoratorAsset>emptyList() : result;
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorAsset upload(TenantContext context, String assetType, String name,
            Long storeId, MultipartFile file)
    {
        return upload(context, assetType, name, storeId, null, file);
    }

    @Transactional(rollbackFor = Exception.class)
    public DecoratorAsset upload(TenantContext context, String assetType, String name,
            Long storeId, String slotKey, MultipartFile file)
    {
        contextService.requirePermission(context, DecoratorPermission.ASSET_MANAGE);
        String type = normalizeType(assetType, true);
        if (storeId != null) contextService.requireStore(context, storeId);
        if (file == null || file.isEmpty()) throw new ServiceException("请选择图片文件");
        if (file.getSize() <= 0 || file.getSize() > MAX_IMAGE_BYTES)
        {
            throw new ServiceException("图片大小必须在 10MB 以内");
        }
        String mimeType = String.valueOf(file.getContentType()).toLowerCase();
        if (!IMAGE_TYPES.contains(mimeType)) throw new ServiceException("仅支持 JPG、PNG 和 GIF 图片");

        try
        {
            byte[] content = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0)
            {
                throw new ServiceException("图片内容无法识别");
            }
            BackgroundSlotSpec slot = null;
            if ("COMPONENT_BACKGROUND".equals(type) && slotKey != null && !slotKey.trim().isEmpty())
            {
                slot = slotService.validateUpload(slotKey.trim(), image.getWidth(), image.getHeight(), content.length);
            }
            StoredFileInfo stored = fileStorageService.upload(file);
            DecoratorAsset asset = new DecoratorAsset();
            asset.setScopeType("MERCHANT");
            asset.setMerchantId(context.getMerchantId());
            asset.setSourceStoreId(storeId);
            asset.setAssetType(type);
            asset.setName(normalizeName(name, stored.getOriginalFilename()));
            asset.setStorageKey(stored.getUrl());
            asset.setMimeType(mimeType);
            asset.setWidth(image.getWidth());
            asset.setHeight(image.getHeight());
            asset.setByteSize((long) content.length);
            asset.setChecksumSha256(sha256(content));
            asset.setAuditStatus("APPROVED");
            asset.setStatus("ACTIVE");
            asset.setSourceType("UPLOAD");
            if (slot != null)
            {
                asset.setSlotKey(slot.getComponentKey());
                asset.setSlotSpecVersion(slot.getSpecVersion());
            }
            asset.setCreatedBy(context.getUserId());
            assetMapper.insertAsset(asset);
            return assetMapper.selectAsset(context.getMerchantId(), asset.getId());
        }
        catch (IOException e)
        {
            throw new ServiceException("图片上传失败: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(TenantContext context, Long assetId)
    {
        contextService.requirePermission(context, DecoratorPermission.ASSET_MANAGE);
        if (assetId == null || assetMapper.selectAsset(context.getMerchantId(), assetId) == null)
        {
            throw new ServiceException("素材不存在或无权访问");
        }
        if (assetMapper.countPublishedReferences(context.getMerchantId(), assetId) > 0)
        {
            throw new ServiceException("素材已被发布版本使用，不能停用");
        }
        if (assetMapper.archiveAsset(context.getMerchantId(), assetId) != 1)
        {
            throw new ServiceException("素材已停用");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceDraftReferences(Long merchantId, Long themeId, Long draftId, JsonNode config)
    {
        assetMapper.deleteDraftReferences(merchantId, themeId, draftId);
        insertReferences(merchantId, themeId, draftId, null, config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceVersionReferences(Long merchantId, Long themeId, Long versionId, JsonNode config)
    {
        assetMapper.deleteVersionReferences(merchantId, themeId, versionId);
        insertReferences(merchantId, themeId, null, versionId, config);
    }

    public Map<String, String> assetUrls(Long merchantId, Long versionId)
    {
        return toUrlMap(assetMapper.selectVersionAssetUrls(merchantId, versionId));
    }

    public Map<String, String> assetUrls(Long merchantId, JsonNode config)
    {
        Map<String, Long> references = references(config);
        if (references.isEmpty()) return Collections.emptyMap();
        return toUrlMap(assetMapper.selectUsableAssetUrls(merchantId,
                new ArrayList<Long>(new LinkedHashSet<Long>(references.values()))));
    }

    public void validateReferences(Long merchantId, JsonNode config)
    {
        Map<String, Long> references = references(config);
        if (references.isEmpty()) return;
        List<DecoratorAsset> assets = assetMapper.selectUsableAssets(merchantId,
                new ArrayList<Long>(new LinkedHashSet<Long>(references.values())));
        Map<Long, DecoratorAsset> byId = new LinkedHashMap<Long, DecoratorAsset>();
        if (assets != null) for (DecoratorAsset asset : assets) byId.put(asset.getId(), asset);
        for (Map.Entry<String, Long> entry : references.entrySet())
        {
            DecoratorAsset asset = byId.get(entry.getValue());
            if (asset == null) throw new ServiceException("THEME_ASSET_INVALID: 素材不存在、未审核或无权使用");
            String expected = entry.getKey().equals("brand.logo") ? "LOGO"
                    : entry.getKey().equals("brand.header") ? "HEADER"
                    : entry.getKey().startsWith("decorations.") ? "ART_TEXT" : "COMPONENT_BACKGROUND";
            if (!expected.equals(asset.getAssetType()))
            {
                throw new ServiceException("THEME_ASSET_INVALID: " + entry.getKey() + " 的素材类型不匹配");
            }
        }
    }

    private void insertReferences(Long merchantId, Long themeId, Long draftId, Long versionId, JsonNode config)
    {
        for (Map.Entry<String, Long> entry : references(config).entrySet())
        {
            assetMapper.insertReference(merchantId, themeId, draftId, versionId,
                    entry.getValue(), entry.getKey());
        }
    }

    private Map<String, Long> references(JsonNode config)
    {
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        addReference(result, "slots.heroBanner.background", config.path("slots").path("heroBanner").get("assetId"));
        JsonNode assets = config.path("assets");
        for (String componentKey : SKIN_COMPONENT_KEYS)
        {
            JsonNode value = assets.get(componentKey);
            if ("homeBanner".equals(componentKey) && value != null && value.isArray())
            {
                for (int index = 0; index < value.size(); index++)
                {
                    addReference(result, "assets.homeBanner." + index, value.get(index));
                }
            }
            else
            {
                addReference(result, "assets." + componentKey, value);
            }
        }
        JsonNode productImages = config.path("productImages");
        java.util.Iterator<String> productIds = productImages.fieldNames();
        while (productIds.hasNext())
        {
            String productId = productIds.next();
            addReference(result, "productImages." + productId, productImages.get(productId));
        }
        JsonNode decorations = config.path("decorations");
        java.util.Iterator<String> decorationKeys = decorations.fieldNames();
        while (decorationKeys.hasNext())
        {
            String key = decorationKeys.next();
            addReference(result, "decorations." + key, decorations.path(key).get("assetId"));
        }
        addReference(result, "brand.logo", config.path("brand").get("logoAssetId"));
        addReference(result, "brand.header", config.path("brand").get("headerAssetId"));
        JsonNode components = config.path("components");
        java.util.Iterator<String> names = components.fieldNames();
        while (names.hasNext())
        {
            String name = names.next();
            addReference(result, "components." + name + ".background",
                    components.path(name).path("background").get("assetId"));
        }
        return result;
    }

    private void addReference(Map<String, Long> result, String usageKey, JsonNode value)
    {
        if (value == null || value.isNull()) return;
        if (value.isIntegralNumber() && value.canConvertToLong() && value.asLong() > 0)
        {
            result.put(usageKey, value.asLong());
            return;
        }
        if (value.isTextual() && value.asText().matches("^[1-9][0-9]*$"))
        {
            try { result.put(usageKey, Long.valueOf(value.asText())); }
            catch (NumberFormatException ignored) { }
        }
    }

    private Map<String, String> toUrlMap(List<DecoratorAsset> assets)
    {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (assets == null) return result;
        for (DecoratorAsset asset : assets)
        {
            if (asset.getId() != null && asset.getStorageKey() != null)
            {
                result.put(String.valueOf(asset.getId()), encodeLocalAssetUrl(asset.getStorageKey()));
            }
        }
        return result;
    }

    /**
     * 微信小程序对中文、空格和括号文件名的本地资源请求不稳定，返回编码后的路径。
     * COS 资源已经是完整 URL，不在这里改写。
     */
    private String encodeLocalAssetUrl(String value)
    {
        if (value == null || !value.startsWith("/")) return value;
        String[] segments = value.split("/", -1);
        StringBuilder encoded = new StringBuilder(value.length()).append('/');
        for (int index = 1; index < segments.length; index++)
        {
            if (index > 1) encoded.append('/');
            String segment = segments[index];
            if (segment.isEmpty()) continue;
            try
            {
                encoded.append(URLEncoder.encode(segment, "UTF-8").replace("+", "%20"));
            }
            catch (java.io.UnsupportedEncodingException ignored)
            {
                encoded.append(segment);
            }
        }
        return encoded.toString();
    }

    private String normalizeType(String assetType, boolean required)
    {
        String value = assetType == null ? "" : assetType.trim().toUpperCase();
        if (value.isEmpty() && !required) return null;
        if (!ASSET_TYPES.contains(value)) throw new ServiceException("不支持的素材类型");
        return value;
    }

    private String normalizeName(String name, String fallback)
    {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) value = fallback == null ? "装修素材" : fallback.trim();
        return value.length() <= 160 ? value : value.substring(0, 160);
    }

    private String sha256(byte[] content)
    {
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder result = new StringBuilder(64);
            for (byte value : hash) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
