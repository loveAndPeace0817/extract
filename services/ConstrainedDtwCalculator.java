package com.demo.extract.services;

import com.demo.extract.DTO.OrderTimeSeries;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConstrainedDtwCalculator {

    /**
     * 计算两个OrderTimeSeries的完整带约束DTW距离（Sakoe-Chiba带）
     * @param s1 第一个时间序列
     * @param s2 第二个时间序列
     * @param windowSize 窗口大小（时间约束范围）
     * @param type 特征类型 (1: values, 2: close, 3: open, 其他: 默认使用values)
     * @return 带约束的DTW距离值
     */
    public static double compute(OrderTimeSeries s1, OrderTimeSeries s2, int windowSize, Integer type) {
        List<Double> data1 = getDataByType(s1, type);
        List<Double> data2 = getDataByType(s2, type);

        int n = data1.size();
        int m = data2.size();

        // 初始化距离矩阵
        double[][] dtw = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dtw[i], Double.POSITIVE_INFINITY);
        }
        dtw[0][0] = 0;

        // 计算带窗口约束的DTW
        for (int i = 1; i <= n; i++) {
            // 窗口约束：只处理j在[i-windowSize, i+windowSize]范围内的点
            int startJ = Math.max(1, i - windowSize);
            int endJ = Math.min(m, i + windowSize);

            for (int j = startJ; j <= endJ; j++) {
                double cost = Math.abs(data1.get(i - 1) - data2.get(j - 1));
                dtw[i][j] = cost + Math.min(
                    Math.min(dtw[i - 1][j], dtw[i][j - 1]),
                    dtw[i - 1][j - 1]
                );
            }
        }

        return dtw[n][m];
    }

    /**
     * 计算两个OrderTimeSeries的部分带约束DTW距离（按比例截取数据）
     * @param s1 第一个时间序列（目标序列）
     * @param s2 第二个时间序列（参考序列）
     * @param ratio 截取比例 (0.0-1.0)
     * @param windowSize 窗口大小（时间约束范围）
     * @param type 特征类型 (1: values, 2: close, 3: open, 其他: 默认使用values)
     * @return 带约束的DTW距离值
     */
    public static double computePartial(OrderTimeSeries s1, OrderTimeSeries s2, double ratio, int windowSize, Integer type) {
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

        int n = data1.size();
        int m = data2.size();

        // 初始化距离矩阵
        double[][] dtw = new double[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dtw[i], Double.POSITIVE_INFINITY);
        }
        dtw[0][0] = 0;

        // 计算带窗口约束的DTW
        for (int i = 1; i <= n; i++) {
            // 窗口约束：只处理j在[i-windowSize, i+windowSize]范围内的点
            int startJ = Math.max(1, i - windowSize);
            int endJ = Math.min(m, i + windowSize);

            for (int j = startJ; j <= endJ; j++) {
                double cost = Math.abs(data1.get(i - 1) - data2.get(j - 1));
                dtw[i][j] = cost + Math.min(
                    Math.min(dtw[i - 1][j], dtw[i][j - 1]),
                    dtw[i - 1][j - 1]
                );
            }
        }

        return dtw[n][m];
    }

    /**
     * 计算收盘价的部分带约束DTW距离
     */
    public static double computePartialClose(OrderTimeSeries s1, OrderTimeSeries s2, double ratio, int windowSize) {
        return computePartial(s1, s2, ratio, windowSize, 2);
    }

    /**
     * 计算开盘价的部分带约束DTW距离
     */
    public static double computePartialOpen(OrderTimeSeries s1, OrderTimeSeries s2, double ratio, int windowSize) {
        return computePartial(s1, s2, ratio, windowSize, 3);
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
                return Arrays.stream(series.getValues())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            case 2:
                return Arrays.stream(series.getClose())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            case 3:
                return Arrays.stream(series.getOpen())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            case 4:
                return Arrays.stream(series.getAtr())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            case 5:
                return Arrays.stream(series.getTH())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            case 6:
                return Arrays.stream(series.getTL())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
            default:
                return Arrays.stream(series.getValues())  // 将double[]转为DoubleStream
                        .boxed()         // 将double转为Double
                        .collect(Collectors.toList());
        }
    }
}