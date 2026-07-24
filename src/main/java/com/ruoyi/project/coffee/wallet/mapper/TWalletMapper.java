package com.ruoyi.project.coffee.wallet.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.coffee.wallet.domain.TWallet;
import com.ruoyi.project.coffee.wallet.domain.TWalletLog;
import com.ruoyi.project.coffee.wallet.domain.TRechargeRecord;
import com.ruoyi.project.coffee.wallet.domain.TRechargeTemplate;

/**
 * 钱包 Mapper
 */
public interface TWalletMapper
{
    // ===== 钱包 =====
    TWallet selectWalletByUserId(Long userId);

    int insertWallet(TWallet wallet);

    int updateWallet(TWallet wallet);

    /**
     * 增加余额（原子操作，带乐观条件检查）
     */
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣减余额（原子操作，余额不足时影响行数为0）
     */
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 消费余额（扣减余额并增加消费，余额不足时影响行数为0）
     */
    int consumeBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 增加余额（用于退款）
     */
    int increaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // ===== 流水 =====
    int insertWalletLog(TWalletLog log);

    List<TWalletLog> selectWalletLogList(@Param("userId") Long userId);

    // ===== 充值记录 =====
    List<TRechargeRecord> selectRechargeRecordList(@Param("userId") Long userId);

    // ===== 充值模板 =====
    List<TRechargeTemplate> selectActiveTemplates();

    List<TRechargeTemplate> selectAllTemplates();

    TRechargeTemplate selectTemplateById(Long templateId);

    int insertTemplate(TRechargeTemplate template);

    int updateTemplate(TRechargeTemplate template);

    int deleteTemplateByIds(String[] ids);

    // ===== 后台查询（不限 userId） =====
    List<TRechargeRecord> selectAllRechargeRecords();

    List<TWalletLog> selectAllWalletLogs();
}
