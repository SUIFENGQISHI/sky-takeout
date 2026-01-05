package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;

import java.util.List;


public interface DishService {
    Long saveWithFlavors(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    DishVO getEchoById(Long id);

    void update(DishDTO dishDTO);

    List<Dish> getDishListById(Long categoryId);

    void startOrStop(Integer status, Long id);
    /**
     * 条件查询菜品和口味
     * @param categoryId
     * @return
     */
    List<DishVO> listWithFlavor(Long categoryId);
}
