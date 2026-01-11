package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.*;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */

    void insert(Orders orders);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 修改订单状态
     * @param orderStatus
     * @param orderPaidStatus
     * @param check_out_time
     * @param orderNumber
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where number=#{orderNumber}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, String orderNumber);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getByid(Long id);

    /**
     * 根据id删除订单数据
     * @param id
     */
    @Delete("delete from orders where id = #{id}")
    void deleteById(Long id);

    /**
     * 统计订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 修改订单状态
     * @param id
     * @param status
     */
    @Update("update orders set status = #{status} where id = #{id}")
    void updateStatusById(Long id, Integer status);

    /**
     * 批量查询订单
     * @param status
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{lastTime}")
    List<Orders> getByStatusAndTime(Integer status, LocalDateTime lastTime);
}
