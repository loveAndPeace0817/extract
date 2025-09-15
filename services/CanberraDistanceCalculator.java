package com.demo.extract.services;

import com.demo.extract.DTO.OrderTimeSeries;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CanberraDistanceCalculator {

    /**
     * 计算两个OrderTimeSeries的完整Canberra距离
     * @param s1 第一个时间序列
     * @param s2 第二个时间序列
     * @param type 特征类型 (1: values, 2: close, 3: open, 4: atr, 5: TH, 6: TL, 其他: 默认使用values)
     * @return Canberra距离值
     */
    public static double compute(OrderTimeSeries s1, OrderTimeSeries s2, Integer type) {
        List<Double> data1 = getDataByType(s1, type);
        List<Double> data2 = getDataByType(s2, type);

        // 确保两个序列长度相同
        int minLength = Math.min(data1.size(), data2.size());
        data1 = data1.subList(0, minLength);
        data2 = data2.subList(0, minLength);

        double distance = 0.0;
        for (int i = 0; i < minLength; i++) {
            double x = data1.get(i);
            double y = data2.get(i);
            double denominator = Math.abs(x) + Math.abs(y);
            
            // 避免除以零
            if (denominator > 0) {
                distance += Math.abs(x - y) / denominator;
            }
        }

        return distance;
    }

    /**
     * 计算两个OrderTimeSeries的部分Canberra距离（按比例截取数据）
     * @param s1 第一个时间序列（目标序列）
     * @param s2 第二个时间序列（参考序列）
     * @param ratio 截取比例 (0.0-1.0)
     * @param type 特征类型 (1: values, 2: close, 3: open, 4: atr, 5: TH, 6: TL, 其他: 默认使用values)
     * @return Canberra距离值
     */
    public static double computePartial(OrderTimeSeries s1, OrderTimeSeries s2, double ratio, Integer type) {
        // 计算目标序列和参考序列的截取点
        List<Double> fullData1 = getDataByType(s1, type);
        List<Double> fullData2 = getDataByType(s2, type);

        int splitPointS1 = (int) Math.round(fullData1.size() * ratio);
        int splitPointS2 = (int) Math.round(fullData2.size() * ratio);

        // 确保截取点至少为1
        splitPointS1 = Math.max(1, splitPointS1);
        splitPointS2 = Math.max(1, splitPointS2);

        // 截取数据
        List<Double> data1 = fullData1.subList(0, splitPointS1);
        List<Double> data2 = fullData2.subList(0, splitPointS2);

        // 确保两个序列长度相同
        int minLength = Math.min(data1.size(), data2.size());
        data1 = data1.subList(0, minLength);
        data2 = data2.subList(0, minLength);

        double distance = 0.0;
        for (int i = 0; i < minLength; i++) {
            double x = data1.get(i);
            double y = data2.get(i);
            double denominator = Math.abs(x) + Math.abs(y);
            
            // 避免除以零
            if (denominator > 0) {
                distance += Math.abs(x - y) / denominator;
            }
        }

        return distance;
    }

    /**
     * 计算收盘价的部分Canberra距离
     */
    public static double computePartialClose(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        return computePartial(s1, s2, ratio, 2);
    }

    /**
     * 计算开盘价的部分Canberra距离
     */
    public static double computePartialOpen(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        return computePartial(s1, s2, ratio, 3);
    }

    /**
     * 计算ATR的部分Canberra距离
     */
    public static double computePartialAtr(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        return computePartial(s1, s2, ratio, 4);
    }

    /**
     * 计算TH的部分Canberra距离
     */
    public static double computePartialTH(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        return computePartial(s1, s2, ratio, 5);
    }

    /**
     * 计算TL的部分Canberra距离
     */
    public static double computePartialTL(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        return computePartial(s1, s2, ratio, 6);
    }

    /**
     * 根据类型获取相应的数据
     */
    private static List<Double> getDataByType(OrderTimeSeries series, Integer type) {
        if (series == null) {
            throw new IllegalArgumentException("OrderTimeSeries cannot be null");
        }

        switch (type) {
            case 1:
                return Arrays.stream(series.getValues())
                        .boxed()
                        .collect(Collectors.toList());
            case 2:
                return Arrays.stream(series.getClose())
                        .boxed()
                        .collect(Collectors.toList());
            case 3:
                return Arrays.stream(series.getOpen())
                        .boxed()
                        .collect(Collectors.toList());
            case 4:
                return Arrays.stream(series.getAtr())
                        .boxed()
                        .collect(Collectors.toList());
            case 5:
                return Arrays.stream(series.getTH())
                        .boxed()
                        .collect(Collectors.toList());
            case 6:
                return Arrays.stream(series.getTL())
                        .boxed()
                        .collect(Collectors.toList());
            default:
                return Arrays.stream(series.getValues())
                        .boxed()
                        .collect(Collectors.toList());
        }
    }
}