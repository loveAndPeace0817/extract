package com.demo.extract.model;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * 增强版Hurst指数计算器，包含优化的Hurst指数计算方法和趋势强度判断方法
 */
public class EnhancedHurstCalculator {

    /**
     * 优化版Hurst指数计算方法，使用经典R/S分析
     * @param ts 时间序列数据
     * @return Hurst指数值
     */
    public static double calculateHurst(double[] ts) {
        if (ts.length < 10) {  // 增加最小序列长度要求
            return 0.5;
        }

        try {
            // 优化maxLag选择，根据序列长度自适应调整
            int maxLag = Math.min(30, ts.length / 3);  // 增加最大滞后值并改为ts.length/3
            List<Integer> lags = new ArrayList<>();
            List<Double> rsValues = new ArrayList<>();

            // 使用经典R/S分析方法
            for (int lag = 2; lag <= maxLag; lag++) {
                lags.add(lag);
                double[] rs = new double[ts.length / lag];

                for (int i = 0; i < rs.length; i++) {
                    int start = i * lag;
                    int end = Math.min(start + lag, ts.length);
                    double[] subset = Arrays.copyOfRange(ts, start, end);

                    // 计算均值
                    double mean = Arrays.stream(subset).average().orElse(0.0);
                    // 计算累积偏差
                    double[] cumDev = new double[subset.length];
                    cumDev[0] = subset[0] - mean;
                    for (int j = 1; j < subset.length; j++) {
                        cumDev[j] = cumDev[j-1] + subset[j] - mean;
                    }
                    // 计算极差
                    double range = Arrays.stream(cumDev).max().orElse(0.0) - 
                                  Arrays.stream(cumDev).min().orElse(0.0);
                    // 计算标准差
                    double stdDev = Math.sqrt(Arrays.stream(subset)
                            .map(v -> Math.pow(v - mean, 2))
                            .average().orElse(0.0));
                    // 计算R/S值
                    rs[i] = range / stdDev;
                }

                // 取平均值
                rsValues.add(Arrays.stream(rs).average().orElse(0.0));
            }

            if (lags.size() < 2) {
                return 0.5;
            }

            // 线性回归计算Hurst指数
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < lags.size(); i++) {
                regression.addData(Math.log(lags.get(i)), Math.log(rsValues.get(i)));
            }
            return regression.getSlope();
        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * 综合多指标判断趋势强度
     * @param values 数据序列
     * @param hurst Hurst指数
     * @return 趋势强度（强/中/弱）
     */
    public static String determineTrendStrength(double[] values, double hurst) {
        double meanReturn = Arrays.stream(values).average().orElse(0.0);
        double volatility = calculateVolatility(values);
        double slope = calculateSlope(values);

        // 综合多指标计算强度分数
        double strengthScore = Math.abs(slope) * 100 + (hurst - 0.5) * 200 + Math.abs(meanReturn) / (volatility + 1);

        if (strengthScore > 40) {
            return "强";
        } else if (strengthScore > 20) {
            return "中";
        } else {
            return "弱";
        }
    }

    /**
     * 判断Hurst指数对应的趋势持续性
     * @param hurst Hurst指数
     * @return 持续性（强/中/弱）
     */
    public static String determinePersistence(double hurst) {
        // 优化的Hurst指数判断阈值
        if (hurst > 0.68) {
            return "强";
        } else if (hurst > 0.52) {
            return "中";
        } else {
            return "弱";
        }
    }

    /**
     * 计算数据序列的斜率
     * @param values 数据序列
     * @return 斜率值
     */
    private static double calculateSlope(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < values.length; i++) {
            regression.addData(i, values[i]);
        }
        return regression.getSlope();
    }

    /**
     * 计算数据序列的波动率
     * @param values 数据序列
     * @return 波动率值
     */
    private static double calculateVolatility(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }
}