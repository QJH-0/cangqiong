package com.sky.service;

import com.sky.dto.CouponDTO;
import com.sky.dto.CouponSeckillDTO;
import com.sky.vo.CouponVO;

import java.util.List;

public interface CouponService {

    void createCoupon(CouponDTO couponDTO);

    void seckill(CouponSeckillDTO couponSeckillDTO);

    List<CouponVO> listEnabledCoupons();
}
