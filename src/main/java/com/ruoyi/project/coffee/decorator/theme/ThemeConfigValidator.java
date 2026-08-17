package com.ruoyi.project.coffee.decorator.theme;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ThemeConfigValidator
{
    public static final String SCHEMA_VERSION = "1.0.0";
    public static final int MAX_CONFIG_BYTES = 64 * 1024;
    public static final int MAX_HOME_BANNERS = 10;

    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern STRING_ID_PATTERN = Pattern.compile("^[1-9][0-9]*$");
    private static final Set<String> COLOR_KEYS = setOf("primary", "pageBackground", "surface",
            "textPrimary", "textSecondary", "buttonBackground", "buttonText", "border");
    private static final Set<String> RADIUS_KEYS = setOf("card", "button", "image");
    private static final Set<String> BRAND_KEYS = setOf("logoAssetId", "headerAssetId");
    public static final Set<String> SKIN_COMPONENT_KEYS = setOf("homeBanner", "actionCard",
            "sectionBanner", "aboutImage", "productCard", "specPanel", "emptyCart", "cartPanel",
            "checkoutBar", "memberCard", "tabBar");
    public static final Set<String> TYPOGRAPHY_ROLES = setOf("pageTitle", "sectionTitle",
            "bannerTitle", "bannerSubtitle", "actionTitle", "actionSubtitle", "productTitle",
            "price", "metaText", "bodyText", "panelTitle", "optionTitle", "optionText",
            "buttonPrimary", "buttonSecondary", "memberTitle", "memberValue", "emptyTitle",
            "emptyDescription", "tabText", "tabTextActive");
    private static final Set<String> SKIN_COLOR_KEYS = setOf("primary", "pageBackground",
            "cardBackground", "textPrimary", "textSecondary");
    private static final Set<String> TYPOGRAPHY_FIELDS = setOf("color", "fontSize", "fontWeight",
            "lineHeight", "letterSpacing", "fontFamily", "textShadow");
    private static final Set<String> SKIN_CONTENT_KEYS = setOf("homeBanner");
    private static final Set<String> HOME_BANNER_CONTENT_FIELDS = setOf("visible", "title", "subtitle");
    private static final Set<String> FORBIDDEN_LAYOUT_FIELDS = setOf("x", "y", "left", "right",
            "top", "bottom", "width", "height", "margin", "padding", "transform",
            "flex", "grid");
    private static final Map<String, Set<String>> VARIANTS = variants();
    private static final Map<String, SlotRule> BACKGROUND_SLOTS = backgroundSlots();

    private final ObjectMapper objectMapper;

    @Autowired
    public ThemeConfigValidator(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    public JsonNode validate(String configJson)
    {
        List<String> errors = new ArrayList<String>();
        if (configJson == null || configJson.trim().isEmpty())
        {
            errors.add("主题配置不能为空");
            throw new ThemeConfigValidationException(errors);
        }
        if (configJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONFIG_BYTES)
        {
            errors.add("主题配置不能超过 64 KB");
            throw new ThemeConfigValidationException(errors);
        }

        JsonNode root;
        try
        {
            root = objectMapper.readTree(configJson);
        }
        catch (JsonProcessingException e)
        {
            errors.add("主题配置不是合法 JSON");
            throw new ThemeConfigValidationException(errors);
        }
        if (!root.isObject())
        {
            errors.add("主题配置根节点必须是对象");
            throw new ThemeConfigValidationException(errors);
        }

        rejectForbiddenFields(root, errors);

        JsonNode skinVersion = root.get("schemaVersion");
        if (skinVersion != null && skinVersion.isIntegralNumber() && skinVersion.asInt() == 1)
        {
            for (String key : new String[] { "schemaVersion", "themeVersion", "page" })
            {
                if (!root.has(key)) errors.add("SkinConfig 缺少字段: " + key);
            }
            JsonNode themeVersion = root.get("themeVersion");
            if (themeVersion != null && (!themeVersion.isIntegralNumber() || themeVersion.asInt() < 1)) errors.add("themeVersion 必须为正整数");
            validateSkinConfigV1(root, errors);
            boolean canonical = root.has("colors") || root.has("assets") || root.has("typography");
            if (canonical)
            {
                for (String key : new String[] { "colors", "assets", "typography" })
                {
                    if (!root.has(key)) errors.add("SkinConfig 缺少字段: " + key);
                }
                validateCanonicalSkinConfig(root, errors);
            }
            if (!errors.isEmpty()) throw new ThemeConfigValidationException(errors);
            return root;
        }
        requireExactText(root, "schemaVersion", SCHEMA_VERSION, "不支持的主题配置版本", errors);
        validateTokens(root.path("tokens"), errors);
        validateBrand(root.path("brand"), errors);
        validateComponents(root.path("components"), errors);
        if (!errors.isEmpty())
        {
            throw new ThemeConfigValidationException(errors);
        }
        return root;
    }

    private void validateSkinConfigV1(JsonNode root, List<String> errors)
    {
        JsonNode page = root.get("page");
        if (page == null || !page.isObject()) errors.add("page 必须是对象");
        else
        {
            validateColor(page, "backgroundColor", "page.backgroundColor", errors);
            validateColor(page, "textColor", "page.textColor", errors);
            validateColor(page, "secondaryTextColor", "page.secondaryTextColor", errors);
            if (page.has("background")) validateBackgroundConfig(page.get("background"), "page.background", false, errors);
        }
        JsonNode slots = root.get("slots");
        if (slots == null || slots.isNull()) return;
        if (!slots.isObject()) { errors.add("slots 必须是对象"); return; }
        Set<String> allowed = setOf("heroBanner", "orderCard", "shopCard", "welcomeBanner", "aboutSection", "tabBar");
        Iterator<String> fields = slots.fieldNames();
        while (fields.hasNext())
        {
            String key = fields.next();
            if (!allowed.contains(key)) { errors.add("不支持的 Skin Slot: " + key); continue; }
            JsonNode slot = slots.get(key);
            if (!slot.isObject()) { errors.add("slots." + key + " 必须是对象"); continue; }
            if ("heroBanner".equals(key) || "welcomeBanner".equals(key)) validateBannerSlot(key, slot, errors);
            else if ("aboutSection".equals(key)) validateAboutSlot(slot, errors);
            else if ("tabBar".equals(key)) validateTabBarSlot(slot, errors);
            else validateCardSlot(key, slot, errors);
            if (slot.has("background")) validateBackgroundConfig(slot.get("background"), "slots." + key + ".background", false, errors);
        }
        for (String required : allowed) if (!slots.has(required)) errors.add("缺少必填 Slot: " + required);
    }

    private void validateCanonicalSkinConfig(JsonNode root, List<String> errors)
    {
        validateSkinColors(root.get("colors"), errors);
        validateSkinAssets(root.get("assets"), errors);
        if (root.has("productImages")) validateProductImages(root.get("productImages"), errors);
        validateTypography(root.get("typography"), errors);
        if (root.has("content")) validateSkinContent(root.get("content"), errors);
    }

    private void validateSkinContent(JsonNode content, List<String> errors)
    {
        if (content == null || !content.isObject())
        {
            errors.add("content 必须是对象");
            return;
        }
        validateExactKeys(content, SKIN_CONTENT_KEYS, "文字内容组件", errors);
        JsonNode homeBanner = content.get("homeBanner");
        if (homeBanner == null || !homeBanner.isObject())
        {
            errors.add("content.homeBanner 必须是对象");
            return;
        }
        validateExactKeys(homeBanner, HOME_BANNER_CONTENT_FIELDS, "Banner 文字字段", errors);
        JsonNode visible = homeBanner.get("visible");
        if (visible == null || !visible.isBoolean()) errors.add("content.homeBanner.visible 必须为布尔值");
        validateOptionalText(homeBanner, "title", 40, "content.homeBanner.title", errors);
        validateOptionalText(homeBanner, "subtitle", 60, "content.homeBanner.subtitle", errors);
    }

    private void validateOptionalText(JsonNode parent, String key, int maxLength,
            String path, List<String> errors)
    {
        JsonNode value = parent.get(key);
        if (value == null || !value.isTextual() || value.asText().length() > maxLength)
        {
            errors.add(path + " 必须是最多 " + maxLength + " 个字符的文本");
        }
    }

    private void validateSkinColors(JsonNode colors, List<String> errors)
    {
        if (colors == null || !colors.isObject())
        {
            errors.add("colors 必须是对象");
            return;
        }
        validateExactKeys(colors, SKIN_COLOR_KEYS, "颜色", errors);
        for (String key : SKIN_COLOR_KEYS) validateColor(colors, key, "colors." + key, errors);
    }

    private void validateSkinAssets(JsonNode assets, List<String> errors)
    {
        if (assets == null || !assets.isObject())
        {
            errors.add("assets 必须是对象");
            return;
        }
        validateExactKeys(assets, SKIN_COMPONENT_KEYS, "皮肤组件", errors);
        for (String key : SKIN_COMPONENT_KEYS)
        {
            JsonNode value = assets.get(key);
            if (value == null)
            {
                errors.add("缺少皮肤组件: " + key);
            }
            else if ("homeBanner".equals(key) && value.isArray())
            {
                if (value.size() > MAX_HOME_BANNERS)
                {
                    errors.add("assets.homeBanner 最多支持 " + MAX_HOME_BANNERS + " 张轮播图");
                }
                for (int index = 0; index < value.size(); index++)
                {
                    if (!isSkinAsset(value.get(index)) || value.get(index).isNull())
                    {
                        errors.add("assets.homeBanner[" + index + "] 必须为素材 ID 或图片 URL");
                    }
                }
            }
            else if (!isSkinAsset(value))
            {
                errors.add("assets." + key + ("homeBanner".equals(key)
                        ? " 必须为轮播素材数组、素材 ID、图片 URL 或 null"
                        : " 必须为素材 ID、图片 URL 或 null"));
            }
        }
    }

    private void validateProductImages(JsonNode productImages, List<String> errors)
    {
        if (productImages == null || !productImages.isObject())
        {
            errors.add("productImages 必须是对象");
            return;
        }
        if (productImages.size() > 200)
        {
            errors.add("productImages 最多支持 200 个商品");
        }
        Iterator<String> productIds = productImages.fieldNames();
        while (productIds.hasNext())
        {
            String productId = productIds.next();
            if (!STRING_ID_PATTERN.matcher(productId).matches())
            {
                errors.add("productImages 的商品 ID 非法: " + productId);
                continue;
            }
            try
            {
                if (Long.parseLong(productId) <= 0) errors.add("productImages 的商品 ID 必须为正整数: " + productId);
            }
            catch (NumberFormatException e)
            {
                errors.add("productImages 的商品 ID 超出范围: " + productId);
            }
            if (!isSkinAsset(productImages.get(productId)))
            {
                errors.add("productImages." + productId + " 必须为素材 ID、图片 URL 或 null");
            }
        }
    }

    private void validateTypography(JsonNode typography, List<String> errors)
    {
        if (typography == null || !typography.isObject())
        {
            errors.add("typography 必须是对象");
            return;
        }
        validateExactKeys(typography, TYPOGRAPHY_ROLES, "文字角色", errors);
        for (String role : TYPOGRAPHY_ROLES)
        {
            JsonNode token = typography.get(role);
            String path = "typography." + role;
            if (token == null || !token.isObject())
            {
                errors.add(path + " 必须是对象");
                continue;
            }
            Iterator<String> fields = token.fieldNames();
            while (fields.hasNext())
            {
                String field = fields.next();
                if (!TYPOGRAPHY_FIELDS.contains(field)) errors.add(path + " 不支持字段: " + field);
            }
            validateColor(token, "color", path + ".color", errors);
            validateNumber(token.get("fontSize"), 10, 32, path + ".fontSize", errors);
            JsonNode weight = token.get("fontWeight");
            if (weight == null || !weight.isIntegralNumber()
                    || !setOf("400", "500", "600", "700", "800").contains(weight.asText()))
            {
                errors.add(path + ".fontWeight 必须为 400/500/600/700/800");
            }
            validateNumber(token.get("lineHeight"), 1, 2, path + ".lineHeight", errors);
            if (token.has("letterSpacing"))
            {
                validateNumber(token.get("letterSpacing"), 0, 4, path + ".letterSpacing", errors);
            }
            for (String field : new String[] { "fontFamily", "textShadow" })
            {
                if (token.has(field) && !isSafeCssValue(token.get(field)))
                {
                    errors.add(path + "." + field + " 包含不安全内容");
                }
            }
        }
    }

    private void validateExactKeys(JsonNode object, Set<String> allowed, String label, List<String> errors)
    {
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext())
        {
            String field = fields.next();
            if (!allowed.contains(field)) errors.add("不支持的" + label + ": " + field);
        }
    }

    private void validateNumber(JsonNode value, double min, double max, String path, List<String> errors)
    {
        if (value == null || !value.isNumber() || value.asDouble() < min || value.asDouble() > max)
        {
            errors.add(path + " 必须在 " + min + " 到 " + max + " 之间");
        }
    }

    private boolean isSkinAsset(JsonNode value)
    {
        if (value.isNull()) return true;
        if (value.isIntegralNumber()) return value.canConvertToLong() && value.asLong() > 0;
        if (!value.isTextual()) return false;
        String text = value.asText().trim();
        if (STRING_ID_PATTERN.matcher(text).matches())
        {
            try { return Long.parseLong(text) > 0; }
            catch (NumberFormatException e) { return false; }
        }
        return (text.startsWith("/") || text.startsWith("http://") || text.startsWith("https://")
                || text.startsWith("data:image/")) && !text.matches(".*[\\s<>\\\"'].*");
    }

    private boolean isSafeCssValue(JsonNode value)
    {
        if (value == null || !value.isTextual()) return false;
        String text = value.asText();
        return text.length() <= 120 && !text.matches(".*[;{}<>].*")
                && !text.toLowerCase().contains("url(");
    }

    private void validateBannerSlot(String key, JsonNode slot, List<String> errors)
    {
        String path = "slots." + key;
        JsonNode assetId = slot.get("assetId");
        if (assetId != null && !isStringId(assetId)) errors.add(path + ".assetId 必须是字符串 ID");
        JsonNode type = slot.get("backgroundType");
        if (type == null || !type.isTextual() || !("image".equals(type.asText()) || "color".equals(type.asText()))) errors.add(path + ".backgroundType 非法");
        if ("image".equals(type == null ? "" : type.asText()))
        {
            JsonNode backgroundImage = slot.get("backgroundImage");
            boolean hasAsset = assetId != null && isStringId(assetId);
            if (!hasAsset) validateText(slot, "backgroundImage", path + ".backgroundImage", errors);
            else if (backgroundImage != null && !backgroundImage.isNull() && !backgroundImage.isTextual())
                errors.add(path + ".backgroundImage 必须是字符串");
            validateFit(slot, path, errors);
        }
        if ("color".equals(type == null ? "" : type.asText())) validateColor(slot, "backgroundColor", path + ".backgroundColor", errors);
        if (slot.has("radius")) validateRadius(slot.get("radius"), path + ".radius", errors);
    }

    private void validateCardSlot(String key, JsonNode slot, List<String> errors)
    {
        String path = "slots." + key;
        JsonNode type = slot.get("backgroundType");
        if (type == null || !type.isTextual() || !("image".equals(type.asText()) || "color".equals(type.asText()))) errors.add(path + ".backgroundType 非法");
        if ("color".equals(type == null ? "" : type.asText())) validateColor(slot, "backgroundColor", path + ".backgroundColor", errors);
        if ("image".equals(type == null ? "" : type.asText())) { validateText(slot, "backgroundImage", path + ".backgroundImage", errors); validateFit(slot, path, errors); }
        for (String field : new String[] { "iconColor", "textColor", "secondaryTextColor" }) validateColor(slot, field, path + "." + field, errors);
        if (slot.has("radius")) validateRadius(slot.get("radius"), path + ".radius", errors);
        if (slot.has("shadow") && (!slot.get("shadow").isTextual() || !setOf("none", "light", "medium").contains(slot.get("shadow").asText()))) errors.add(path + ".shadow 非法");
    }

    private void validateAboutSlot(JsonNode slot, List<String> errors)
    {
        validateColor(slot, "backgroundColor", "slots.aboutSection.backgroundColor", errors);
        validateColor(slot, "titleColor", "slots.aboutSection.titleColor", errors);
        JsonNode image = slot.get("image");
        if (image != null && !image.isTextual()) errors.add("slots.aboutSection.image 必须为字符串");
    }

    private void validateTabBarSlot(JsonNode slot, List<String> errors)
    {
        String path = "slots.tabBar";
        for (String field : new String[] { "backgroundColor", "textColor", "activeTextColor", "iconColor", "activeIconColor", "activeBackgroundColor" }) validateColor(slot, field, path + "." + field, errors);
    }

    private void validateColor(JsonNode parent, String key, String path, List<String> errors)
    {
        JsonNode value = parent.get(key);
        if (value == null || !value.isTextual() || !Pattern.compile("^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$").matcher(value.asText()).matches()) errors.add(path + " 必须为合法颜色");
    }

    private void validateBackgroundConfig(JsonNode background, String path, boolean allowImage, List<String> errors)
    {
        if (background == null || !background.isObject())
        {
            errors.add(path + " 必须是对象");
            return;
        }
        JsonNode type = background.get("type");
        if (type == null || !type.isTextual() || !("solid".equals(type.asText())
                || "gradient".equals(type.asText()) || (allowImage && "image".equals(type.asText()))))
        {
            errors.add(path + ".type 仅支持 solid、gradient" + (allowImage ? "、image" : ""));
            return;
        }
        if ("solid".equals(type.asText()))
        {
            validateColor(background, "color", path + ".color", errors);
            return;
        }
        if ("image".equals(type.asText())) return;
        JsonNode gradient = background.get("gradient");
        if (gradient == null || !gradient.isObject())
        {
            errors.add(path + ".gradient 必须是对象");
            return;
        }
        JsonNode gradientType = gradient.get("type");
        if (gradientType == null || !gradientType.isTextual() || !"linear".equals(gradientType.asText()))
        {
            errors.add(path + ".gradient.type 必须为 linear");
        }
        JsonNode direction = gradient.get("direction");
        if (direction == null || !direction.isNumber() || direction.asDouble() < 0 || direction.asDouble() > 360)
        {
            errors.add(path + ".gradient.direction 必须在 0 到 360 之间");
        }
        JsonNode colors = gradient.get("colors");
        if (colors == null || !colors.isArray() || colors.size() < 2)
        {
            errors.add(path + ".gradient.colors 至少需要两个颜色");
            return;
        }
        int previousPosition = -1;
        for (int index = 0; index < colors.size(); index++)
        {
            JsonNode stop = colors.get(index);
            String stopPath = path + ".gradient.colors[" + index + "]";
            if (stop == null || !stop.isObject())
            {
                errors.add(stopPath + " 必须是对象");
                continue;
            }
            validateColor(stop, "color", stopPath + ".color", errors);
            JsonNode position = stop.get("position");
            if (position == null || !position.isIntegralNumber() || position.asInt() < 0 || position.asInt() > 100)
            {
                errors.add(stopPath + ".position 必须在 0 到 100 之间");
            }
            else if (position.asInt() < previousPosition)
            {
                errors.add(stopPath + ".position 必须按升序排列");
            }
            else
            {
                previousPosition = position.asInt();
            }
        }
    }

    private void validateText(JsonNode parent, String key, String path, List<String> errors)
    {
        JsonNode value = parent.get(key);
        if (value == null || !value.isTextual() || value.asText().trim().isEmpty()) errors.add(path + " 不能为空");
    }

    private void validateFit(JsonNode slot, String path, List<String> errors)
    {
        JsonNode fit = slot.get("fit");
        if (fit == null || !fit.isTextual() || !("cover".equals(fit.asText()) || "contain".equals(fit.asText()))) errors.add(path + ".fit 必须为 cover 或 contain");
    }

    private void validateRadius(JsonNode value, String path, List<String> errors)
    {
        if (value == null || !value.isIntegralNumber() || !Arrays.asList(0, 8, 12, 16, 20, 24).contains(value.asInt())) errors.add(path + " 必须为 0/8/12/16/20/24");
    }

    private void validateTokens(JsonNode tokens, List<String> errors)
    {
        if (!tokens.isObject())
        {
            errors.add("tokens 必须是对象");
            return;
        }
        JsonNode colors = tokens.path("colors");
        if (!colors.isObject())
        {
            errors.add("tokens.colors 必须是对象");
        }
        else
        {
            for (String key : COLOR_KEYS)
            {
                JsonNode value = colors.get(key);
                if (value != null && (!value.isTextual() || !COLOR_PATTERN.matcher(value.asText()).matches()))
                {
                    errors.add("tokens.colors." + key + " 必须使用 #RRGGBB 格式");
                }
            }
        }

        JsonNode radius = tokens.path("radius");
        if (!radius.isObject())
        {
            errors.add("tokens.radius 必须是对象");
        }
        else
        {
            for (String key : RADIUS_KEYS)
            {
                JsonNode value = radius.get(key);
                if (value != null && (!value.isIntegralNumber() || value.asInt() < 0 || value.asInt() > 32))
                {
                    errors.add("tokens.radius." + key + " 必须是 0 到 32 的整数");
                }
            }
        }

        JsonNode shadow = tokens.path("shadow").path("card");
        if (!shadow.isMissingNode() && (!shadow.isTextual()
                || !setOf("none", "soft", "medium").contains(shadow.asText())))
        {
            errors.add("tokens.shadow.card 不在允许范围内");
        }
    }

    private void validateBrand(JsonNode brand, List<String> errors)
    {
        if (brand.isMissingNode())
        {
            return;
        }
        if (!brand.isObject())
        {
            errors.add("brand 必须是对象");
            return;
        }
        for (String key : BRAND_KEYS)
        {
            JsonNode value = brand.get(key);
            if (value != null && !isStringId(value))
            {
                errors.add("brand." + key + " 必须是字符串 ID");
            }
        }
    }

    private void validateComponents(JsonNode components, List<String> errors)
    {
        if (!components.isObject())
        {
            errors.add("components 必须是对象");
            return;
        }
        Iterator<String> names = components.fieldNames();
        while (names.hasNext())
        {
            String componentKey = names.next();
            JsonNode component = components.get(componentKey);
            Set<String> allowedVariants = VARIANTS.get(componentKey);
            if (allowedVariants == null)
            {
                errors.add("不支持的组件: " + componentKey);
                continue;
            }
            if (!component.isObject())
            {
                errors.add("components." + componentKey + " 必须是对象");
                continue;
            }
            JsonNode variant = component.get("variant");
            if (variant == null || !variant.isTextual() || !allowedVariants.contains(variant.asText()))
            {
                errors.add("components." + componentKey + ".variant 不在允许范围内");
            }
            JsonNode visible = component.get("visible");
            if (visible != null && (!"activityBanner".equals(componentKey) || !visible.isBoolean()))
            {
                errors.add("仅 activityBanner.visible 允许使用布尔值");
            }
            JsonNode background = component.get("background");
            if (background != null)
            {
                validateBackground(componentKey, background, errors);
            }
        }
    }

    private void validateBackground(String componentKey, JsonNode background, List<String> errors)
    {
        SlotRule rule = BACKGROUND_SLOTS.get(componentKey);
        if (rule == null)
        {
            errors.add("components." + componentKey + " 不支持背景插槽");
            return;
        }
        String path = "components." + componentKey + ".background";
        if (!background.isObject())
        {
            errors.add(path + " 必须是对象");
            return;
        }
        if (!isStringId(background.get("assetId")))
        {
            errors.add(path + ".assetId 必须是字符串 ID");
        }
        requireExactText(background, "slotKey", rule.slotKey, path + ".slotKey 与组件不匹配", errors);
        requireExactText(background, "renderMode", rule.renderMode, path + ".renderMode 与插槽不匹配", errors);
        JsonNode specVersion = background.get("slotSpecVersion");
        if (specVersion == null || !specVersion.isIntegralNumber() || specVersion.asInt() != 1)
        {
            errors.add(path + ".slotSpecVersion 必须为 1");
        }
        JsonNode opacity = background.get("opacity");
        if (opacity == null || !opacity.isNumber() || opacity.asDouble() < 0 || opacity.asDouble() > 1)
        {
            errors.add(path + ".opacity 必须在 0 到 1 之间");
        }
    }

    private void rejectForbiddenFields(JsonNode root, List<String> errors)
    {
        rejectForbiddenFields(root, "", errors);
    }

    private void rejectForbiddenFields(JsonNode node, String path, List<String> errors)
    {
        if (node == null) return;
        if (node.isArray())
        {
            for (int index = 0; index < node.size(); index++)
            {
                rejectForbiddenFields(node.get(index), path + "[" + index + "]", errors);
            }
            return;
        }
        if (!node.isObject()) return;
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext())
        {
            String key = fields.next();
            String fieldPath = path.isEmpty() ? key : path + "." + key;
            if (setOf("merchantId", "storeId", "css", "wxml", "javascript", "script").contains(key))
            {
                errors.add("ThemeConfig 不允许包含 " + key);
            }
            if (FORBIDDEN_LAYOUT_FIELDS.contains(key))
            {
                errors.add("SkinConfig 不允许编辑布局字段: " + fieldPath);
            }
            rejectForbiddenFields(node.get(key), fieldPath, errors);
        }
    }

    private static void requireExactText(JsonNode parent, String key, String expected,
            String message, List<String> errors)
    {
        JsonNode value = parent.get(key);
        if (value == null || !value.isTextual() || !expected.equals(value.asText()))
        {
            errors.add(message);
        }
    }

    private static boolean isStringId(JsonNode value)
    {
        if (value == null || !value.isTextual() || !STRING_ID_PATTERN.matcher(value.asText()).matches())
        {
            return false;
        }
        try
        {
            return Long.parseLong(value.asText()) > 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private static Map<String, Set<String>> variants()
    {
        Map<String, Set<String>> values = new LinkedHashMap<String, Set<String>>();
        values.put("shopHeader", setOf("centered", "compact"));
        values.put("activityBanner", setOf("single", "carousel"));
        values.put("categoryNav", setOf("icon-grid", "text-row"));
        values.put("productCard", setOf("vertical", "horizontal"));
        values.put("tabBar", setOf("standard", "brand"));
        values.put("profileHeader", setOf("brand", "minimal"));
        return Collections.unmodifiableMap(values);
    }

    private static Map<String, SlotRule> backgroundSlots()
    {
        Map<String, SlotRule> values = new LinkedHashMap<String, SlotRule>();
        values.put("shopHeader", new SlotRule("shopHeader.background", "cover"));
        values.put("activityBanner", new SlotRule("activityBanner.background", "cover"));
        values.put("productCard", new SlotRule("productCard.background", "repeat"));
        return Collections.unmodifiableMap(values);
    }

    private static Set<String> setOf(String... values)
    {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }

    private static final class SlotRule
    {
        private final String slotKey;
        private final String renderMode;

        private SlotRule(String slotKey, String renderMode)
        {
            this.slotKey = slotKey;
            this.renderMode = renderMode;
        }
    }
}
