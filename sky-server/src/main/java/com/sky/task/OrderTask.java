package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {


    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理订单状态超时
     */
//    @Scheduled(cron = "0 * * * * ? ")
    public void handleWithOrderTimeOut() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        //查询状态为待付款且创建时间超过15min的订单
        LocalDateTime lastTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> orders = orderMapper.getByStatusAndTime(Orders.UN_PAID, lastTime);

        //将订单状态设置为取消，并更新取消时间
        orders.forEach(order -> {
            order.setStatus(Orders.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("订单超时，取消订单");
            orderMapper.update(order);
        });
    }

//    @Scheduled(cron = "0 0 1 * * ? ")
    public void handleDeliverInProgress() {
        log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());
        //查询状态为派送中且创建时间在00点之前的前一天订单
        LocalDateTime lastTime = LocalDateTime.now().plusHours(-1);
        List<Orders> orders = orderMapper.getByStatusAndTime(Orders.DELIVERY_IN_PROGRESS, lastTime);
        //将订单状态设置为取消，并更新取消时间
        orders.forEach(order -> {
            order.setStatus(Orders.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("订单仍处于派送状态，取消订单");
            orderMapper.update(order);
        });
    }

}




