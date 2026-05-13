package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@Api(tags = "菜品相关接口")
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品{}",dishDTO);
        //清理缓存
        String key = "dish"+"_"+dishDTO.getCategoryId();
        this.cleanCache(key);
        //缓存数据在查询的时候再缓存
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);//后绪步骤定义
        return Result.success(pageResult);
    }
    /**
     * 菜品批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品批量删除：{}", ids);
        dishService.deleteBatch(ids);
        //清理缓存
        this.cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("菜品回显")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("菜品回显");
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping()
    @ApiOperation("修改菜品")
    public Result uodate(@RequestBody DishDTO dishDTO){
        log.info("菜品修改");
        dishService.updateWithFlavor(dishDTO);
        //清理缓存
        this.cleanCache("dish_*");
        return Result.success();
    }
    @PostMapping("/status/{statue}")
    @ApiOperation("菜品停售、启售")
    public Result stopOrStart(@PathVariable Integer statue, Long id){
        dishService.stopOrStart(statue,id);
        //清理缓存
        this.cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }

    private void cleanCache(String pattern){
        //清除所有对应缓存
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
