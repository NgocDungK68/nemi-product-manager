package com.nemi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckXRealIp {
    /**
     * Optional partner id to check against. If empty, aspect will attempt to
     * extract partner from request path like /webhook/v1/{partner}/...
     */
    String partner() default "";
}
