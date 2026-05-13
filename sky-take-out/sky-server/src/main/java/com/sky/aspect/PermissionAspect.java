package com.sky.aspect;

import com.sky.annotation.PermissionCheck;
import com.sky.constant.IdentityConstant;
import com.sky.context.AuthContext;
import com.sky.context.BaseContext;
import com.sky.exception.BaseException;
import com.sky.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 基于 {@link com.sky.annotation.PermissionCheck} 的环绕通知：
 * 仅管理员身份校验通过后，再比对当前员工是否具备注解声明的权限编码。
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(permissionCheck)")
    public Object check(ProceedingJoinPoint joinPoint, PermissionCheck permissionCheck) throws Throwable {
        // 非管理端身份直接拒绝（细粒度权限仅对后台管理员生效）
        String identity = AuthContext.getCurrentIdentity();
        if (!IdentityConstant.ADMIN.equals(identity)) {
            throw new BaseException("Permission denied");
        }

        Long employeeId = BaseContext.getCurrentId();
        if (employeeId == null) {
            log.warn("permission denied, current employee id is null");
            throw new BaseException("Permission denied");
        }
        Set<String> permissionCodes = permissionService.listPermissionCodes(employeeId);
        if (!permissionCodes.contains(permissionCheck.value())) {
            log.warn("permission denied, employeeId={}, permission={}", employeeId, permissionCheck.value());
            throw new BaseException("Permission denied");
        }

        return joinPoint.proceed();
    }
}
