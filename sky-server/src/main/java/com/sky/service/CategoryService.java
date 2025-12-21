
package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
public interface CategoryService {
    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 分类删除
     * @param categoryDTO
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 分类删除
     * @param id
     */
    void delete(Long id);

    /**
     * 分类启用禁用
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    void update(CategoryDTO categoryDTO);
}
