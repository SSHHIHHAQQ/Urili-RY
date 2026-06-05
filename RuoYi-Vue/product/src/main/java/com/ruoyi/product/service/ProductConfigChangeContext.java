package com.ruoyi.product.service;

import org.apache.commons.lang3.StringUtils;

/**
 * 当前商品配置变更来源上下文。
 */
public final class ProductConfigChangeContext
{
    public static final String SOURCE_PAGE = "PAGE";

    public static final String SOURCE_IMPORT = "IMPORT";

    private static final ThreadLocal<String> ACTION_SOURCE = new ThreadLocal<>();

    private ProductConfigChangeContext()
    {
    }

    public static String getActionSource()
    {
        return StringUtils.defaultIfBlank(ACTION_SOURCE.get(), SOURCE_PAGE);
    }

    public static Scope useActionSource(String actionSource)
    {
        String previous = ACTION_SOURCE.get();
        ACTION_SOURCE.set(StringUtils.defaultIfBlank(actionSource, SOURCE_PAGE));
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable
    {
        private final String previous;

        private Scope(String previous)
        {
            this.previous = previous;
        }

        @Override
        public void close()
        {
            if (previous == null)
            {
                ACTION_SOURCE.remove();
                return;
            }
            ACTION_SOURCE.set(previous);
        }
    }
}
