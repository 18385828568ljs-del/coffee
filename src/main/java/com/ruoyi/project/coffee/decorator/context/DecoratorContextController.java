package com.ruoyi.project.coffee.decorator.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.security.ShiroUtils;
import com.ruoyi.framework.web.domain.AjaxResult;

@RestController
@RequestMapping("/coffee/decorator/context")
public class DecoratorContextController
{
    @Autowired
    private TenantContextService contextService;

    @GetMapping("/merchants")
    public AjaxResult merchants()
    {
        List<MerchantAccess> memberships = contextService.listMemberships(ShiroUtils.getUserId());
        return AjaxResult.success(memberships);
    }

    @PostMapping("/select")
    public AjaxResult select(@RequestBody Map<String, Object> body)
    {
        Long merchantId = parseLong(body == null ? null : body.get("merchantId"));
        try
        {
            TenantContext context = contextService.resolve(ShiroUtils.getUserId(), merchantId);
            ShiroUtils.getSession().setAttribute(TenantContextInterceptor.SESSION_MERCHANT_ID, merchantId);
            return AjaxResult.success(toResponse(context));
        }
        catch (ServiceException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/current")
    public AjaxResult current()
    {
        Session session = ShiroUtils.getSession();
        Long merchantId = parseLong(session.getAttribute(TenantContextInterceptor.SESSION_MERCHANT_ID));
        if (merchantId == null)
        {
            return AjaxResult.error("请先选择商家");
        }
        try
        {
            return AjaxResult.success(toResponse(contextService.resolve(ShiroUtils.getUserId(), merchantId)));
        }
        catch (ServiceException e)
        {
            session.removeAttribute(TenantContextInterceptor.SESSION_MERCHANT_ID);
            return AjaxResult.error(e.getMessage());
        }
    }

    private Map<String, Object> toResponse(TenantContext context)
    {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("merchantId", String.valueOf(context.getMerchantId()));
        result.put("role", context.getRole());
        result.put("storeScope", context.getStoreScope());
        result.put("storeIds", context.getStoreIds());
        result.put("permissions", contextService.permissionsFor(context.getRole()));
        return result;
    }

    private Long parseLong(Object value)
    {
        if (value == null) return null;
        try { return Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException e) { return null; }
    }
}
