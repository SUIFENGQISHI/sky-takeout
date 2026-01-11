package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportMapper {

    BigDecimal sumByOrdersTime(LocalDateTime beginTime, LocalDateTime endTime, Integer status);

    /**
     * 统计指定时间范围内的新增用户数
     */
    Integer countNewUsers(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 统计截止到指定时间的用户总数
     */
    Integer countTotalUsers(LocalDateTime endTime);

    /**
     * 统计指定时间范围内的订单数
     */
    Integer countOrders(LocalDateTime beginTime, LocalDateTime endTime, Integer status);

    /**
     * 查询销量Top10
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime, Integer status);
}
