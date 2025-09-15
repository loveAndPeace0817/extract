package com.demo.extract.services;

import com.demo.extract.util.StandardScaler;
import java.util.Arrays;

/**
 * 替代状态检测模型示例类
 * 用于展示如何通过其他可靠模型获取实际状态序列
 */
public class AlternativeStateDetector {

    /**
     * 使用移动平均线交叉策略识别状态
     * @param prices 价格序列
     * @param shortWindow 短期窗口大小
     * @param longWindow 长期窗口大小
     * @return 状态序列 (0: 下跌, 1: 上涨)
     */
    public int[] detectByMA(double[] prices, int shortWindow, int longWindow) {
        if (prices == null || prices.length < longWindow) {
            throw new IllegalArgumentException("价格序列长度必须大于长期窗口");
        }

        int[] states = new int[prices.length];
        double[] shortMA = calculateMA(prices, shortWindow);
        double[] longMA = calculateMA(prices, longWindow);

        // 初始化所有状态为0（下跌）
        Arrays.fill(states, 0);

        // 当短期均线上穿长期均线时，状态为1（上涨）
        for (int i = longWindow; i < prices.length; i++) {
            if (shortMA[i] > longMA[i] && shortMA[i-1] <= longMA[i-1]) {
                // 金叉：短期均线上穿长期均线
                for (int j = i; j < prices.length; j++) {
                    states[j] = 1;
                }
            } else if (shortMA[i] < longMA[i] && shortMA[i-1] >= longMA[i-1]) {
                // 死叉：短期均线下穿长期均线
                for (int j = i; j < prices.length; j++) {
                    states[j] = 0;
                }
            }
        }

        return states;
    }

    /**
     * 使用RSI指标识别状态
     * @param returns 收益序列
     * @param window RSI窗口大小
     * @param overboughtThreshold 超买阈值
     * @param oversoldThreshold 超卖阈值
     * @return 状态序列 (0: 超卖, 1: 正常, 2: 超买)
     */
    public int[] detectByRSI(double[] returns, int window, double overboughtThreshold, double oversoldThreshold) {
        if (returns == null || returns.length < window) {
            throw new IllegalArgumentException("收益序列长度必须大于窗口大小");
        }

        int[] states = new int[returns.length];
        double[] rsi = calculateRSI(returns, window);

        for (int i = 0; i < returns.length; i++) {
            if (i < window - 1) {
                states[i] = 1; // 前几个数据点设为正常状态
            } else {
                if (rsi[i] >= overboughtThreshold) {
                    states[i] = 2; // 超买
                } else if (rsi[i] <= oversoldThreshold) {
                    states[i] = 0; // 超卖
                } else {
                    states[i] = 1; // 正常
                }
            }
        }

        return states;
    }


    // 计算移动平均线
    private double[] calculateMA(double[] prices, int window) {
        double[] ma = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            if (i < window - 1) {
                ma[i] = prices[i]; // 前几个数据点使用原始价格
            } else {
                double sum = 0;
                for (int j = i - window + 1; j <= i; j++) {
                    sum += prices[j];
                }
                ma[i] = sum / window;
            }
        }
        return ma;
    }

    // 计算RSI指标
    private double[] calculateRSI(double[] returns, int window) {
        double[] rsi = new double[returns.length];
        double upSum = 0;
        double downSum = 0;

        // 计算第一个窗口的平均涨跌
        for (int i = 1; i < window; i++) {
            if (returns[i] > 0) {
                upSum += returns[i];
            } else {
                downSum -= returns[i]; // 取正值
            }
        }

        double avgUp = upSum / (window - 1);
        double avgDown = downSum / (window - 1);
        rsi[window - 1] = 100 - (100 / (1 + avgUp / avgDown));

        // 计算剩余的RSI值
        for (int i = window; i < returns.length; i++) {
            if (returns[i] > 0) {
                avgUp = (avgUp * (window - 1) + returns[i]) / window;
                avgDown = (avgDown * (window - 1)) / window;
            } else {
                avgUp = (avgUp * (window - 1)) / window;
                avgDown = (avgDown * (window - 1) - returns[i]) / window;
            }
            rsi[i] = 100 - (100 / (1 + avgUp / avgDown));
        }

        return rsi;
    }

    // 计算欧几里得距离
    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
}