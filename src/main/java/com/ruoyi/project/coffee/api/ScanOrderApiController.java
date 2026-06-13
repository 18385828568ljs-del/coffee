package com.ruoyi.project.coffee.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;
import com.ruoyi.project.coffee.activity.domain.MarketingPreviewResult;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.member.service.MemberService;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrder;
import com.ruoyi.project.coffee.scanOrder.domain.ScanOrderStatus;
import com.ruoyi.project.coffee.scanOrder.service.IScanOrderService;

/**
 * 小程序扫码点单订单接口
 *
 * 仅操作 t_scan_order / t_scan_order_item / t_scan_cart,
 * 不写入 t_order / t_cart。
 */
@RestController
@RequestMapping("/api/scanOrder")
public class ScanOrderApiController extends BaseController
{
    private static final long DEFAULT_SHOP_ID = 1L;

    @Autowired
    private IScanOrderService scanOrderService;

    @Autowired
    private MemberService memberService;

    @Value("${wx.miniapp.subscribe.pickup-template-id:}")
    private String pickupTemplateId;

    @GetMapping("/subscribe-config")
    public AjaxResult subscribeConfig()
    {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("pickupTemplateId", pickupTemplateId);
        return AjaxResult.success(data);
    }

    @PostMapping("/create")
    public AjaxResult createOrder(@RequestBody Map<String, Object> body)
    {
        if (body == null)
        {
            return AjaxResult.error("请求参数不能为空");
        }

        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录后再下单");
        }
        String openid = currentOpenid();
        if (openid == null)
        {
            openid = trimToNull(parseString(body.get("openid")));
        }
        Long shopId = parseLong(body.get("shopId"));
        if (shopId == null)
        {
            shopId = DEFAULT_SHOP_ID;
        }
        String tableNo = trimToNull(parseString(body.get("tableNo")));
        String remark = parseString(body.get("remark"));
        String payType = trimToNull(parseString(body.get("payType")));

        try
        {
            ScanOrder order = scanOrderService.createOrderFromCart(
                userId, openid, shopId, tableNo, remark, payType);
            return AjaxResult.success("下单成功", order);
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public TableDataInfo getMyOrderList(
        @RequestParam(value = "status", required = false) Integer status)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return getDataTable(java.util.Collections.<ScanOrder>emptyList());
        }
        ScanOrder query = new ScanOrder();
        query.setUserId(userId);
        query.setStatus(status);
        startPage();
        List<ScanOrder> list = scanOrderService.selectScanOrderList(query);
        return getDataTable(list);
    }

    /**
     * 预览金额拆分:返回原价、活动优惠、会员折扣、应付,供前端"立即付款"前展示
     */
    @GetMapping("/preview")
    public AjaxResult previewOrder(
        @RequestParam(value = "shopId", required = false) Long shopId,
        @RequestParam(value = "tableNo", required = false) String tableNo,
        @RequestParam(value = "openid", required = false) String openid)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }
        Long resolvedShopId = shopId == null ? DEFAULT_SHOP_ID : shopId;
        String resolvedOpenid = currentOpenid();
        if (resolvedOpenid == null)
        {
            resolvedOpenid = trimToNull(openid);
        }
        MarketingPreviewResult preview = scanOrderService.previewOrderFromCart(
            userId, resolvedOpenid, resolvedShopId, trimToNull(tableNo));

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("totalAmount", preview.getTotalAmount());
        data.put("payAmount", preview.getPayAmount());
        data.put("discountAmount", preview.getDiscountAmount());
        data.put("memberDiscount", preview.getMemberDiscount());
        data.put("activitySummary", preview.getActivitySummary() == null ? "" : preview.getActivitySummary());
        return AjaxResult.success(data);
    }

    @GetMapping("/{orderId}")
    public AjaxResult getOrderDetail(@PathVariable Long orderId)
    {
        if (orderId == null)
        {
            return AjaxResult.error("订单ID不能为空");
        }
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }
        ScanOrder order = scanOrderService.selectScanOrderWithItems(orderId);
        if (order == null || !userId.equals(order.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        return AjaxResult.success(order);
    }

    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancelOrder(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }
        ScanOrder exist = scanOrderService.selectScanOrderById(orderId);
        if (exist == null || !userId.equals(exist.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        Integer status = exist.getStatus();
        if (status == null || status != ScanOrderStatus.PENDING_PAY)
        {
            if (status != null && status == ScanOrderStatus.CANCELLED)
            {
                return AjaxResult.error("订单已取消");
            }
            return AjaxResult.error("仅未支付订单可以取消");
        }
        int affected = scanOrderService.cancelOrder(orderId);
        if (affected <= 0)
        {
            return AjaxResult.error("订单状态已变更,取消失败,请刷新后重试");
        }
        return AjaxResult.success("订单已取消");
    }

    @PutMapping("/pay/{orderId}")
    public AjaxResult payOrder(@PathVariable Long orderId,
                               @RequestParam(value = "payType", required = false) String payType)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }
        ScanOrder exist = scanOrderService.selectScanOrderById(orderId);
        if (exist == null || !userId.equals(exist.getUserId()))
        {
            return AjaxResult.error("订单不存在");
        }
        Integer status = exist.getStatus();
        if (status == null)
        {
            return AjaxResult.error("订单状态异常");
        }
        if (ScanOrderStatus.isPaid(status))
        {
            return AjaxResult.error("订单已支付");
        }
        if (status == ScanOrderStatus.CANCELLED)
        {
            return AjaxResult.error("订单已取消");
        }
        if (status != ScanOrderStatus.PENDING_PAY)
        {
            return AjaxResult.error("仅未支付订单可以支付");
        }

        String resolvedPayType = trimToNull(payType);
        if (resolvedPayType == null)
        {
            resolvedPayType = exist.getPayType();
        }
        if (resolvedPayType == null)
        {
            resolvedPayType = "shouqianba";
        }
        if (!"balance".equals(resolvedPayType) && !"shouqianba".equals(resolvedPayType))
        {
            return AjaxResult.error("不支持的支付方式: " + resolvedPayType);
        }

        try
        {
            int affected = scanOrderService.payOrder(orderId, resolvedPayType);
            if (affected <= 0)
            {
                return AjaxResult.error("订单状态已变更,支付失败,请刷新后重试");
            }
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
        // 支付成功后累加会员消费,与普通订单 OrderApiController.payOrder 行为对齐
        ScanOrder paidOrder = scanOrderService.selectScanOrderById(orderId);
        if (paidOrder != null && paidOrder.getPayAmount() != null)
        {
            memberService.addSpending(userId, paidOrder.getPayAmount());
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderId", orderId);
        data.put("payStatus", 1);
        data.put("orderStatus", ScanOrderStatus.MAKING);
        data.put("pickupNo", paidOrder == null ? null : paidOrder.getPickupNo());
        data.put("estimatedWaitMinutes", paidOrder == null ? null : paidOrder.getEstimatedWaitMinutes());
        return AjaxResult.success("支付成功", data);
    }

    @PostMapping("/urge/{orderId}")
    public AjaxResult urgeOrder(@PathVariable Long orderId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null)
        {
            return AjaxResult.error("请先登录");
        }
        try
        {
            int affected = scanOrderService.urgeOrder(orderId, userId);
            if (affected <= 0)
            {
                return AjaxResult.error("催单失败，请刷新后重试");
            }
            return AjaxResult.success("已催单，商家会尽快处理");
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    private String parseString(Object v)
    {
        return v == null ? null : v.toString();
    }

    private Long parseLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private String trimToNull(String s)
    {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String currentOpenid()
    {
        AbucoderWxuser user = WxUserAuthContext.getCurrentUser();
        return user == null ? null : trimToNull(user.getOpenid());
    }
}
