package com.ruoyi.project.coffee.decorator.context;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.security.ShiroUtils;
import com.ruoyi.framework.web.domain.AjaxResult;

@Component
public class TenantContextInterceptor implements HandlerInterceptor
{
    public static final String SESSION_MERCHANT_ID = "decorator.currentMerchantId";

    @Autowired
    private TenantContextService contextService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        Subject subject = ShiroUtils.getSubject();
        if (subject == null || subject.getPrincipal() == null)
        {
            writeError(response, 401, "请先登录");
            return false;
        }
        Session session = ShiroUtils.getSession();
        Object selected = session.getAttribute(SESSION_MERCHANT_ID);
        if (selected == null)
        {
            writeError(response, 409, "请先选择商家");
            return false;
        }
        try
        {
            Long merchantId = Long.valueOf(String.valueOf(selected));
            TenantContextHolder.set(contextService.resolve(ShiroUtils.getUserId(), merchantId));
            return true;
        }
        catch (NumberFormatException e)
        {
            session.removeAttribute(SESSION_MERCHANT_ID);
            writeError(response, 409, "商家上下文无效，请重新选择");
            return false;
        }
        catch (ServiceException e)
        {
            session.removeAttribute(SESSION_MERCHANT_ID);
            writeError(response, 403, e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex)
    {
        TenantContextHolder.clear();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(AjaxResult.error(message)));
    }
}
