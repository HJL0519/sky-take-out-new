package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImp implements ReportService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;


    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //存放从begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            //计算指定日期后一天的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额,营业额是状态已完成的订单金额的合计
            //获取date当天的开始时间(时分秒)和结束时间(时分秒)
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = (turnover == null ? 0.0:turnover);
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        //存放从begin到end范围内每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            //计算指定日期后一天的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        //存放每天新增用户数量和总用户数量
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //获取date当天的开始时间(时分秒)和结束时间(时分秒)
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end",endTime);

            //总用户数量
            Integer totalUser = userMapper.countByMap(map);
            totalUser = (totalUser == null ? 0 : totalUser);
            totalUserList.add(totalUser);

            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUser = (newUser == null ? 0 : newUser);
            newUserList.add(newUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        //计算并存储日期数据
        List<LocalDate> localDateList = new ArrayList<>();
        localDateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            localDateList.add(begin);
        }

        //每日订单数，以逗号分隔，例如：260,210,215
        List<Integer> orderCountList = new ArrayList<>();
        //每日有效订单数(状态为已完成)，以逗号分隔，例如：20,21,10
        List<Integer> vaildOrderCountList = new ArrayList<>();

        for (LocalDate date : localDateList) {
            //获取date当天的开始时间(时分秒)和结束时间(时分秒)
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map todayOrderMap = new HashMap<>();
            todayOrderMap.put("begin",beginTime);
            todayOrderMap.put("end",endTime);
            Integer todayOrder = orderMapper.countByMap(todayOrderMap);
            todayOrder = (todayOrder == null ? 0 : todayOrder);
            todayOrderMap.put("status",Orders.COMPLETED);
            Integer todayVaildOrder = orderMapper.countByMap(todayOrderMap);
            todayVaildOrder = (todayVaildOrder == null ? 0 : todayVaildOrder);

            orderCountList.add(todayOrder);
            vaildOrderCountList.add(todayVaildOrder);
        }

        //计算时间区间内的订单总数和有效订单数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer vaildOrderCount = vaildOrderCountList.stream().reduce(Integer::sum).get();

        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){
            orderCompletionRate = vaildOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(localDateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(vaildOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(vaildOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {

        //获取date当天的开始时间(时分秒)和结束时间(时分秒)
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end , LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(names,","))
                .numberList(StringUtils.join(numbers,","))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库，获取营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(dateEnd, LocalTime.MAX);
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);

        //通过poi将数据写入到excel中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板");

        try {
            //基于模板文件创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //填充数据--时间
            //获取sheet标签页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //获取行
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第四行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第五行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
