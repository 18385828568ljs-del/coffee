package com.ruoyi.project.coffee.member.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.member.domain.TMember;
import com.ruoyi.project.coffee.member.mapper.TMemberMapper;
import com.ruoyi.project.system.config.service.IConfigService;

/**
 * 会员服务
 *
 * 等级名称、升级阈值、折扣率全部从 sys_config 读取(coffee.member.* 系列 key),
 * 商家可在后台修改后立即生效。
 *   coffee.member.name.lvN       等级名称
 *   coffee.member.threshold.lvN  升级累计消费阈值(元),Lv1 固定为 0
 *   coffee.member.discount.lvN   折扣率(0,1]
 */
@Service
public class MemberService
{
    public static final int MAX_LEVEL = 4;

    private static final String NAME_CONFIG_KEY_PREFIX = "coffee.member.name.lv";
    private static final String THRESHOLD_CONFIG_KEY_PREFIX = "coffee.member.threshold.lv";
    private static final String DISCOUNT_CONFIG_KEY_PREFIX = "coffee.member.discount.lv";

    /** sys_config 缺失或解析失败时的兜底名称 */
    private static final String[] DEFAULT_LEVEL_NAMES = { "", "初遇会员", "常客会员", "知味会员", "臻享会员" };

    /** sys_config 缺失或解析失败时的兜底阈值(元) */
    private static final long[] DEFAULT_THRESHOLDS = { 0, 0, 200, 800, 2000 };

    /** sys_config 缺失或解析失败时的兜底折扣 */
    private static final BigDecimal[] DEFAULT_DISCOUNT_RATES = {
        BigDecimal.ONE,
        BigDecimal.ONE,
        new BigDecimal("0.98"),
        new BigDecimal("0.95"),
        new BigDecimal("0.92")
    };

    @Autowired
    private TMemberMapper memberMapper;

    @Autowired
    private IConfigService configService;

    /**
     * 获取或初始化会员
     */
    public TMember getOrCreateMember(Long userId)
    {
        TMember member = memberMapper.selectMemberByUserId(userId);
        if (member == null)
        {
            member = new TMember();
            member.setUserId(userId);
            member.setLevel(1);
            member.setLevelName(getLevelNameByLevel(1));
            member.setDiscountRate(getDiscountRateByLevel(1));
            member.setTotalSpending(BigDecimal.ZERO);
            memberMapper.insertMember(member);
        }
        return member;
    }

    /**
     * 获取会员折扣率(实时从 sys_config 读, 商家改了立即对所有同等级用户生效)
     */
    public BigDecimal getMemberDiscountRate(Long userId)
    {
        TMember member = getOrCreateMember(userId);
        int level = (member == null || member.getLevel() == null) ? 1 : member.getLevel();
        return getDiscountRateByLevel(level);
    }

    /**
     * 累加消费金额并重新计算等级(原子SQL,避免并发丢失更新)
     */
    public TMember addSpending(Long userId, BigDecimal amount)
    {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            return getOrCreateMember(userId);
        }

        // 确保会员记录存在
        TMember member = getOrCreateMember(userId);

        // 原子累加消费金额
        memberMapper.atomicAddSpending(userId, amount);

        // 重新查询最新消费总额并评估等级
        member = memberMapper.selectMemberByUserId(userId);
        if (member != null)
        {
            evaluateLevel(member);
            memberMapper.updateMember(member);
        }

        return member;
    }

    /**
     * 商家在后台修改了等级配置后,基于最新配置重新评估所有会员的等级。
     * @return 受影响的会员数
     */
    public int recalcAllMembers()
    {
        List<TMember> all = memberMapper.selectAllMembers();
        if (all == null || all.isEmpty())
        {
            return 0;
        }
        int affected = 0;
        for (TMember m : all)
        {
            if (m == null) continue;
            evaluateLevel(m);
            memberMapper.updateMember(m);
            affected++;
        }
        return affected;
    }

    /**
     * 根据当前 totalSpending 与最新配置评估等级,并把名称/折扣同步到 t_member 字段
     */
    private void evaluateLevel(TMember member)
    {
        BigDecimal spending = member.getTotalSpending() == null ? BigDecimal.ZERO : member.getTotalSpending();
        int newLevel = 1;
        for (int level = MAX_LEVEL; level >= 2; level--)
        {
            BigDecimal threshold = getThresholdByLevel(level);
            if (spending.compareTo(threshold) >= 0)
            {
                newLevel = level;
                break;
            }
        }
        member.setLevel(newLevel);
        member.setLevelName(getLevelNameByLevel(newLevel));
        member.setDiscountRate(getDiscountRateByLevel(newLevel));
    }

    /**
     * 从 sys_config 读取指定等级名称,失败时回退到默认值
     */
    public String getLevelNameByLevel(int level)
    {
        if (level < 1 || level >= DEFAULT_LEVEL_NAMES.length)
        {
            return "";
        }
        try
        {
            String value = configService.selectConfigByKey(NAME_CONFIG_KEY_PREFIX + level);
            if (StringUtils.isNotEmpty(value))
            {
                return value.trim();
            }
        }
        catch (Exception ignore)
        {
        }
        return DEFAULT_LEVEL_NAMES[level];
    }

    /**
     * 从 sys_config 读取指定等级升级阈值(元),失败时回退到默认值;Lv1 永远是 0
     */
    public BigDecimal getThresholdByLevel(int level)
    {
        if (level <= 1)
        {
            return BigDecimal.ZERO;
        }
        if (level > MAX_LEVEL)
        {
            return new BigDecimal(DEFAULT_THRESHOLDS[MAX_LEVEL]);
        }
        try
        {
            String value = configService.selectConfigByKey(THRESHOLD_CONFIG_KEY_PREFIX + level);
            if (StringUtils.isNotEmpty(value))
            {
                BigDecimal v = new BigDecimal(value.trim());
                if (v.compareTo(BigDecimal.ZERO) >= 0)
                {
                    return v;
                }
            }
        }
        catch (Exception ignore)
        {
        }
        return new BigDecimal(DEFAULT_THRESHOLDS[level]);
    }

    /**
     * 从 sys_config 读取指定等级折扣率,失败时回退到默认值
     */
    public BigDecimal getDiscountRateByLevel(int level)
    {
        if (level < 1 || level >= DEFAULT_DISCOUNT_RATES.length)
        {
            return BigDecimal.ONE;
        }
        try
        {
            String value = configService.selectConfigByKey(DISCOUNT_CONFIG_KEY_PREFIX + level);
            if (StringUtils.isNotEmpty(value))
            {
                return new BigDecimal(value.trim());
            }
        }
        catch (Exception ignore)
        {
        }
        return DEFAULT_DISCOUNT_RATES[level];
    }
}
