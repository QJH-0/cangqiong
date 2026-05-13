package com.sky.service;

import java.util.Set;

/** 员工细粒度权限查询 */
public interface PermissionService {

    /**
     * 根据员工 ID 查询其通过角色关联拥有的全部权限编码（去重后由实现类返回 Set）。
     *
     * @param employeeId 当前登录员工 ID，为 {@code null} 时返回空集合，不访问数据库
     * @return 权限编码集合，无角色/无权限时可为空集合
     */
    Set<String> listPermissionCodes(Long employeeId);
}
