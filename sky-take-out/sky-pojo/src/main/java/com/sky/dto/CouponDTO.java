package com.sky.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponDTO {

    private Long id;
    private String name;
    private String description;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer limitPerUser;
}
