package com.sky.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限校验：与 {@link com.sky.aspect.PermissionAspect} 配合使用，
 * {@link #value()} 为所需权限编码（如 employee:create）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionCheck {

    /** 权限编码，须与数据库中 permission.code 一致 */
    String value();
}
