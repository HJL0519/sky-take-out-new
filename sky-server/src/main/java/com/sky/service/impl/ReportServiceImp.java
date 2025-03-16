package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImp implements ReportService {

    private OrderMapper orderMapper;


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
}
