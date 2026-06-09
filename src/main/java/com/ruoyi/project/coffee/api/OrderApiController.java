package com.ruoyi.project.coffee.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.common.annotation.RateLimit;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.cart.service.ITCartService;
import com.ruoyi.project.coffee.item.domain.TOrderItem;
import com.ruoyi.project.coffee.item.service.ITOrderItemService;
import com.ruoyi.project.coffee.order.domain.TOrder;
import com.ruoyi.project.coffee.order.service.ITOrderService;
import com.ruoyi.project.coffee.order.util.OrderUtils;
import com.ruoyi.project.coffee.product.domain.TProduct;
import com.ruoyi.project.coffee.product.service.ITProductService;
import com.ruoyi.project.coffee.wallet.service.WalletService;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.common.util.AuthorizationUtils;
import com.ruoyi.project.coffee.common.util.OrderStatusValidator;

/**
 * 小程序订单接口
 */
@RestController
@RequestMapping("/api/order")
public class OrderApiController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(OrderApiController.class);

    private static final int DUPLICATE_ORDER_WINDOW_SECONDS = 60;
    private static final int LOCK_CLEANUP_THRESHOLD = 1000;

    private static final ConcurrentMap<Long, ReentrantLock> USER_ORDER_LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Long> LOCK_LAST_USE_TIME = new ConcurrentHashMap<>();

    @Autowired
    private ITOrderService orderService;

    @Autowired
    private ITOrderItemService orderItemService;

    @Autowired
    private ITProductService productService;

    @Autowired
    private ITCartService cartService;

    @Autowired
    private MarketingActivityEngine marketingActivityEngine;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MemberService memberService;

    @GetMapping("/list")
    public TableDataInfo getOrderList(@RequestParam(required = false) String status)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TOrder query = new TOrder();
        query.setUserId(userId);
        if (StringUtils.isNotEmpty(status) && !"null".equalsIgnoreCase(status))
        {
            try
            {
                query.setStatus(Integer.valueOf(status));
            }
            catch (NumberFormatException e)
            {
                return getDataTable(java.util.Collections.emptyList());
            }
        }

        startPage();
        List<TOrder> list = orderService.selectTOrderList(query);
        Map<Long, List<TOrderItem>> itemMap = OrderUtils.buildOrderItemsMap(list, orderItemService);
        for (TOrder item : list)
        {
            List<TOrderItem> orderItems = itemMap.get(item.getOrderId());
            item.setOrderItems(orderItems == null ? new ArrayList<TOrderItem>() : orderItems);
        }
        return getDataTable(list);
    }

    @GetMapping("/{orderId}")
    public AjaxResult getOrderDetail(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TOrder order = orderService.selectTOrderByOrderId(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }

        order.setOrderItems(OrderUtils.loadOrderItems(orderId, orderItemService));
        return AjaxResult.success(order);
    }

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    @RateLimit(key = "order_create", time = 60, count = 5, limitType = RateLimit.LimitType.USER)
    public AjaxResult createOrder(@RequestBody TOrder order)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        order.setUserId(userId);
        if (userId == null)
        {
            return AjaxResult.error("用户信息缺失");
        }
        if (order.getReceiverName() == null || order.getReceiverPhone() == null || order.getReceiverAddress() == null)
        {
            return AjaxResult.error("收货信息不完整");
        }

        List<TOrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty())
        {
            return AjaxResult.error("订单商品不能为空");
        }

        ReentrantLock orderLock = getUserOrderLock(order.getUserId());
        orderLock.lock();
        try
        {
            String orderNo = "CF" + System.currentTimeMillis();
            order.setOrderNo(orderNo);
            order.setStatus(0);
            order.setCreateTime(DateUtils.getNowDate());

            if (order.getCartIds() != null && !order.getCartIds().isEmpty())
            {
                for (Long cartId : order.getCartIds())
                {
                    TCart cartItem = cartService.selectTCartByCartId(cartId);
                    if (cartItem == null || !order.getUserId().equals(cartItem.getUserId()))
                    {
                        return AjaxResult.error("购物车商品已失效，请刷新后重试");
                    }

                    TProduct cartProduct = productService.selectTProductByProductId(cartItem.getProductId());
                    if (cartProduct == null || cartProduct.getStatus() != 1)
                    {
                        return AjaxResult.error("购物车中的商品已下架，请刷新后重试");
                    }
                    if (cartProduct.getStock() == null || cartProduct.getStock() < cartItem.getQuantity())
                    {
                        return AjaxResult.error("购物车中的商品库存不足，请刷新后重试");
                    }
                }
            }

            for (TOrderItem item : items)
            {
                if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() < 1)
                {
                    return AjaxResult.error("订单商品参数不完整");
                }

                TProduct product = productService.selectTProductByProductId(item.getProductId());
                if (product == null || product.getStatus() != 1)
                {
                    return AjaxResult.error("商品不存在或已下架");
                }
                if (product.getStock() == null || product.getStock() < item.getQuantity())
                {
                    return AjaxResult.error("商品库存不足");
                }

                item.setProductName(product.getProductName());
                item.setProductImage(product.getImageUrl());
                item.setPrice(product.getPrice());
                if (StringUtils.isEmpty(item.getSpec()))
                {
                    item.setSpec(product.getRemark() == null ? "" : product.getRemark());
                }
                item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            List<TOrderItem> finalItems = new ArrayList<TOrderItem>(items);
            // 防止客户端传入 payAmount/totalAmount/discountAmount 等字段直接落库,
            // 一律清零后由 MarketingActivityEngine 基于服务端商品价格 + 营销规则重算
            order.setTotalAmount(null);
            order.setPayAmount(null);
            order.setDiscountAmount(null);
            order.setMemberDiscount(null);
            order.setFreightAmount(null);
            order.setActivitySummary(null);
            finalItems.addAll(marketingActivityEngine.applyOrderMarketing(order, items).getGiftItems());
            order.setOrderItems(finalItems);

            TOrder duplicateOrder = findRecentDuplicatePendingOrder(order, finalItems);
            if (duplicateOrder != null)
            {
                return AjaxResult.success("订单已创建，请勿重复提交", duplicateOrder.getOrderId());
            }

            orderService.insertTOrder(order);

            for (TOrderItem item : finalItems)
            {
                item.setOrderId(order.getOrderId());
                orderItemService.insertTOrderItem(item);
                if (productService.decreaseStock(item.getProductId(), item.getQuantity()) <= 0)
                {
                    throw new RuntimeException("商品库存不足，请刷新后重试");
                }
            }

            if (order.getCartIds() != null && !order.getCartIds().isEmpty())
            {
                for (Long cartId : order.getCartIds())
                {
                    TCart cartItem = cartService.selectTCartByCartId(cartId);
                    if (cartItem != null && order.getUserId().equals(cartItem.getUserId()))
                    {
                        cartService.deleteTCartByCartId(cartId);
                    }
                }
            }

            return AjaxResult.success("订单创建成功", order.getOrderId());
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("订单创建失败，请稍后重试");
        }
        finally
        {
            orderLock.unlock();
        }
    }

    @PutMapping("/cancel/{orderId}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult cancelOrder(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TOrder order = orderService.selectTOrderByOrderId(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0)
        {
            return AjaxResult.error("只能取消待支付订单");
        }

        List<TOrderItem> items = OrderUtils.loadOrderItems(orderId, orderItemService);
        for (TOrderItem item : items)
        {
            // 商品可能已被后台删除,此时 update 影响 0 行;仅记日志,不阻塞订单取消
            if (productService.increaseStock(item.getProductId(), item.getQuantity()) <= 0)
            {
                log.warn("订单 {} 库存回补失败(商品 {} 可能已删除),跳过该明细继续取消",
                    order.getOrderNo(), item.getProductId());
            }
        }

        int updated = orderService.changeOrderStatus(orderId, 0, 4, null, null, null,
            DateUtils.getNowDate(), order.getExpressNo(), order.getRemark());
        if (updated <= 0)
        {
            throw new RuntimeException("订单状态已变化，请刷新后重试");
        }

        return AjaxResult.success("订单已取消");
    }

    @PutMapping("/pay/{orderId}")
    @RateLimit(key = "order_pay", time = 60, count = 10, limitType = RateLimit.LimitType.USER)
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult payOrder(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TOrder order = orderService.selectTOrderByOrderId(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0)
        {
            return AjaxResult.error("当前订单状态不可支付");
        }

        if ("balance".equals(order.getPayType()))
        {
            try {
                walletService.deductForOrder(userId, order.getPayAmount(), order.getOrderNo());
            } catch (Exception e) {
                return AjaxResult.error(e.getMessage());
            }
        }
        else if ("wechat".equals(order.getPayType()))
        {
            // 微信支付功能开发中,暂不支持
            return AjaxResult.error("微信支付功能开发中,请选择余额支付");
        }
        else
        {
            return AjaxResult.error("不支持的支付方式: " + order.getPayType());
        }

        int updated = orderService.changeOrderStatus(orderId, 0, 1, DateUtils.getNowDate(), null, null,
            null, order.getExpressNo(), order.getRemark());
        if (updated <= 0)
        {
            throw new RuntimeException("订单状态已变化，请勿重复支付");
        }

        // 支付成功后增加对应消费金额
        memberService.addSpending(userId, order.getPayAmount());

        return AjaxResult.success("支付成功");
    }

    @PutMapping("/confirm/{orderId}")
    public AjaxResult confirmOrder(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TOrder order = orderService.selectTOrderByOrderId(orderId);
        // 使用统一的越权检查和状态校验
        AuthorizationUtils.checkOrderOwnership(userId, order, TOrder::getUserId);
        OrderStatusValidator.validateForConfirm(order.getStatus());

        int updated = orderService.changeOrderStatus(orderId, 2, 3, null, null, DateUtils.getNowDate(),
            null, order.getExpressNo(), order.getRemark());
        if (updated <= 0)
        {
            return AjaxResult.error("订单状态已变化，请刷新后重试");
        }
        return AjaxResult.success("确认收货成功");
    }

    private ReentrantLock getUserOrderLock(Long userId)
    {
        cleanupExpiredLocks();
        LOCK_LAST_USE_TIME.put(userId, System.currentTimeMillis());
        return USER_ORDER_LOCKS.computeIfAbsent(userId, key -> new ReentrantLock());
    }

    private void cleanupExpiredLocks()
    {
        if (USER_ORDER_LOCKS.size() < LOCK_CLEANUP_THRESHOLD)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = 30 * 60 * 1000L;

        List<Long> expiredUserIds = new ArrayList<Long>();
        for (Map.Entry<Long, Long> entry : LOCK_LAST_USE_TIME.entrySet())
        {
            if (now - entry.getValue() > expireTime)
            {
                expiredUserIds.add(entry.getKey());
            }
        }

        for (Long userId : expiredUserIds)
        {
            ReentrantLock lock = USER_ORDER_LOCKS.get(userId);
            if (lock != null && !lock.isLocked())
            {
                USER_ORDER_LOCKS.remove(userId);
                LOCK_LAST_USE_TIME.remove(userId);
            }
        }
    }

    private TOrder findRecentDuplicatePendingOrder(TOrder order, List<TOrderItem> currentItems)
    {
        TOrder query = new TOrder();
        query.setUserId(order.getUserId());
        query.setStatus(0);
        List<TOrder> pendingOrders = orderService.selectTOrderList(query);
        if (pendingOrders == null || pendingOrders.isEmpty())
        {
            return null;
        }

        for (TOrder pendingOrder : pendingOrders)
        {
            if (pendingOrder.getCreateTime() == null
                || Math.abs(DateUtils.getNowDate().getTime() - pendingOrder.getCreateTime().getTime())
                    > DUPLICATE_ORDER_WINDOW_SECONDS * 1000L)
            {
                continue;
            }
            if (!StringUtils.equals(order.getReceiverName(), pendingOrder.getReceiverName())
                || !StringUtils.equals(order.getReceiverPhone(), pendingOrder.getReceiverPhone())
                || !StringUtils.equals(order.getReceiverAddress(), pendingOrder.getReceiverAddress()))
            {
                continue;
            }
            if (order.getPayAmount() != null && pendingOrder.getPayAmount() != null
                && order.getPayAmount().compareTo(pendingOrder.getPayAmount()) != 0)
            {
                continue;
            }

            List<TOrderItem> pendingItems = OrderUtils.loadOrderItems(pendingOrder.getOrderId(), orderItemService);
            if (isSameOrderItems(currentItems, pendingItems))
            {
                return pendingOrder;
            }
        }
        return null;
    }

    private boolean isSameOrderItems(List<TOrderItem> currentItems, List<TOrderItem> pendingItems)
    {
        if (currentItems == null || pendingItems == null || currentItems.size() != pendingItems.size())
        {
            return false;
        }

        List<TOrderItem> left = normalizeItems(currentItems);
        List<TOrderItem> right = normalizeItems(pendingItems);
        for (int i = 0; i < left.size(); i++)
        {
            TOrderItem leftItem = left.get(i);
            TOrderItem rightItem = right.get(i);
            if (!Objects.equals(leftItem.getProductId(), rightItem.getProductId())
                || !Objects.equals(leftItem.getQuantity(), rightItem.getQuantity())
                || !StringUtils.equals(StringUtils.nvl(leftItem.getSpec(), ""),
                    StringUtils.nvl(rightItem.getSpec(), ""))
                || compareAmount(leftItem.getPrice(), rightItem.getPrice()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    private List<TOrderItem> normalizeItems(List<TOrderItem> items)
    {
        List<TOrderItem> normalized = new ArrayList<>(items);
        normalized.sort(Comparator
            .comparing(TOrderItem::getProductId, Comparator.nullsLast(Long::compareTo))
            .thenComparing(item -> StringUtils.nvl(item.getSpec(), ""))
            .thenComparing(TOrderItem::getQuantity, Comparator.nullsLast(Long::compareTo)));
        return normalized;
    }

    private int compareAmount(BigDecimal left, BigDecimal right)
    {
        BigDecimal safeLeft = left == null ? BigDecimal.ZERO : left;
        BigDecimal safeRight = right == null ? BigDecimal.ZERO : right;
        return safeLeft.compareTo(safeRight);
    }

}
