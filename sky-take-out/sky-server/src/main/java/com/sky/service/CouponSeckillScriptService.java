package com.sky.service;

public interface CouponSeckillScriptService {

    int seckill(Long couponId, Long userId, int limitPerUser);
}
