package com.sky.service.impl;

import com.sky.mapper.PermissionMapper;
import com.sky.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 通过员工-角色-权限关联查询权限编码 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Set<String> listPermissionCodes(Long employeeId) {
        if (employeeId == null) {
            return Collections.emptySet();
        }
        List<String> permissionCodes = permissionMapper.listPermissionCodesByEmployeeId(employeeId);
        // 无关联权限时避免分配可变空集合
        if (CollectionUtils.isEmpty(permissionCodes)) {
            return Collections.emptySet();
        }
        // 同一权限可能经多角色重复出现，转为 Set 便于切面 contains 判断
        return new HashSet<>(permissionCodes);
    }
}
