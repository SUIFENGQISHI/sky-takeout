package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetMealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        // 查询总订单数
        Integer totalOrderCount = reportMapper.countOrders(begin, end, null);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;

        // 营业额（已完成订单）
        BigDecimal turnoverDecimal = reportMapper.sumByOrdersTime(begin, end, Orders.COMPLETED);
        Double turnover = turnoverDecimal == null ? 0.0 : turnoverDecimal.doubleValue();

        // 有效订单数（已完成）
        Integer validOrderCount = reportMapper.countOrders(begin, end, Orders.COMPLETED);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;

        Double unitPrice = 0.0;
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            // 订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            // 平均客单价
            unitPrice = turnover / validOrderCount;
        }

        // 新增用户数
        Integer newUsers = reportMapper.countNewUsers(begin, end);
        newUsers = newUsers == null ? 0 : newUsers;

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        // 待接单
        Integer waitingOrders = reportMapper.countOrders(begin, end, Orders.TO_BE_CONFIRMED);
        // 待派送
        Integer deliveredOrders = reportMapper.countOrders(begin, end, Orders.CONFIRMED);
        // 已完成
        Integer completedOrders = reportMapper.countOrders(begin, end, Orders.COMPLETED);
        // 已取消
        Integer cancelledOrders = reportMapper.countOrders(begin, end, Orders.CANCELLED);
        // 全部订单
        Integer allOrders = reportMapper.countOrders(begin, end, null);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders == null ? 0 : waitingOrders)
                .deliveredOrders(deliveredOrders == null ? 0 : deliveredOrders)
                .completedOrders(completedOrders == null ? 0 : completedOrders)
                .cancelledOrders(cancelledOrders == null ? 0 : cancelledOrders)
                .allOrders(allOrders == null ? 0 : allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
