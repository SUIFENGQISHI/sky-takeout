package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<BigDecimal> turnoverList = new ArrayList<>();
        while (!begin.isAfter(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        Integer status = Orders.COMPLETED;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            BigDecimal tempturnover = reportMapper.sumByOrdersTime(beginTime, endTime, status);
            BigDecimal turnover = tempturnover == null ? BigDecimal.ZERO : tempturnover;
            turnoverList.add(turnover);
        }
        String dateListStr = StringUtils.join(dateList, ",");
        String turnoverListStr = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        while (!begin.isAfter(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 新增用户数
            Integer newUsers = reportMapper.countNewUsers(beginTime, endTime);
            newUserList.add(newUsers == null ? 0 : newUsers);
            // 用户总数
            Integer totalUsers = reportMapper.countTotalUsers(endTime);
            totalUserList.add(totalUsers == null ? 0 : totalUsers);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        while (!begin.isAfter(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 每日订单数
            Integer orderCount = reportMapper.countOrders(beginTime, endTime, null);
            orderCountList.add(orderCount == null ? 0 : orderCount);
            // 每日有效订单数（已完成）
            Integer validOrderCount = reportMapper.countOrders(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validOrderCount == null ? 0 : validOrderCount);
        }

        // 计算总数
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        Integer validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        // 订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量Top10统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = reportMapper.getSalesTop10(beginTime, endTime, Orders.COMPLETED);

        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) throws IOException {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/template.xlsx");
        if (in == null) {
            throw new RuntimeException("模板文件不存在");
        }
        XSSFWorkbook excel = new XSSFWorkbook(in);
        Sheet sheet = excel.getSheetAt(0);
        sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "~" + end);
        XSSFRow row_3 = (XSSFRow) sheet.getRow(3);
        row_3.getCell(2).setCellValue(businessData.getTurnover());
        row_3.getCell(4).setCellValue(businessData.getOrderCompletionRate());
        row_3.getCell(6).setCellValue(businessData.getNewUsers());
        XSSFRow row_4 = (XSSFRow) sheet.getRow(4);
        row_4.getCell(2).setCellValue(businessData.getValidOrderCount());
        row_4.getCell(4).setCellValue(businessData.getUnitPrice());
        for (int i = 0; i < 30; i++) {
            XSSFRow row = (XSSFRow) sheet.getRow(7 + i);
            LocalDate date = begin.plusDays(i);
            BusinessDataVO dateValue = workspaceService.getBusinessData(
                    LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(dateValue.getTurnover());
            row.getCell(3).setCellValue(dateValue.getValidOrderCount());
            row.getCell(4).setCellValue(dateValue.getOrderCompletionRate());
            row.getCell(5).setCellValue(dateValue.getUnitPrice());
            row.getCell(6).setCellValue(dateValue.getNewUsers());
        }
        ServletOutputStream out = response.getOutputStream();
        excel.write(out);
        excel.close();
        in.close();
        out.close();


    }
}
