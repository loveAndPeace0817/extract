package com.demo.extract.services;


import com.demo.extract.DTO.OrderFeatures;
import com.demo.extract.DTO.OrderTimeSeries;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.*;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.ibatis.annotations.Param;

import java.util.Arrays;

public class FeatureService {
    public OrderFeatures extractFeatures(OrderTimeSeries series,Integer type) {
        double[] values = series.getValues();
        switch (type){
            case 1:
                values = series.getValues();
                break;

            case 2:
                values = series.getClose();
                break;

            case 3:
                values = series.getOpen();
                break;
            case 4:
                values = series.getAtr();
                break;
            case 5:
                values = series.getTH();
                break;
            case 6:
                values = series.getTL();
                break;
        }

        double[] timestamps = series.getTimestamps();

        // 基础统计量
        double mean = StatUtils.mean(values);
        double std = new StandardDeviation().evaluate(values);
        double skewness = new Skewness().evaluate(values);
        double kurtosis = new Kurtosis().evaluate(values);

        // 趋势特征
        Double slope = null, intercept = null, rValue = null, pValue = null;
        if (values.length > 1) {
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < values.length; i++) {
                regression.addData(timestamps[i], values[i]);
            }
            slope = regression.getSlope();
            intercept = regression.getIntercept();
            rValue = regression.getR();
            pValue = regression.getSignificance();
        }

        return OrderFeatures.builder()
                .orderId(series.getOrderId())
                .count(values.length)
                .mean(mean)
                .std(std)
                .min(StatUtils.min(values))
                .max(StatUtils.max(values))
                .median(StatUtils.percentile(values, 50))
                .q25(StatUtils.percentile(values, 25))
                .q75(StatUtils.percentile(values, 75))
                .skewness(skewness)
                .kurtosis(kurtosis)
                .range(StatUtils.max(values) - StatUtils.min(values))
                .firstValue(values[0])
                .lastValue(values[values.length-1])
                .absMax(StatUtils.max(Arrays.stream(values).map(Math::abs).toArray()))
                .absMean(StatUtils.mean(Arrays.stream(values).map(Math::abs).toArray()))
                .posCount(Arrays.stream(values).filter(v -> v > 0).count())
                .negCount(Arrays.stream(values).filter(v -> v < 0).count())
                .zeroCount(Arrays.stream(values).filter(v -> v == 0).count())
                .trendSlope(slope)
                .trendIntercept(intercept)
                .trendRValue(rValue)
                .trendPValue(pValue)
                .build();
    }

    public OrderFeatures extractClose(OrderTimeSeries series) {
        double[] values = series.getClose();
        double[] timestamps = series.getTimestamps();

        // 基础统计量
        double mean = StatUtils.mean(values);
        double std = new StandardDeviation().evaluate(values);
        double skewness = new Skewness().evaluate(values);
        double kurtosis = new Kurtosis().evaluate(values);

        // 趋势特征
        Double slope = null, intercept = null, rValue = null, pValue = null;
        if (values.length > 1) {
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < values.length; i++) {
                regression.addData(timestamps[i], values[i]);
            }
            slope = regression.getSlope();
            intercept = regression.getIntercept();
            rValue = regression.getR();
            pValue = regression.getSignificance();
        }

        return OrderFeatures.builder()
                .orderId(series.getOrderId())
                .count(values.length)
                .mean(mean)
                .std(std)
                .min(StatUtils.min(values))
                .max(StatUtils.max(values))
                .median(StatUtils.percentile(values, 50))
                .q25(StatUtils.percentile(values, 25))
                .q75(StatUtils.percentile(values, 75))
                .skewness(skewness)
                .kurtosis(kurtosis)
                .range(StatUtils.max(values) - StatUtils.min(values))
                .firstValue(values[0])
                .lastValue(values[values.length-1])
                .absMax(StatUtils.max(Arrays.stream(values).map(Math::abs).toArray()))
                .absMean(StatUtils.mean(Arrays.stream(values).map(Math::abs).toArray()))
                .posCount(Arrays.stream(values).filter(v -> v > 0).count())
                .negCount(Arrays.stream(values).filter(v -> v < 0).count())
                .zeroCount(Arrays.stream(values).filter(v -> v == 0).count())
                .trendSlope(slope)
                .trendIntercept(intercept)
                .trendRValue(rValue)
                .trendPValue(pValue)
                .build();
    }

    public OrderFeatures extractOpen(OrderTimeSeries series) {
        double[] values = series.getOpen();
        double[] timestamps = series.getTimestamps();

        // 基础统计量
        double mean = StatUtils.mean(values);
        double std = new StandardDeviation().evaluate(values);
        double skewness = new Skewness().evaluate(values);
        double kurtosis = new Kurtosis().evaluate(values);

        // 趋势特征
        Double slope = null, intercept = null, rValue = null, pValue = null;
        if (values.length > 1) {
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < values.length; i++) {
                regression.addData(timestamps[i], values[i]);
            }
            slope = regression.getSlope();
            intercept = regression.getIntercept();
            rValue = regression.getR();
            pValue = regression.getSignificance();
        }

        return OrderFeatures.builder()
                .orderId(series.getOrderId())
                .count(values.length)
                .mean(mean)
                .std(std)
                .min(StatUtils.min(values))
                .max(StatUtils.max(values))
                .median(StatUtils.percentile(values, 50))
                .q25(StatUtils.percentile(values, 25))
                .q75(StatUtils.percentile(values, 75))
                .skewness(skewness)
                .kurtosis(kurtosis)
                .range(StatUtils.max(values) - StatUtils.min(values))
                .firstValue(values[0])
                .lastValue(values[values.length-1])
                .absMax(StatUtils.max(Arrays.stream(values).map(Math::abs).toArray()))
                .absMean(StatUtils.mean(Arrays.stream(values).map(Math::abs).toArray()))
                .posCount(Arrays.stream(values).filter(v -> v > 0).count())
                .negCount(Arrays.stream(values).filter(v -> v < 0).count())
                .zeroCount(Arrays.stream(values).filter(v -> v == 0).count())
                .trendSlope(slope)
                .trendIntercept(intercept)
                .trendRValue(rValue)
                .trendPValue(pValue)
                .build();
    }
}