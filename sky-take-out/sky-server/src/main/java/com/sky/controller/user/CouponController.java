package com.sky.controller.user;

import com.sky.dto.CouponSeckillDTO;
import com.sky.result.Result;
import com.sky.service.CouponService;
import com.sky.vo.CouponVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/coupon")
@Api(tags = "用户优惠券接口")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/list")
    @ApiOperation("查询可用优惠券活动")
    public Result<List<CouponVO>> list() {
        return Result.success(couponService.listEnabledCoupons());
    }

    @PostMapping("/seckill")
    @ApiOperation("秒杀优惠券")
    public Result seckill(@RequestBody CouponSeckillDTO couponSeckillDTO) {
        couponService.seckill(couponSeckillDTO);
        return Result.success();
    }
}
