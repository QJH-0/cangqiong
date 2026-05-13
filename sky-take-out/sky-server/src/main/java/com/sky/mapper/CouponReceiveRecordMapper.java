package com.sky.mapper;

import com.sky.entity.CouponReceiveRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponReceiveRecordMapper {

    void insert(CouponReceiveRecord record);
}
