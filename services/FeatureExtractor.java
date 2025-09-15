package com.demo.extract.services;




import com.demo.extract.DTO.OrderFeatures;
import com.demo.extract.DTO.OrderTimeSeries;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 特征提取服务
 * 从时间序列数据中提取统计特征
 */


/**
 * 特征提取服务（完整实现）
 */

public class FeatureExtractor {

    /**
     * 批量提取所有订单的特征
     */
    public List<OrderFeatures> extractAll(List<OrderTimeSeries> seriesList) {
        List<OrderFeatures> features = new ArrayList<>();
        for (OrderTimeSeries series : seriesList) {
            features.add(extract(series));
        }
        return features;
    }

    /**
     * 提取单个订单的特征（完整版）
     */
    public OrderFeatures extract(OrderTimeSeries series) {
        double[] values = series.getValues();
        int count = values.length;

        // 基础统计量
        double mean = StatUtils.mean(values);
        double std = new StandardDeviation().evaluate(values);
        double min = StatUtils.min(values);
        double max = StatUtils.max(values);
        double median = StatUtils.percentile(values, 50);
        double q25 = StatUtils.percentile(values, 25);
        double q75 = StatUtils.percentile(values, 75);
        double skewness = new Skewness().evaluate(values);
        double kurtosis = new Kurtosis().evaluate(values);
        double range = max - min;
        double firstValue = values[0];
        double lastValue = values[count - 1];
        double absMax = StatUtils.max(Arrays.stream(values).map(Math::abs).toArray());
        double absMean = StatUtils.mean(Arrays.stream(values).map(Math::abs).toArray());
        long posCount = Arrays.stream(values).filter(v -> v > 0).count();
        long negCount = Arrays.stream(values).filter(v -> v < 0).count();
        long zeroCount = Arrays.stream(values).filter(v -> v == 0).count();

        // 趋势特征
        Double slope = null, intercept = null, rValue = null, pValue = null;
        if (count > 1) {
            SimpleRegression regression = new SimpleRegression();
            double[] timestamps = series.getTimestamps();
            for (int i = 0; i < count; i++) {
                regression.addData(timestamps[i], values[i]);
            }
            slope = regression.getSlope();
            intercept = regression.getIntercept();
            rValue = regression.getR();
            pValue = regression.getSignificance();
        }

        return new OrderFeatures(
                series.getOrderId(),
                count,
                mean, std, min, max, median,
                q25, q75, skewness, kurtosis,
                range, firstValue, lastValue,
                absMax, absMean, posCount,
                negCount, zeroCount, slope,
                intercept, rValue, pValue
        );
    }

    // 私有工具方法
    private long countPositive(double[] values) {
        return Arrays.stream(values).filter(v -> v > 0).count();
    }

    private long countNegative(double[] values) {
        return Arrays.stream(values).filter(v -> v < 0).count();
    }
}
