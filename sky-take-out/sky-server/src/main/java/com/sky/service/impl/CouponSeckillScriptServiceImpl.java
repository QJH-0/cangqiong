package com.sky.service.impl;

import com.sky.service.CouponSeckillScriptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CouponSeckillScriptServiceImpl implements CouponSeckillScriptService {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> script;

    @Value("${sky.coupon.stock-key-prefix}")
    private String stockKeyPrefix;

    @Value("${sky.coupon.users-key-prefix}")
    private String usersKeyPrefix;

    public CouponSeckillScriptServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("lua/coupon_seckill.lua"));
        this.script.setResultType(Long.class);
    }

    @Override
    public int seckill(Long couponId, Long userId, int limitPerUser) {
        Long result = stringRedisTemplate.execute(
                script,
                Arrays.asList(stockKeyPrefix + couponId, usersKeyPrefix + couponId),
                String.valueOf(userId),
                String.valueOf(limitPerUser)
        );
        return result == null ? 0 : result.intValue();
    }
}
