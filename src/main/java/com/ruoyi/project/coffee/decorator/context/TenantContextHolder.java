package com.ruoyi.project.coffee.decorator.context;

/** Request-scoped tenant holder. Always clear it after the request completes. */
public final class TenantContextHolder
{
    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<TenantContext>();

    private TenantContextHolder() {}

    public static void set(TenantContext context)
    {
        HOLDER.set(context);
    }

    public static TenantContext get()
    {
        return HOLDER.get();
    }

    public static TenantContext require()
    {
        TenantContext context = HOLDER.get();
        if (context == null)
        {
            throw new IllegalStateException("Merchant tenant context is not available");
        }
        return context;
    }

    public static void clear()
    {
        HOLDER.remove();
    }
}
