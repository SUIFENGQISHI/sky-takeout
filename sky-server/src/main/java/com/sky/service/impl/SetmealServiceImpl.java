package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {


    @Autowired
    private SetMealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("开始进行分页查询");
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        //保存套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        //保存关联菜品信息
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        setmealDishList.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        setmealDishMapper.insertBatch(setmealDishList);
    }


    /**
     * 根据id查询套餐和关联的菜品数据
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        //查询套餐信息和分类名称
        SetmealVO setmealVO = setmealMapper.getByIdWithCategoryName(id);

        //查询关联菜品
        List<SetmealDish> setmealDishList = setmealDishMapper.getSetmealDishBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void update(SetmealDTO setmealDTO) {
        log.info("修改套餐：{}", setmealDTO);
        //更新套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //批量删除套餐关联菜品数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        //批量插入新的套餐关联菜品数据
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        setmealDishList.forEach(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()));
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前套餐是否在售
        for (Long id : ids) {
            SetmealVO setmeal = setmealMapper.getByIdWithCategoryName(id);
            if (setmeal.getStatus() == 1) {
                //在售，不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //删除套餐信息
        setmealMapper.deleteBatchByIds(ids);

        //删除关联菜品信息
        setmealDishMapper.deleteBatchByIds(ids);

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //包含未起售的菜品的套餐不能起售
        if (status == 1) {
            List<SetmealDish> setmealDishList = setmealDishMapper.getSetmealDishBySetmealId(id);
            List<Long> dishIds = setmealDishList.stream().map(SetmealDish::getDishId).collect(toList());
            for(Long dishId: dishIds){
                if(dishMapper.getById(dishId).getStatus()==0){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
