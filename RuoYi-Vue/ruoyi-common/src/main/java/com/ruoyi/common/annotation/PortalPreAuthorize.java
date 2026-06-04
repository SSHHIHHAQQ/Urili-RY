package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Seller/buyer portal permission guard.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PortalPreAuthorize
{
    /**
     * Portal terminal, for example seller or buyer.
     */
    public String terminal();

    /**
     * Permissions that must all be present.
     */
    public String[] hasPermi() default {};

    /**
     * Permissions where any one is enough.
     */
    public String[] hasAnyPermi() default {};
}
