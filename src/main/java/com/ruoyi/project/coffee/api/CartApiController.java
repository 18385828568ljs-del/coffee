package com.ruoyi.project.coffee.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.coffee.cart.domain.TCart;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.cart.service.ITCartService;

/**
 * 小程序购物车接口
 */
@RestController
@RequestMapping("/api/cart")
public class CartApiController extends BaseController
{
    @Autowired
    private ITCartService cartService;

    @GetMapping("/list")
    public AjaxResult getCartList()
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        cartService.deleteInvalidTCartByUserId(userId);
        TCart query = new TCart();
        query.setUserId(userId);
        List<TCart> list = cartService.selectTCartList(query);
        return AjaxResult.success(list);
    }

    @PostMapping("/add")
    public AjaxResult addToCart(@RequestBody TCart cart)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (userId == null || cart.getProductId() == null || cart.getQuantity() == null || cart.getQuantity() < 1)
        {
            return AjaxResult.error("购物车参数不完整");
        }
        cart.setUserId(userId);

        if (cart.getSpec() == null)
        {
            cart.setSpec("");
        }

        TCart query = new TCart();
        query.setUserId(cart.getUserId());
        query.setProductId(cart.getProductId());
        query.setSpec(cart.getSpec());
        List<TCart> existList = cartService.selectTCartList(query);

        if (existList != null && !existList.isEmpty())
        {
            TCart existCart = existList.get(0);
            existCart.setQuantity(existCart.getQuantity() + cart.getQuantity());
            cartService.updateTCart(existCart);
            return AjaxResult.success("已更新购物车数量");
        }

        cartService.insertTCart(cart);
        return AjaxResult.success("已添加到购物车");
    }

    @PutMapping("/update")
    public AjaxResult updateCart(@RequestBody TCart cart)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        if (cart.getCartId() == null || userId == null || cart.getQuantity() == null || cart.getQuantity() < 1)
        {
            return AjaxResult.error("购物车参数不完整");
        }

        TCart existCart = cartService.selectTCartByCartId(cart.getCartId());
        if (existCart == null || !userId.equals(existCart.getUserId()))
        {
            return AjaxResult.error("购物车记录不存在");
        }

        existCart.setQuantity(cart.getQuantity());
        if (cart.getSpec() != null)
        {
            existCart.setSpec(cart.getSpec());
        }

        return toAjax(cartService.updateTCart(existCart));
    }

    @DeleteMapping("/{cartId}")
    public AjaxResult deleteCart(@PathVariable Long cartId)
    {
        Long userId = WxUserAuthContext.getCurrentUserId();
        TCart existCart = cartService.selectTCartByCartId(cartId);
        if (existCart == null || !userId.equals(existCart.getUserId()))
        {
            return AjaxResult.error("购物车记录不存在");
        }
        return toAjax(cartService.deleteTCartByCartId(cartId));
    }

    @DeleteMapping("/clear/{userId}")
    public AjaxResult clearCart(@PathVariable Long userId)
    {
        Long currentUserId = WxUserAuthContext.getCurrentUserId();
        TCart query = new TCart();
        query.setUserId(currentUserId);
        List<TCart> list = cartService.selectTCartList(query);
        for (TCart item : list)
        {
            cartService.deleteTCartByCartId(item.getCartId());
        }
        return AjaxResult.success("购物车已清空");
    }
}
