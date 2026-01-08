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


    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
