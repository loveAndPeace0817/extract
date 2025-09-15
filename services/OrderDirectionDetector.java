package com.demo.extract.services;

import com.demo.extract.DTO.OrderTimeSeries;

/**
 * 订单方向检测器，用于判断订单是多单还是空单
 */
public class OrderDirectionDetector {
    
    /**
     * 判断订单是多单还是空单（简化版，只比较特定索引点的值）
     * @param orderTimeSeries 订单时间序列数据
     * @return "多单" 或 "空单"，无法判断时返回"未知"
     */
    public static String detectOrderDirection(OrderTimeSeries orderTimeSeries) {
        if (orderTimeSeries == null) {
            throw new IllegalArgumentException("订单时间序列数据不能为空");
        }
        
        double[] values = orderTimeSeries.getValues();
        double[] close = orderTimeSeries.getClose();
        
        // 确保数组不为空且长度足够（至少有索引10的元素）
        if (values == null || close == null || values.length < 11 || close.length < 11) {
            return "未知";
        }
        
        // 比较索引2和10处的收盘价和收益值
        int index1 = 2;
        int index2 = 10;
        
        double closeAt1 = close[index1];
        double closeAt2 = close[index2];
        double valueAt1 = values[index1];
        double valueAt2 = values[index2];
        
        // 判断逻辑：
        // 1. 收益持续升高(valueAt2 > valueAt1)且收盘价持续上升(closeAt2 > closeAt1) → 多单
        // 2. 收益持续升高(valueAt2 > valueAt1)且收盘价持续下降(closeAt2 < closeAt1) → 空单
        
        if (valueAt2 > valueAt1) {
            // 收益持续升高
            if (closeAt2 > closeAt1) {
                // 收盘价持续上升 → 多单
                return "多单";
            } else if (closeAt2 < closeAt1) {
                // 收盘价持续下降 → 空单
                return "空单";
            }
        }
        
        return "未知";
    }
}