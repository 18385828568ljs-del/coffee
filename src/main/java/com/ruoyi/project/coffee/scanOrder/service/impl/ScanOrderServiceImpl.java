package com.ruoyi.project.coffee.scanOrder.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.activity.service.MarketingActivityEngine;
import com.ruoyi.project.coffee.scanOrder.domain.ScanCart;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderItem;
import com.ruoyi.project.coffee.scanOrder.domain.ScanTableQrcode;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanCartMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderItemMapper;
import com.ruoyi.project.coffee.scanOrder.mapper.ScanOrderMapper;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;
import com.ruoyi.project.coffee.scanOrder.service.IScanTableQrcodeService;
import com.ruoyi.project.coffee.wallet.service.WalletService;

/**
 * 扫码点单-订单Service实现
 */
@Service
public class ScanOrderServiceImpl implements IScanOrderService
{
    private static final String ORDER_NO_PREFIX = "SO";
    private static final String DEFAULT_SCENE = "dine_in";

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanOrder createOrderFromCart(Long userId, String openid, Long shopId,
                                         String tableNo, String remark, String payType)
    {
        if (userId == null)
        {
            throw new ServiceException("请先登录后再下单");
        }

        // 校验桌台号有效性(如果传入了tableNo)
        if (StringUtils.isNotEmpty(tableNo) && shopId != null)
        {
            ScanTableQrcode table = scanTableQrcodeService.selectByShopAndTable(shopId, tableNo);
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
        query.setOpenid(emptyToNull(openid));
        query.setShopId(shopId);
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
        order.setShopId(shopId);
        order.setTableNo(emptyToNull(tableNo));
        order.setScene(DEFAULT_SCENE);
        order.setStatus(0);
        order.setPayType(emptyToNull(payType));
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
    public MarketingPreviewResult previewOrderFromCart(Long userId, String openid, Long shopId, String tableNo)
    {
        if (userId == null)
        {
            return new MarketingPreviewResult();
        }
        ScanCart query = new ScanCart();
        query.setUserId(userId);
        query.setOpenid(emptyToNull(openid));
        query.setShopId(shopId);
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
        return scanOrderMapper.selectScanOrderList(scanOrder);
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
        return scanOrderMapper.selectScanOrderList(query);
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
            resolvedPayType = order.getPayType();
        }

        // 先用乐观锁抢占订单状态,避免并发重复扣款
        Date now = DateUtils.getNowDate();
        int affected = scanOrderMapper.updateScanOrderStatus(
            orderId, 0, 1,
            now, null, null,
            resolvedPayType);
        if (affected <= 0)
        {
            // 状态已变更(并发或重复支付),直接返回失败,此时尚未扣款
            throw new ServiceException("订单状态已变更,支付失败,请刷新后重试");
        }

        // 状态锁定成功后才执行扣款,余额支付走真实扣款,其余支付方式(wechat 等)目前为模拟
        if ("balance".equals(resolvedPayType))
        {
            BigDecimal amount = order.getPayAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            {
                throw new ServiceException("订单金额异常,无法支付");
            }
            // deductForOrder 会在余额不足时抛 ServiceException,与本事务一起回滚
            walletService.deductForOrder(order.getUserId(), amount, order.getOrderNo());
        }

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
            orderId, 0, 3,
            null, null, now,
            null);
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
