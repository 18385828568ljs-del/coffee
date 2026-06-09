package com.ruoyi.project.coffee.auth;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson.JSON;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.abucoder.wxuser.domain.AbucoderWxuser;

/**
 * 小程序接口鉴权拦截器。
 */
@Component
public class WxUserAuthInterceptor implements HandlerInterceptor
{
    @Autowired
    private WxUserTokenService wxUserTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException
    {
        AbucoderWxuser wxuser = wxUserTokenService.resolveUser(request);
        if (wxuser == null)
        {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        WxUserAuthContext.setCurrentUser(wxuser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        WxUserAuthContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        AjaxResult result = AjaxResult.error(message);
        result.put(AjaxResult.CODE_TAG, HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
