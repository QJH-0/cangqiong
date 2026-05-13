package com.sky.mapper;

import com.sky.entity.CouponSeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CouponSeckillOrderMapper {

    void insert(CouponSeckillOrder order);

    @Select("select count(1) from coupon_seckill_order where coupon_id = #{couponId} and user_id = #{userId}")
    int countByCouponIdAndUserId(Long couponId, Long userId);
}
