package com.demo.extract.DTO;





import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bytedeco.opencv.presets.opencv_core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
//基础模型类

/**
 * 订单时间序列数据模型（修复版）
 * 包含完整的 getValueAtTime 实现
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderTimeSeries {
    private  String orderId;//订单id
    private  double[] timestamps;//持仓时常
    private  double[] values;//收益
    private  double[] close;//收盘价
    private  double[] open;//开盘价
    private  double[] atr;//
    private  double[] TH;//唐安琪通道上轨
    private  double[] TL;//唐安琪通道下轨
    private  String[]valueTime;//事实时间
    private Boolean targetOrder = false;
    private String action;//多空方向
    private Integer step;//最佳入场step
    public OrderTimeSeries(String orderId, double[] timestamps, double[] values,double[] close,double[] open,double[] atr,double[]TH,double[]TL,String[] valueTime) {
        if (timestamps == null || values == null) {
            throw new IllegalArgumentException("时间和收益数组不能为null");
        }
        if (timestamps.length != values.length) {
            throw new IllegalArgumentException("时间点和收益值数组长度必须相同");
        }
        this.orderId = orderId;
        this.timestamps = Arrays.copyOf(timestamps, timestamps.length);
        this.values = Arrays.copyOf(values, values.length);
        this.close = Arrays.copyOf(close, close.length);
        this.open = Arrays.copyOf(open, open.length);
        this.atr = Arrays.copyOf(atr,atr.length);
        this.TH = Arrays.copyOf(TH, TH.length);
        this.TL = Arrays.copyOf(TL,TL.length);
        this.valueTime = valueTime;
    }

    /**
     * 获取指定时间点的收益值（线性插值）
     * @param targetTime 目标时间点
     * @return 对应的收益值
     * @throws IllegalStateException 如果时间序列为空
     */
    public double getValueAtTime(double targetTime) {
        // 1. 检查精确匹配
        for (int i = 0; i < timestamps.length; i++) {
            if (Math.abs(timestamps[i] - targetTime) < 1e-6) {
                return values[i];
            }
        }

        // 2. 处理边界
        if (targetTime <= timestamps[0]) {
            return values[0];
        }
        if (targetTime >= timestamps[timestamps.length-1]) {
            return values[timestamps.length-1];
        }

        // 3. 二分查找插值区间
        int left = 0, right = timestamps.length - 1;
        while (left < right - 1) {
            int mid = left + (right - left) / 2;
            if (timestamps[mid] < targetTime) {
                left = mid;
            } else {
                right = mid;
            }
        }

        // 4. 线性插值
        double alpha = (targetTime - timestamps[left]) / (timestamps[right] - timestamps[left]);
        return values[left] + alpha * (values[right] - values[left]);
    }

    private int findClosestIndex(double targetTime) {
        int left = 0;
        int right = timestamps.length - 1;
        int closestIndex = 0;
        double minDiff = Math.abs(timestamps[0] - targetTime);

        // 二分查找最近点
        while (left <= right) {
            int mid = left + (right - left) / 2;
            double diff = Math.abs(timestamps[mid] - targetTime);

            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = mid;
            }

            if (timestamps[mid] < targetTime) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return closestIndex;
    }

    private double linearInterpolation(double targetTime, int baseIndex) {
        // 确定前后索引
        int before = (targetTime < timestamps[baseIndex]) ? baseIndex - 1 : baseIndex;
        int after = before + 1;

        // 计算插值权重
        double timeBefore = timestamps[before];
        double timeAfter = timestamps[after];
        double alpha = (targetTime - timeBefore) / (timeAfter - timeBefore);

        // 执行插值
        return values[before] + alpha * (values[after] - values[before]);
    }

    // --------------------------
    // 其他必要方法
    // --------------------------

    public String getOrderId() {
        return orderId;
    }

    public double[] getTimestamps() {
        return Arrays.copyOf(timestamps, timestamps.length);
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public int getLength() {
        return values.length;
    }

    /**
     * 获取部分数据（用于比例测试）
     */
    public double[] getPartialValues(double ratio) {
        if (ratio <= 0 || ratio > 1) {
            throw new IllegalArgumentException("比例必须在(0,1]范围内");
        }
        int splitPoint = (int) (values.length * ratio);
        return Arrays.copyOfRange(values, 0, splitPoint);
    }

    public Map<String, OrderTimeSeries> convertToMapWithMerge(List<OrderTimeSeries> orderTimeSeriesList) {
        return orderTimeSeriesList.stream()
                .collect(Collectors.toMap(
                        OrderTimeSeries::getOrderId,
                        order -> order,
                        (existing, replacement) -> existing  // 这里选择保留已存在的值
                ));
    }

    @Override
    public String toString() {
        return String.format("OrderTimeSeries[%s, points=%d]", orderId, values.length);
    }
}
