package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@Service
@Slf4j
@Api(tags = "菜品相关接口")
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return 菜品id
     */
    @Transactional
    public Long saveWithFlavors(DishDTO dishDTO) {
        //新增菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        log.info("保存菜品：{}", dishDTO);
        //获取菜品id
        Long dishId = dish.getId();
        //新增菜品口味，关联菜品
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
                dishFlavor.setId(null); // 确保不使用前端传来的ID
            });
            dishFlavorMapper.insertBatch(flavors);
        }
        return dishId;
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //是否能删除------状态为起售的不能删
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == 1) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //是否能删除------关联套餐的不能删
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //批量删除菜品,并删除相关联的口味
        dishMapper.deleteBatchByIds(ids);
        dishFlavorMapper.deleteBatchByIds(ids);
    }

    /**
     * 通过ID查询菜品信息
     */
    public DishVO getEchoById(Long id) {
        //获取菜品信息，和关联的分类名称
        DishVO dishVO = dishMapper.getEchoById(id);
        //获取关联的口味信息
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }


    /**
     * 修改菜品
     *
     * @param dishDTO
     * @return
     */
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //更新菜品表
        dishMapper.update(dish);
        //删除原关联的口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //重新插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
                dishFlavor.setId(null); // 确保不使用前端传来的ID
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类获取菜品数据
     */
    public List<Dish> getDishListById(Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> dishList = dishMapper.getDishListById(categoryId);
        return dishList;
    }
}
