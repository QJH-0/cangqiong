package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CouponReceiveRecord implements Serializable {

    private Long id;
    private Long couponId;
    private Long userId;
    private LocalDateTime receiveTime;
    private String source;
}
