package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "菜品相关接口")
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishServiceImpl dishService;


    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")

    public Result saveWithFlavors(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}",dishDTO);
        Long dishId = dishService.saveWithFlavors(dishDTO);
        return Result.success(dishId);
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除")
    public Result deleteBatch(@RequestParam List<Long> ids){
        log.info("批量删除：{}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询")
    public Result<DishVO> getEchoById(@PathVariable("id") Long id){
        log.info("根据id查询：{}",id);
        DishVO dishVO = dishService.getEchoById(id);
        return Result.success(dishVO);
    }
}

