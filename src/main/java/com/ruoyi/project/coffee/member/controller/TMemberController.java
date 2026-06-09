package com.ruoyi.project.coffee.member.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.member.domain.TMember;
import com.ruoyi.project.coffee.member.mapper.TMemberMapper;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.system.config.domain.Config;
import com.ruoyi.project.system.config.service.IConfigService;

/**
 * 会员管理 后台Controller
 */
@Controller
@RequestMapping("/coffee/member")
public class TMemberController extends BaseController
{
    private static final String NAME_CONFIG_KEY_PREFIX = "coffee.member.name.lv";
    private static final String THRESHOLD_CONFIG_KEY_PREFIX = "coffee.member.threshold.lv";
    private static final String DISCOUNT_CONFIG_KEY_PREFIX = "coffee.member.discount.lv";
    private static final int MAX_LEVEL = MemberService.MAX_LEVEL;
    private static final int MAX_NAME_LENGTH = 16;

    private String prefix = "coffee/member";

    @Autowired
    private TMemberMapper memberMapper;

    @Autowired
    private IConfigService configService;

    @Autowired
    private MemberService memberService;

    @RequiresPermissions("coffee:member:view")
    @GetMapping()
    public String member(ModelMap mmap)
    {
        // 把当前 4 个等级的名称/阈值/折扣率塞进页面,模板顶部表单区直接读取
        List<LevelRow> rows = new ArrayList<LevelRow>();
        for (int level = 1; level <= MAX_LEVEL; level++)
        {
            String name = memberService.getLevelNameByLevel(level);
            BigDecimal threshold = memberService.getThresholdByLevel(level);
            BigDecimal rate = memberService.getDiscountRateByLevel(level);
            rows.add(new LevelRow(
                level,
                name,
                threshold == null ? "0" : threshold.toPlainString(),
                rate == null ? "" : rate.toPlainString()));
        }
        mmap.put("levelRows", rows);
        return prefix + "/member";
    }

    @RequiresPermissions("coffee:member:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TMember member)
    {
        startPage();
        List<TMember> list = memberMapper.selectAllMembers();
        return getDataTable(list);
    }

    /**
     * 保存等级配置(名称 + 升级阈值 + 折扣率)
     * 1. 校验阈值在 (前一级, 后一级) 区间内,保证递增不被打破
     * 2. 写入 sys_config 三条记录
     * 3. 调用 memberService.recalcAllMembers() 全表按新规则重算等级
     */
    @RequiresPermissions("coffee:member:edit")
    @PostMapping("/saveLevelConfig")
    @ResponseBody
    public AjaxResult saveLevelConfig(
        @RequestParam("level") Integer level,
        @RequestParam("levelName") String levelName,
        @RequestParam(value = "threshold", required = false) String thresholdText,
        @RequestParam("rate") String rateText)
    {
        if (level == null || level < 1 || level > MAX_LEVEL)
        {
            return AjaxResult.error("等级参数错误");
        }
        // 名称
        if (StringUtils.isEmpty(levelName))
        {
            return AjaxResult.error("等级名称不能为空");
        }
        String trimmedName = levelName.trim();
        if (trimmedName.isEmpty())
        {
            return AjaxResult.error("等级名称不能为空");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH)
        {
            return AjaxResult.error("等级名称最多 " + MAX_NAME_LENGTH + " 个字符");
        }
        // 折扣
        if (StringUtils.isEmpty(rateText))
        {
            return AjaxResult.error("折扣率不能为空");
        }
        BigDecimal rate;
        try
        {
            rate = new BigDecimal(rateText.trim());
        }
        catch (NumberFormatException e)
        {
            return AjaxResult.error("折扣率格式错误");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) > 0)
        {
            return AjaxResult.error("折扣率必须在 (0, 1] 之间");
        }
        // 阈值: Lv1 永远是 0,不允许修改
        BigDecimal threshold;
        if (level == 1)
        {
            threshold = BigDecimal.ZERO;
        }
        else
        {
            if (StringUtils.isEmpty(thresholdText))
            {
                return AjaxResult.error("升级阈值不能为空");
            }
            try
            {
                threshold = new BigDecimal(thresholdText.trim());
            }
            catch (NumberFormatException e)
            {
                return AjaxResult.error("升级阈值格式错误");
            }
            if (threshold.compareTo(BigDecimal.ZERO) <= 0)
            {
                return AjaxResult.error("升级阈值必须大于 0");
            }
            // 必须 > 前一级
            BigDecimal prevThreshold = memberService.getThresholdByLevel(level - 1);
            if (threshold.compareTo(prevThreshold) <= 0)
            {
                return AjaxResult.error("Lv" + level + " 升级阈值必须大于 Lv" + (level - 1)
                    + " 当前阈值 " + prevThreshold.toPlainString());
            }
            // 必须 < 后一级(若存在)
            if (level < MAX_LEVEL)
            {
                BigDecimal nextThreshold = memberService.getThresholdByLevel(level + 1);
                if (threshold.compareTo(nextThreshold) >= 0)
                {
                    return AjaxResult.error("Lv" + level + " 升级阈值必须小于 Lv" + (level + 1)
                        + " 当前阈值 " + nextThreshold.toPlainString());
                }
            }
        }

        // 写 sys_config: 名称 / 阈值 / 折扣
        upsertConfig(NAME_CONFIG_KEY_PREFIX + level, trimmedName,
            "Lv" + level + " 等级名称", "Lv" + level + " 等级名称");
        if (level > 1)
        {
            upsertConfig(THRESHOLD_CONFIG_KEY_PREFIX + level, threshold.toPlainString(),
                "Lv" + level + " 升级阈值", "累计消费多少元升 Lv" + level + ",单位元");
        }
        upsertConfig(DISCOUNT_CONFIG_KEY_PREFIX + level, rate.toPlainString(),
            "Lv" + level + " 折扣率", "Lv" + level + " 折扣率");

        // 全表按最新配置重算等级,顺便同步 level_name / discount_rate 快照字段
        int affected = memberService.recalcAllMembers();
        return AjaxResult.success("保存成功,已重新评估 " + affected + " 个会员");
    }

    /**
     * sys_config 没有按 key upsert 的接口,这里先查再决定 insert/update
     */
    private void upsertConfig(String key, String value, String name, String remark)
    {
        Config query = new Config();
        query.setConfigKey(key);
        List<Config> existList = configService.selectConfigList(query);
        Config existSame = null;
        if (existList != null)
        {
            for (Config c : existList)
            {
                if (key.equals(c.getConfigKey()))
                {
                    existSame = c;
                    break;
                }
            }
        }
        if (existSame == null)
        {
            Config insert = new Config();
            insert.setConfigKey(key);
            insert.setConfigValue(value);
            insert.setConfigName(name);
            insert.setConfigType("Y");
            insert.setRemark(remark);
            insert.setCreateBy(getLoginName());
            configService.insertConfig(insert);
        }
        else
        {
            existSame.setConfigValue(value);
            existSame.setUpdateBy(getLoginName());
            configService.updateConfig(existSame);
        }
    }

    /**
     * 顶部表单使用的数据载体
     */
    public static class LevelRow
    {
        private int level;
        private String levelName;
        private String threshold;
        private String rate;

        public LevelRow(int level, String levelName, String threshold, String rate)
        {
            this.level = level;
            this.levelName = levelName;
            this.threshold = threshold;
            this.rate = rate;
        }

        public int getLevel() { return level; }
        public String getLevelName() { return levelName; }
        public String getThreshold() { return threshold; }
        public String getRate() { return rate; }
    }
}
