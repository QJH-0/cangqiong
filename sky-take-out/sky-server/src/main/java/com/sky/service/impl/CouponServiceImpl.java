package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.CouponDTO;
import com.sky.dto.CouponSeckillDTO;
import com.sky.entity.Coupon;
import com.sky.entity.CouponReceiveRecord;
import com.sky.entity.CouponSeckillOrder;
import com.sky.exception.BaseException;
import com.sky.mapper.CouponMapper;
import com.sky.mapper.CouponReceiveRecordMapper;
import com.sky.mapper.CouponSeckillOrderMapper;
import com.sky.service.CouponSeckillScriptService;
import com.sky.service.CouponService;
import com.sky.vo.CouponVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final CouponReceiveRecordMapper couponReceiveRecordMapper;
    private final CouponSeckillOrderMapper couponSeckillOrderMapper;
    private final CouponSeckillScriptService couponSeckillScriptService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${sky.coupon.stock-key-prefix}")
    private String stockKeyPrefix;

    public CouponServiceImpl(CouponMapper couponMapper,
                             CouponReceiveRecordMapper couponReceiveRecordMapper,
                             CouponSeckillOrderMapper couponSeckillOrderMapper,
                             CouponSeckillScriptService couponSeckillScriptService,
                             StringRedisTemplate stringRedisTemplate) {
        this.couponMapper = couponMapper;
        this.couponReceiveRecordMapper = couponReceiveRecordMapper;
        this.couponSeckillOrderMapper = couponSeckillOrderMapper;
        this.couponSeckillScriptService = couponSeckillScriptService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public void createCoupon(CouponDTO couponDTO) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(couponDTO, coupon);
        LocalDateTime now = LocalDateTime.now();
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        couponMapper.insert(coupon);
        stringRedisTemplate.opsForValue().set(stockKeyPrefix + coupon.getId(), String.valueOf(coupon.getStock()));
        log.info("coupon activity created, couponId={}, stock={}", coupon.getId(), coupon.getStock());
    }

    @Override
    @Transactional
    public void seckill(CouponSeckillDTO couponSeckillDTO) {
        Long userId = BaseContext.getCurrentId();
        Coupon coupon = couponMapper.getById(couponSeckillDTO.getCouponId());
        if (coupon == null || !Coupon.ENABLED.equals(coupon.getStatus())) {
            throw new BaseException("Coupon not available");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new BaseException("Coupon activity not started");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new BaseException("Coupon activity ended");
        }

        int result = couponSeckillScriptService.seckill(coupon.getId(), userId, coupon.getLimitPerUser());
        if (result == 0) {
            throw new BaseException("Coupon stock not enough");
        }
        if (result < 0) {
            throw new BaseException("Duplicate coupon seckill");
        }
        if (couponSeckillOrderMapper.countByCouponIdAndUserId(coupon.getId(), userId) > 0) {
            throw new BaseException("Duplicate coupon seckill");
        }

        CouponSeckillOrder order = new CouponSeckillOrder();
        order.setCouponId(coupon.getId());
        order.setUserId(userId);
        order.setStatus(CouponSeckillOrder.CREATED);
        order.setCreateTime(now);
        couponSeckillOrderMapper.insert(order);

        CouponReceiveRecord record = new CouponReceiveRecord();
        record.setCouponId(coupon.getId());
        record.setUserId(userId);
        record.setReceiveTime(now);
        record.setSource("SECKILL");
        couponReceiveRecordMapper.insert(record);
        log.info("coupon seckill success, couponId={}, userId={}", coupon.getId(), userId);
    }

    @Override
    public List<CouponVO> listEnabledCoupons() {
        return couponMapper.listEnabled();
    }
}
