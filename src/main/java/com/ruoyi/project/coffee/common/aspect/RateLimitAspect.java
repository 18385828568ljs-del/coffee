package com.ruoyi.project.coffee.common.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.IpUtils;
import com.ruoyi.project.coffee.auth.WxUserAuthContext;
import com.ruoyi.project.coffee.common.annotation.RateLimit;

/**
 * 接口限流切面
 */
@Aspect
@Component
public class RateLimitAspect
{
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final Map<String, RateLimitCounter> RATE_LIMIT_CACHE = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) throws Throwable
    {
        String key = getCombineKey(rateLimit, point);
        RateLimitCounter counter = RATE_LIMIT_CACHE.computeIfAbsent(key, k -> new RateLimitCounter());

        long now = System.currentTimeMillis();
        long windowStart = counter.getWindowStart();
        int timeWindow = rateLimit.time() * 1000;

        if (now - windowStart > timeWindow)
        {
            counter.reset(now);
        }

        int currentCount = counter.increment();
        if (currentCount > rateLimit.count())
        {
            log.warn("接口限流触发: key={}, count={}, limit={}", key, currentCount, rateLimit.count());
            throw new ServiceException("访问过于频繁，请稍后再试");
        }

        cleanupExpiredCounters();
    }

    private String getCombineKey(RateLimit rateLimit, JoinPoint point)
    {
        StringBuilder key = new StringBuilder(rateLimit.key());
        key.append(":");

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        key.append(method.getDeclaringClass().getName()).append(".").append(method.getName());

        if (rateLimit.limitType() == RateLimit.LimitType.IP)
        {
            key.append(":").append(getIpAddress());
        }
        else if (rateLimit.limitType() == RateLimit.LimitType.USER)
        {
            Long userId = WxUserAuthContext.getCurrentUserId();
            if (userId != null)
            {
                key.append(":").append(userId);
            }
            else
            {
                key.append(":").append(getIpAddress());
            }
        }

        return key.toString();
    }

    private String getIpAddress()
    {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
        {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        return IpUtils.getIpAddr(request);
    }

    private void cleanupExpiredCounters()
    {
        if (RATE_LIMIT_CACHE.size() < 1000)
        {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = 5 * 60 * 1000L;

        RATE_LIMIT_CACHE.entrySet().removeIf(entry -> {
            RateLimitCounter counter = entry.getValue();
            return now - counter.getWindowStart() > expireTime;
        });
    }

    private static class RateLimitCounter
    {
        private volatile long windowStart;
        private AtomicInteger count;

        public RateLimitCounter()
        {
            this.windowStart = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        public long getWindowStart()
        {
            return windowStart;
        }

        public int increment()
        {
            return count.incrementAndGet();
        }

        public void reset(long newWindowStart)
        {
            this.windowStart = newWindowStart;
            this.count.set(0);
        }
    }
}
