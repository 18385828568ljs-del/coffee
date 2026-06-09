package com.ruoyi.project.coffee.order.task;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.order.util.OrderUtils;
import com.ruoyi.project.coffee.product.service.ITProductService;

/**
 * 订单超时自动取消定时任务
 */
@Component("orderTimeoutTask")
public class OrderTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private static final int ORDER_TIMEOUT_MINUTES = 30;

    @Autowired
    private ITOrderService orderService;

    @Autowired
    private ITOrderItemService orderItemService;

    @Autowired
    private ITProductService productService;

    /**
     * 取消超时未支付订单
     */
    public void cancelTimeoutOrders()
    {
        log.info("开始执行订单超时取消任务");
        try
        {
            TOrder query = new TOrder();
            query.setStatus(0);
            List<TOrder> pendingOrders = orderService.selectTOrderList(query);

            if (pendingOrders == null || pendingOrders.isEmpty())
            {
                log.info("没有待支付订单需要处理");
                return;
            }

            Date now = DateUtils.getNowDate();
            int canceledCount = 0;
            int failedCount = 0;

            for (TOrder order : pendingOrders)
            {
                if (order.getCreateTime() == null)
                {
                    continue;
                }

                long diffMinutes = (now.getTime() - order.getCreateTime().getTime()) / (60 * 1000);
                if (diffMinutes >= ORDER_TIMEOUT_MINUTES)
                {
                    try
                    {
                        cancelOrder(order);
                        canceledCount++;
                        log.info("订单 {} 超时自动取消成功", order.getOrderNo());
                    }
                    catch (Exception e)
                    {
                        failedCount++;
                        log.error("订单 {} 超时自动取消失败: {}", order.getOrderNo(), e.getMessage());
                    }
                }
            }

            log.info("订单超时取消任务完成，成功取消 {} 个订单，失败 {} 个订单", canceledCount, failedCount);
        }
        catch (Exception e)
        {
            log.error("订单超时取消任务执行异常", e);
        }
    }

    /**
     * 取消单个订单(带事务,先锁定状态再回滚库存,避免并发时库存虚增)
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(TOrder order) throws Exception
    {
        // 先用乐观锁抢占订单状态,避免与用户支付/手动取消并发
        int updated = orderService.changeOrderStatus(order.getOrderId(), 0, 4, null, null, null,
            DateUtils.getNowDate(), order.getExpressNo(), "订单超时自动取消");
        if (updated <= 0)
        {
            // 状态已变更(已支付或已取消),跳过
            throw new Exception("订单状态已变更,无需取消");
        }

        // 状态锁定成功后才回滚库存
        List<TOrderItem> items = OrderUtils.loadOrderItems(order.getOrderId(), orderItemService);
        for (TOrderItem item : items)
        {
            // 商品可能已被后台删除,此时 update 影响 0 行;仅记日志,不阻塞订单取消
            if (productService.increaseStock(item.getProductId(), item.getQuantity()) <= 0)
            {
                log.warn("订单 {} 库存回补失败(商品 {} 可能已删除),跳过该明细继续取消",
                    order.getOrderNo(), item.getProductId());
            }
        }
    }
}
