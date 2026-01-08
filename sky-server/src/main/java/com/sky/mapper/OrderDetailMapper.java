package com.sky.mapper;


import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.result.PageResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetailsList
     */
    void insertBatch(List<OrderDetail> orderDetailsList);


    /**
     * 根据订单id查询订单明细
     * @param id
     * @return
     */
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);

    /**
     * 根据id删除订单数据
     * @param id
     */
    @Select("delete from order_detail where order_id = #{id}")
    void deleteById(Long id);
}
