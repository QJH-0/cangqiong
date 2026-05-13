package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CouponSeckillOrder implements Serializable {

    public static final Integer CREATED = 1;

    private Long id;
    private Long couponId;
    private Long userId;
    private Integer status;
    private LocalDateTime createTime;
}
