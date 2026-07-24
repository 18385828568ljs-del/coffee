package com.ruoyi.project.coffee.order.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.order.domain.TOrder;

/**
 * 订单工具类
 */
public class OrderUtils
{
    /**
     * 构建订单商品映射表
     *
     * @param orders 订单列表
     * @param orderItemService 订单商品服务
     * @return 订单ID到商品列表的映射
     */
    public static Map<Long, List<TOrderItem>> buildOrderItemsMap(List<TOrder> orders, ITOrderItemService orderItemService)
    {
        Map<Long, List<TOrderItem>> itemMap = new HashMap<Long, List<TOrderItem>>();
        if (orders == null || orders.isEmpty())
        {
            return itemMap;
        }

        List<Long> orderIds = new ArrayList<Long>();
        for (TOrder order : orders)
        {
            if (order != null && order.getOrderId() != null)
            {
                orderIds.add(order.getOrderId());
            }
        }
        if (orderIds.isEmpty())
        {
            return itemMap;
        }

        List<TOrderItem> items = orderItemService.selectTOrderItemListByOrderIds(orderIds);
        for (TOrderItem item : items)
        {
            if (item == null || item.getOrderId() == null)
            {
                continue;
            }
            List<TOrderItem> orderItems = itemMap.get(item.getOrderId());
            if (orderItems == null)
            {
                orderItems = new ArrayList<TOrderItem>();
                itemMap.put(item.getOrderId(), orderItems);
            }
            orderItems.add(item);
        }
        return itemMap;
    }

    /**
     * 加载订单商品列表
     *
     * @param orderId 订单ID
     * @param orderItemService 订单商品服务
     * @return 商品列表
     */
    public static List<TOrderItem> loadOrderItems(Long orderId, ITOrderItemService orderItemService)
    {
        TOrderItem query = new TOrderItem();
        query.setOrderId(orderId);
        return orderItemService.selectTOrderItemList(query);
    }
}
