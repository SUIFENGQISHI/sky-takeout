package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.EmployeeMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private SetMealMapper setMealMapper;


    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        long total = page.getTotal();
        List<Category> records = page.getResult();
        return new PageResult(total, records);
    }
    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO){
        Category category = Category.builder().
                type(categoryDTO.getType()).
                name(categoryDTO.getName()).
                sort(categoryDTO.getSort()).
                status(StatusConstant.ENABLE).
                build();

        categoryMapper.insert(category);
    }

    /**
     * 分类删除
     * @param id
     */
    public void delete(Long id) {
        //查询当前分类是否关联了菜品
        Integer dishCount = dishMapper.countByCategoryId(id);
        //查询当前分类是否关联了套餐
        Integer setMealCount = setMealMapper.countByCategoryId(id);
        
        //判断是否有关联菜品或套餐
        if (dishCount > 0) {
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        
        if (setMealCount > 0) {
            //当前分类下有套餐，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        
        //删除分类
        categoryMapper.delete(id);
    }

    /**
     * 分类编辑
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = Category.builder().
                id(categoryDTO.getId()).
                type(categoryDTO.getType()).
                name(categoryDTO.getName()).
                sort(categoryDTO.getSort()).
                status(StatusConstant.ENABLE).
                build();
        categoryMapper.update(category);
    }
    /**
     * 分类禁用/启用
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder().
                id(id).
                status(status).
                updateTime(LocalDateTime.now()).
                updateUser(BaseContext.getCurrentId()).
                build();
        categoryMapper.update(category);
    }
}