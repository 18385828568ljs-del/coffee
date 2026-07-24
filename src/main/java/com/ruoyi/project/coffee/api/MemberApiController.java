package com.ruoyi.project.coffee.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.member.domain.TMember;
import com.ruoyi.project.coffee.member.service.MemberService;

/**
 * 小程序会员接口
 */
@RestController
@RequestMapping("/api/member")
public class MemberApiController extends BaseController
{
    @Autowired
    private MemberService memberService;

    /**
     * 获取当前用户会员信息
     */
    @GetMapping("/info")
    public AjaxResult info()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("未登录");
        }
        TMember member = memberService.getOrCreateMember(userId);
        return AjaxResult.success(member);
    }

    /**
     * 获取等级配置(前端用于进度条/折扣展示)
     * 名称、阈值、折扣实时从 sys_config 读取,商家在后台改动后立即同步给小程序;
     * icon 取名称首字,改名后图标自动跟随。
     */
    @GetMapping("/level-config")
    public AjaxResult levelConfig()
    {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int level = 1; level <= MemberService.MAX_LEVEL; level++)
        {
            String name = memberService.getLevelNameByLevel(level);
            BigDecimal threshold = memberService.getThresholdByLevel(level);
            BigDecimal discount = memberService.getDiscountRateByLevel(level);

            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("level", level);
            map.put("levelName", name);
            map.put("icon", firstChar(name));
            map.put("threshold", threshold);
            map.put("discountRate", discount);
            list.add(map);
        }
        return AjaxResult.success(list);
    }

    /**
     * 取字符串首个 Unicode 字符,兼容 emoji / 多字节;名称为空时返回空串
     */
    private String firstChar(String text)
    {
        if (text == null || text.isEmpty())
        {
            return "";
        }
        int codePoint = text.codePointAt(0);
        return new String(Character.toChars(codePoint));
    }
}
