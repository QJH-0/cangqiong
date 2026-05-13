package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper {

    /** 员工经角色关联的权限编码列表（可能含重复） */
    List<String> listPermissionCodesByEmployeeId(Long employeeId);
}
