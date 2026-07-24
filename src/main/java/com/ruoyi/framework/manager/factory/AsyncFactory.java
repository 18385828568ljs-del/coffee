package com.ruoyi.framework.manager.factory;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.AddressUtils;
import com.ruoyi.common.utils.LogUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.security.ShiroUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.project.monitor.online.domain.OnlineSession;
import com.ruoyi.project.monitor.online.domain.UserOnline;
import com.ruoyi.project.monitor.online.service.IUserOnlineService;
import eu.bitwalker.useragentutils.UserAgent;

/**
 * 异步工厂（产生任务用）
 * 
 * @author liuhulu
 *
 */
public class AsyncFactory
{
    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 同步session到数据库
     * 
     * @param session 在线用户会话
     * @return 任务task
     */
    public static TimerTask syncSessionToDb(final OnlineSession session)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                UserOnline online = new UserOnline();
                online.setSessionId(String.valueOf(session.getId()));
                online.setDeptName(session.getDeptName());
                online.setLoginName(session.getLoginName());
                online.setStartTimestamp(session.getStartTimestamp());
                online.setLastAccessTime(session.getLastAccessTime());
                online.setExpireTime(session.getTimeout());
                online.setIpaddr(session.getHost());
                online.setLoginLocation(AddressUtils.getRealAddressByIP(session.getHost()));
                online.setBrowser(session.getBrowser());
                online.setOs(session.getOs());
                online.setStatus(session.getStatus());
                online.setSession(session);
                SpringUtils.getBean(IUserOnlineService.class).saveOnline(online);

            }
        };
    }

    /**
     * 操作日志记录
     * 
     * @param title 操作标题
     * @param businessType 业务类型
     * @param operatorType 操作人类别
     * @param operName 操作人
     * @param operUrl 请求地址
     * @param operIp 请求IP
     * @param requestMethod 请求方法
     * @param method 调用方法
     * @param operParam 请求参数
     * @param jsonResult 响应结果
     * @param status 操作状态
     * @param errorMsg 错误信息
     * @return 任务task
     */
    public static TimerTask recordOper(final String title, final String businessType, final String operatorType,
            final String operName, final String operUrl, final String operIp, final String requestMethod,
            final String method, final String operParam, final String jsonResult, final Integer status,
            final String errorMsg)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                String operLocation = AddressUtils.getRealAddressByIP(operIp);
                sys_user_logger.info(
                        "后台操作 title={} businessType={} operatorType={} operName={} operUrl={} operIp={} operLocation={} requestMethod={} method={} status={} errorMsg={} operParam={} jsonResult={}",
                        title, businessType, operatorType, operName, operUrl, operIp, operLocation, requestMethod,
                        method, status, StringUtils.defaultString(errorMsg), StringUtils.defaultString(operParam),
                        StringUtils.defaultString(jsonResult));
            }
        };
    }

    /**
     * 记录登录信息
     * 
     * @param username 用户名
     * @param status 状态
     * @param message 消息
     * @param args 列表
     * @return 任务task
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message, final Object... args)
    {
        final UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        final String ip = ShiroUtils.getIp();
        return new TimerTask()
        {
            @Override
            public void run()
            {
                String address = AddressUtils.getRealAddressByIP(ip);
                StringBuilder s = new StringBuilder();
                s.append(LogUtils.getBlock(ip));
                s.append(address);
                s.append(LogUtils.getBlock(username));
                s.append(LogUtils.getBlock(status));
                s.append(LogUtils.getBlock(message));
                // 打印信息到日志
                sys_user_logger.info(s.toString(), args);
                // 获取客户端操作系统
                String os = userAgent.getOperatingSystem().getName();
                // 获取客户端浏览器
                String browser = userAgent.getBrowser().getName();
                String result = Constants.LOGIN_FAIL.equals(status) ? Constants.FAIL : Constants.SUCCESS;
                sys_user_logger.info("登录审计 loginName={} ip={} location={} browser={} os={} status={} message={}",
                        username, ip, address, browser, os, result, message);
            }
        };
    }
}
