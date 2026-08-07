package com.ruoyi.project.coffee.profile.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.profile.domain.ProfilePreference;
import com.ruoyi.project.coffee.profile.domain.UserProfile;
import com.ruoyi.project.coffee.profile.domain.UserProfileView;
import com.ruoyi.project.coffee.profile.mapper.UserProfileMapper;

/** Builds the read-only profile payload used by the merchant user list. */
@Service
public class UserProfileViewService
{
    private static final String SCENE_MALL = "MALL";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private ITProductService productService;

    @Autowired
    private ProductRecommendationService recommendationService;

    public UserProfileView getProfileView(Long userId)
    {
        UserProfileView view = new UserProfileView();
        view.setUserId(userId);
        if (userId == null)
        {
            view.setProfileStatus("EMPTY");
            return view;
        }

        UserProfile profile = userProfileMapper.selectUserProfileByUserId(userId);
        if (profile == null)
        {
            view.setProfileStatus("EMPTY");
            return view;
        }
        copySummary(profile, view);
        readMallPreferences(profile.getProfileData(), view);

        if (!"READY".equalsIgnoreCase(profile.getProfileStatus()))
        {
            return view;
        }

        TProduct query = new TProduct();
        query.setStatus(1);
        List<TProduct> products = productService.selectTProductList(query);
        view.setRecommendations(recommendationService.explainMall(profile, products, 5));
        return view;
    }

    private void copySummary(UserProfile profile, UserProfileView view)
    {
        view.setUserId(profile.getUserId());
        view.setOrderCount(profile.getOrderCount());
        view.setTotalAmount(profile.getTotalAmount());
        view.setAvgOrderAmount(profile.getAvgOrderAmount());
        view.setPreferredPriceMin(profile.getPreferredPriceMin());
        view.setPreferredPriceMax(profile.getPreferredPriceMax());
        view.setLastOrderTime(profile.getLastOrderTime());
        view.setLastActiveTime(profile.getLastActiveTime());
        view.setEvidenceCount(profile.getEvidenceCount());
        view.setProfileStatus(profile.getProfileStatus());
        view.setCalculateTime(profile.getCalculateTime());
    }

    private void readMallPreferences(String profileData, UserProfileView view)
    {
        if (profileData == null || profileData.trim().isEmpty())
        {
            return;
        }
        try
        {
            JSONObject root = JSON.parseObject(profileData);
            JSONObject mall = root == null ? null : root.getJSONObject(SCENE_MALL);
            if (mall == null)
            {
                return;
            }
            view.setMallCategories(readPreferences(mall.getJSONArray("categories"), 3));
            view.setMallProducts(readPreferences(mall.getJSONArray("products"), 3));
        }
        catch (RuntimeException ignored)
        {
            // A malformed snapshot should not prevent the merchant from seeing summary fields.
        }
    }

    private List<ProfilePreference> readPreferences(JSONArray values, int limit)
    {
        if (values == null || limit <= 0)
        {
            return Collections.emptyList();
        }
        List<ProfilePreference> result = new ArrayList<>();
        for (int i = 0; i < values.size() && i < limit; i++)
        {
            JSONObject value = values.getJSONObject(i);
            if (value == null || value.getLong("id") == null)
            {
                continue;
            }
            ProfilePreference item = new ProfilePreference();
            item.setId(value.getLong("id"));
            item.setName(value.getString("name"));
            item.setScore(value.getDouble("score"));
            item.setEvidenceCount(value.getInteger("evidenceCount"));
            item.setLastEvidenceTime(parseDate(value.getString("lastEvidenceTime")));
            result.add(item);
        }
        return result;
    }

    private java.util.Date parseDate(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        try
        {
            return new SimpleDateFormat(DATE_PATTERN).parse(value);
        }
        catch (ParseException ignored)
        {
            return null;
        }
    }
}
