package com.sky.controller.admin;

import com.sky.annotation.PermissionCheck;
import com.sky.dto.CouponDTO;
import com.sky.result.Result;
import com.sky.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/coupon")
@Api(tags = "优惠券管理接口")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @ApiOperation("新增优惠券活动")
    @PermissionCheck("coupon:create")
    public Result create(@RequestBody CouponDTO couponDTO) {
        couponService.createCoupon(couponDTO);
        return Result.success();
    }
}
