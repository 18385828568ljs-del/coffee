package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.payment.service.PaymentLogService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderStatus;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCartMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderItemMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;
import com.ruoyi.project.coffee.scanOrder.wx.ScanOrderSubscribeMessageService;
import com.ruoyi.project.coffee.wallet.service.WalletService;

/**
 * 扫码点单-订单Service实现
 */
@Service
public class ScanOrderServiceImpl implements IScanOrderService
{
    private static final String ORDER_NO_PREFIX = "SO";
    private static final String DEFAULT_SCENE = "dine_in";
    private static final int DEFAULT_MINUTES_PER_ORDER = 5;
    private static final long URGE_INTERVAL_MILLIS = 60L * 1000L;

    /**
     * 取餐号内存序列: key 为日期, value 为当天已分配到的最大号。
     * 单机部署下用 AtomicInteger 原子自增, 解决并发支付回调"查最大值+1"两步非原子导致的取餐号重复。
     * 首次访问当天的 key 时, 从 DB 当天最大值初始化(兼容白天重启)。
     * 实例字段: @Service 单例下全局唯一, 生产行为与 static 一致, 但保证单测隔离。
     */
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> PICKUP_NO_SEQ =
        new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>();


    @Autowired
    private ScanOrderMapper scanOrderMapper;

    @Autowired
    private ScanOrderItemMapper scanOrderItemMapper;

    @Autowired
    private ScanCartMapper scanCartMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private MarketingActivityEngine marketingActivityEngine;

    @Autowired
    private IScanTableQrcodeService scanTableQrcodeService;

    @Autowired
    private ScanOrderSubscribeMessageService subscribeMessageService;

    @Autowired
    private PaymentLogService paymentLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanOrder createOrderFromCart(Long userId, String openid,
                                         String tableNo, String remark, String payType)
    {
        if (userId == null)
        {
            throw new ServiceException("请先登录后再下单");
        }

        // 校验桌台号有效性(如果传入了tableNo)
        if (StringUtils.isNotEmpty(tableNo))
        {
            ScanTableQrcode table = scanTableQrcodeService.selectByTableNo(tableNo);
            if (table == null)
            {
                throw new ServiceException("桌台不存在,请重新扫码");
            }
            if (table.getStatus() == null || table.getStatus() != 1)
            {
                throw new ServiceException("桌台已停用,请联系工作人员");
            }
        }

        ScanCart query = new ScanCart();
        query.setUserId(userId);
        if (userId == null)
        {
            query.setOpenid(emptyToNull(openid));
        }
        query.setTableNo(emptyToNull(tableNo));
        query.setStatus(1);
        List<ScanCart> cartList = scanCartMapper.selectScanCartList(query);
        // 只结算被勾选的
        List<ScanCart> selected = new ArrayList<ScanCart>();
        if (cartList != null)
        {
            for (ScanCart c : cartList)
            {
                if (c.getDelFlag() != null && c.getDelFlag() == 1) continue;
                if (c.getStatus() != null && c.getStatus() != 1) continue;
                if (c.getSelected() == null || c.getSelected() == 1)
                {
                    selected.add(c);
                }
            }
        }
        if (selected.isEmpty())
        {
            throw new ServiceException("购物车为空");
        }

        Date now = DateUtils.getNowDate();
        ScanOrder order = new ScanOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOpenid(emptyToNull(openid));
        order.setTableNo(emptyToNull(tableNo));
        order.setScene(DEFAULT_SCENE);
        order.setStatus(0);
        String resolvedPayType = emptyToNull(payType);
        if (resolvedPayType != null && !"balance".equals(resolvedPayType))
        {
            throw new ServiceException("不支持的支付方式: " + resolvedPayType);
        }
        order.setPayType("balance");
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);

        // 服务端走营销活动 + 会员折扣引擎重算所有金额,前端不能篡改
        try
        {
            marketingActivityEngine.applyScanOrderMarketing(order, selected);
        }
        catch (IllegalArgumentException e)
        {
            // 引擎对商品状态/参数的校验失败,转成业务异常返给前端
            throw new ServiceException(e.getMessage());
        }

        scanOrderMapper.insertScanOrder(order);

        List<ScanOrderItem> items = new ArrayList<ScanOrderItem>(selected.size());
        for (ScanCart c : selected)
        {
            ScanOrderItem item = new ScanOrderItem();
            item.setOrderId(order.getOrderId());
            item.setProductId(c.getProductId());
            item.setProductName(c.getProductName());
            item.setProductImage(c.getProductImage());
            item.setSpec(c.getSpecText());
            item.setSpecJson(c.getSpecJson());
            BigDecimal price = c.getPrice() == null ? BigDecimal.ZERO : c.getPrice();
            int qty = c.getQuantity() == null ? 0 : c.getQuantity();
            item.setPrice(price);
            item.setQuantity(qty);
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(qty)));
            item.setCreateTime(now);
            items.add(item);
        }
        scanOrderItemMapper.batchInsertScanOrderItem(items);

        // 软删除已结算的购物车记录
        for (ScanCart c : selected)
        {
            scanCartMapper.logicDeleteById(c.getId());
        }

        order.setItems(items);
        return order;
    }

    @Override
    public MarketingPreviewResult previewOrderFromCart(Long userId, String openid, String tableNo)
    {
        if (userId == null)
        {
            return new MarketingPreviewResult();
        }
        ScanCart query = new ScanCart();
        query.setUserId(userId);
        if (userId == null)
        {
            query.setOpenid(emptyToNull(openid));
        }
        query.setTableNo(emptyToNull(tableNo));
        query.setStatus(1);
        List<ScanCart> cartList = scanCartMapper.selectScanCartList(query);
        List<ScanCart> selected = new ArrayList<ScanCart>();
        if (cartList != null)
        {
            for (ScanCart c : cartList)
            {
                if (c.getDelFlag() != null && c.getDelFlag() == 1) continue;
                if (c.getStatus() != null && c.getStatus() != 1) continue;
                if (c.getSelected() == null || c.getSelected() == 1)
                {
                    selected.add(c);
                }
            }
        }
        if (selected.isEmpty())
        {
            return new MarketingPreviewResult();
        }
        try
        {
            return marketingActivityEngine.previewScanOrder(userId, selected);
        }
        catch (IllegalArgumentException e)
        {
            // 商品下架等问题前端通过空预览感知,真正下单时再返回明确错误
            return new MarketingPreviewResult();
        }
    }

    @Override
    public ScanOrder selectScanOrderById(Long orderId)
    {
        return scanOrderMapper.selectScanOrderById(orderId);
    }

    @Override
    public ScanOrder selectScanOrderByOrderNo(String orderNo)
    {
        if (orderNo == null || orderNo.trim().isEmpty())
        {
            return null;
        }
        return scanOrderMapper.selectScanOrderByOrderNo(orderNo.trim());
    }

    @Override
    public ScanOrder selectScanOrderWithItems(Long orderId)
    {
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null)
        {
            return null;
        }
        List<ScanOrderItem> items = scanOrderItemMapper.selectItemsByOrderId(orderId);
        order.setItems(items == null ? new ArrayList<ScanOrderItem>() : items);
        return order;
    }

    @Override
    public List<ScanOrder> selectScanOrderList(ScanOrder scanOrder)
    {
        List<ScanOrder> orders = scanOrderMapper.selectScanOrderList(scanOrder);
        attachItems(orders);
        return orders;
    }

    @Override
    public List<ScanOrder> selectMyOrderList(Long userId)
    {
        if (userId == null)
        {
            return new ArrayList<ScanOrder>();
        }
        ScanOrder query = new ScanOrder();
        query.setUserId(userId);
        return selectScanOrderList(query);
    }

    private void attachItems(List<ScanOrder> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return;
        }
        List<Long> orderIds = new ArrayList<Long>();
        for (ScanOrder order : orders)
        {
            if (order != null && order.getOrderId() != null)
            {
                orderIds.add(order.getOrderId());
            }
        }
        if (orderIds.isEmpty())
        {
            return;
        }
        List<ScanOrderItem> items = scanOrderItemMapper.selectItemsByOrderIds(orderIds);
        Map<Long, List<ScanOrderItem>> itemMap = new HashMap<Long, List<ScanOrderItem>>();
        if (items != null)
        {
            for (ScanOrderItem item : items)
            {
                if (item == null || item.getOrderId() == null)
                {
                    continue;
                }
                List<ScanOrderItem> orderItems = itemMap.get(item.getOrderId());
                if (orderItems == null)
                {
                    orderItems = new ArrayList<ScanOrderItem>();
                    itemMap.put(item.getOrderId(), orderItems);
                }
                orderItems.add(item);
            }
        }
        for (ScanOrder order : orders)
        {
            if (order == null || order.getOrderId() == null)
            {
                continue;
            }
            List<ScanOrderItem> orderItems = itemMap.get(order.getOrderId());
            order.setItems(orderItems == null ? new ArrayList<ScanOrderItem>() : orderItems);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int payOrder(Long orderId, String payType)
    {
        if (orderId == null)
        {
            return 0;
        }
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null)
        {
            return 0;
        }
        String resolvedPayType = emptyToNull(payType);
        if (resolvedPayType == null)
        {
            resolvedPayType = "balance";
        }
        if (!"balance".equals(resolvedPayType))
        {
            throw new ServiceException("不支持的支付方式: " + resolvedPayType);
        }

        // 先用乐观锁抢占订单状态,避免并发重复扣款
        Date now = DateUtils.getNowDate();
        String pickupNo = generatePickupNo();
        int estimatedWaitMinutes = estimateWaitMinutes();
        int affected = scanOrderMapper.updateScanOrderStatus(
            orderId, ScanOrderStatus.PENDING_PAY, ScanOrderStatus.MAKING,
            now, now, now, null, null, null,
            pickupNo, estimatedWaitMinutes,
            resolvedPayType);
        if (affected <= 0)
        {
            // 状态已变更(并发或重复支付),直接返回失败,此时尚未扣款
            throw new ServiceException("订单状态已变更,支付失败,请刷新后重试");
        }

        // 状态锁定成功后才执行扣款；余额支付真实扣余额。
        BigDecimal amount = order.getPayAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("订单金额异常,无法支付");
        }
        // deductForOrder 会在余额不足时抛 ServiceException,与本事务一起回滚
        walletService.deductForOrder(order.getUserId(), amount, order.getOrderNo());
        paymentLogService.recordBalancePaid("SCAN_ORDER", order.getOrderNo(), order.getUserId(), amount);

        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long orderId)
    {
        if (orderId == null)
        {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        return scanOrderMapper.updateScanOrderStatus(
            orderId, ScanOrderStatus.PENDING_PAY, ScanOrderStatus.CANCELLED,
            null, null, null, null, null, now,
            null, null,
            null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int callOrder(Long orderId)
    {
        int affected = changeStatus(orderId, ScanOrderStatus.MAKING, ScanOrderStatus.WAIT_PICKUP);
        if (affected > 0)
        {
            subscribeMessageService.sendPickupNoticeAsync(scanOrderMapper.selectScanOrderById(orderId));
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeOrder(Long orderId)
    {
        return changeStatus(orderId, ScanOrderStatus.WAIT_PICKUP, ScanOrderStatus.COMPLETED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelPaidOrder(Long orderId)
    {
        return changeStatus(orderId, ScanOrderStatus.MAKING, ScanOrderStatus.CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int urgeOrder(Long orderId, Long userId)
    {
        if (orderId == null || userId == null)
        {
            return 0;
        }
        ScanOrder order = scanOrderMapper.selectScanOrderById(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            throw new ServiceException("订单不存在");
        }
        if (!ScanOrderStatus.isUnfinishedPaid(order.getStatus()))
        {
            throw new ServiceException("当前订单不能催单");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 1)
        {
            throw new ServiceException("退款处理中，不能催单");
        }
        if (order.getRefundStatus() != null && order.getRefundStatus() == 2)
        {
            throw new ServiceException("订单已退款，不能催单");
        }
        Date now = DateUtils.getNowDate();
        Date lastUrgeTime = order.getLastUrgeTime();
        if (lastUrgeTime != null && now.getTime() - lastUrgeTime.getTime() < URGE_INTERVAL_MILLIS)
        {
            throw new ServiceException("催单太频繁，请稍后再试");
        }
        return scanOrderMapper.markUrged(orderId, now);
    }

    private int changeStatus(Long orderId, int oldStatus, int newStatus)
    {
        if (orderId == null)
        {
            return 0;
        }
        Date now = DateUtils.getNowDate();
        Date acceptTime = null;
        Date makingTime = null;
        Date callTime = null;
        Date finishTime = null;
        Date cancelTime = null;
        if (newStatus == ScanOrderStatus.MAKING)
        {
            acceptTime = now;
            makingTime = now;
        }
        else if (newStatus == ScanOrderStatus.WAIT_PICKUP)
        {
            callTime = now;
        }
        else if (newStatus == ScanOrderStatus.COMPLETED)
        {
            finishTime = now;
        }
        else if (newStatus == ScanOrderStatus.CANCELLED)
        {
            cancelTime = now;
        }
        return scanOrderMapper.updateScanOrderStatus(
            orderId, oldStatus, newStatus,
            null, acceptTime, makingTime, callTime, finishTime, cancelTime,
            null, null, null);
    }

    private String generatePickupNo()
    {
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        // computeIfAbsent 保证当天只初始化一次, 从 DB 读取已有最大值
        java.util.concurrent.atomic.AtomicInteger seq = PICKUP_NO_SEQ.computeIfAbsent(today, k -> {
            Integer maxToday = scanOrderMapper.selectMaxPickupNoToday();
            return new java.util.concurrent.atomic.AtomicInteger(maxToday == null ? 0 : maxToday);
        });
        return String.format("%03d", seq.incrementAndGet());
    }

    private int estimateWaitMinutes()
    {
        int waitingOrders = 0;
        Integer count = scanOrderMapper.countUnfinishedPaidOrders();
        if (count != null)
        {
            waitingOrders = count;
        }
        return Math.max(DEFAULT_MINUTES_PER_ORDER, (waitingOrders + 1) * DEFAULT_MINUTES_PER_ORDER);
    }

    private String generateOrderNo()
    {
        // 订单号: SO + yyyyMMddHHmmss + 4 位随机;保证 64 字符列容量内
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return ORDER_NO_PREFIX + ts + rand;
    }

    private String emptyToNull(String s)
    {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
