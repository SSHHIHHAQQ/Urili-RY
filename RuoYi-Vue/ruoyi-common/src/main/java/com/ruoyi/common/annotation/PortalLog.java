package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ruoyi.common.enums.BusinessType;

/**
 * Seller/buyer portal operation log annotation.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PortalLog
{
    /**
     * Portal terminal, for example seller or buyer.
     */
    public String terminal();

    /**
     * Module title.
     */
    public String title() default "";

    /**
     * Business operation type.
     */
    public BusinessType businessType() default BusinessType.OTHER;

    /**
     * Whether request parameters should be saved.
     */
    public boolean isSaveRequestData() default true;

    /**
     * Whether response data should be saved.
     */
    public boolean isSaveResponseData() default true;

    /**
     * Whether an endpoint can be audited before a portal session exists.
     */
    public boolean allowAnonymous() default false;

    /**
     * Request parameters to exclude.
     */
    public String[] excludeParamNames() default {};
}
