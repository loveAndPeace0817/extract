package com.demo.extract.DTO;




import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Objects;





/**
 * 订单特征完整模型（与Python实现严格对齐）
 */
@Data
@Getter
@Builder
@AllArgsConstructor
public class OrderFeatures {
    // ==================== 基础统计特征 ====================
    private final String orderId;       // 订单ID
    private final int count;            // 数据点数量
    private final double mean;          // 平均值


    private final double std;           // 标准差
    private final double min;           // 最小值
    private final double max;           // 最大值
    private final double median;        // 中位数
    private final double q25;           // 25分位数
    private final double q75;           // 75分位数
    private final double skewness;      // 偏度
    private final double kurtosis;      // 峰度

    // ==================== 范围特征 ====================
    private final double range;         // 极差 (max-min)
    private final double firstValue;    // 第一个值
    private final double lastValue;     // 最后一个值
    private final double absMax;        // 绝对值的最大值
    private final double absMean;       // 绝对值的平均值

    // ==================== 分布特征 ====================
    private final long posCount;        // 正收益计数
    private final long negCount;        // 负收益计数
    private final long zeroCount;       // 零收益计数

    // ==================== 趋势特征 ====================
    private final Double trendSlope;    // 趋势斜率
    private final Double trendIntercept;// 趋势截距
    private final Double trendRValue;   // 趋势R值
    private final Double trendPValue;   // 趋势P值

    @Override
    public String toString() {
        return "OrderFeatures{" +
                "orderId='" + orderId + '\'' +
                ", count=" + count +
                ", mean=" + mean +
                ", std=" + std +
                ", min=" + min +
                ", max=" + max +
                ", median=" + median +
                ", q25=" + q25 +
                ", q75=" + q75 +
                ", skewness=" + skewness +
                ", kurtosis=" + kurtosis +
                ", range=" + range +
                ", firstValue=" + firstValue +
                ", lastValue=" + lastValue +
                ", absMax=" + absMax +
                ", absMean=" + absMean +
                ", posCount=" + posCount +
                ", negCount=" + negCount +
                ", zeroCount=" + zeroCount +
                ", trendSlope=" + trendSlope +
                ", trendIntercept=" + trendIntercept +
                ", trendRValue=" + trendRValue +
                ", trendPValue=" + trendPValue +
                '}';
    }

}
