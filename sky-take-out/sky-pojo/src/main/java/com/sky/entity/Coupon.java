package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Coupon implements Serializable {

    public static final Integer DISABLED = 0;
    public static final Integer ENABLED = 1;

    private Long id;
    private String name;
    private String description;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer limitPerUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
