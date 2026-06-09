package com.ruoyi.project.coffee.scanOrder.task;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;

/**
 * 扫码点单订单超时自动取消定时任务
 *
 * 由 RuoYi quartz 调度,在 sys_job 中以 invoke_target = scanOrderTimeoutTask.cancelTimeoutOrders 注册。
 * 点单无库存概念,仅做 status: 0 → 3 状态流转,依赖 ScanOrderMapper 的乐观锁保证幂等。
 */
@Component("scanOrderTimeoutTask")
public class ScanOrderTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(ScanOrderTimeoutTask.class);

    private static final int ORDER_TIMEOUT_MINUTES = 30;

    @Autowired
    private IScanOrderService scanOrderService;

    public void cancelTimeoutOrders()
    {
        log.info("开始执行扫码点单订单超时取消任务");
        try
        {
            ScanOrder query = new ScanOrder();
            query.setStatus(0);
            List<ScanOrder> pendingOrders = scanOrderService.selectScanOrderList(query);

            if (pendingOrders == null || pendingOrders.isEmpty())
            {
                log.info("没有待支付扫码订单需要处理");
                return;
            }

            Date now = DateUtils.getNowDate();
            int canceledCount = 0;
            int failedCount = 0;

            for (ScanOrder order : pendingOrders)
            {
                if (order.getCreateTime() == null)
                {
                    continue;
                }

                long diffMinutes = (now.getTime() - order.getCreateTime().getTime()) / (60 * 1000);
                if (diffMinutes < ORDER_TIMEOUT_MINUTES)
                {
                    continue;
                }

                try
                {
                    int affected = scanOrderService.cancelOrder(order.getOrderId());
                    if (affected > 0)
                    {
                        canceledCount++;
                        log.info("扫码订单 {} 超时自动取消成功", order.getOrderNo());
                    }
                }
                catch (Exception e)
                {
                    failedCount++;
                    log.error("扫码订单 {} 超时自动取消失败: {}", order.getOrderNo(), e.getMessage());
                }
            }

            log.info("扫码点单超时取消任务完成,成功 {} 个,失败 {} 个", canceledCount, failedCount);
        }
        catch (Exception e)
        {
            log.error("扫码点单超时取消任务执行异常", e);
        }
    }
}
