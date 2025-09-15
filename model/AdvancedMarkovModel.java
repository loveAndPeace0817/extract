package com.demo.extract.model;

import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.model.MarkovAnalysisResult;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高级马尔可夫模型实现
 * 扩展了原有的三段式马尔可夫模型，增加了更多的分析维度和优化算法
 */
public class AdvancedMarkovModel {
    private List<FinancialDataPoint> dataPoints;
    private List<Segment> segments;
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MIN_INTERVAL = 6; // 最小间隔为6个数据点(约30分钟)
    private static final double PEAK_PROMINENCE = 20.0; // 峰值突出度门槛
    private static final double PRICE_CHANGE_THRESHOLD = 30.0; // 价格变化阈值
    private static final long MIN_DURATION_MINUTES = 30; // 每个分段的最小持续时间(分钟)
    private static final int MAX_SEGMENTS = 5; // 最多保留的分段数量

    /**
     * 构造函数
     * @param values 收益值数组
     * @param valueTime 时间戳数组
     * @param orderId 订单ID
     */
    public AdvancedMarkovModel(double[] values, String[] valueTime, String orderId) {
        parseData(values, valueTime, orderId);
        this.segments = new ArrayList<>();
    }

    /**
     * 解析数据点
     */
    private void parseData(double[] values, String[] valueTime, String orderId) {
        dataPoints = new ArrayList<>();
        for (int i = 0; i < valueTime.length; i++) {
            double value = values[i];
            LocalDateTime timestamp = LocalDateTime.parse(valueTime[i], INPUT_FORMATTER);
            dataPoints.add(new FinancialDataPoint(value, timestamp.format(ISO_FORMATTER), orderId));
        }
    }

    /**
     * 安全的斜率计算
     */
    private double safeSlopeCalc(double[] values) {
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
     * 趋势类型判断
     */
    private TrendResult determineTrendType(double[] segmentValues) {
        if (segmentValues.length < 3) {
            return new TrendResult("数据不足", 0.5);
        }

        double slope = safeSlopeCalc(segmentValues);
        double volatility = calculateVolatility(segmentValues);
        double hurst = hurstExponent(segmentValues);

        // 基于波动率的动态阈值
        double trendThreshold = Math.max(0.10, Math.min(0.20, volatility / 300.0));
        
        if (Math.abs(slope) > trendThreshold) {
            return new TrendResult(slope > 0 ? "趋势上涨" : "趋势下跌", hurst);
        } else {
            return new TrendResult(volatility > 20 ? "宽幅震荡" : "窄幅震荡", hurst);
        }
    }

    /**
     * 计算波动率
     */
    private double calculateVolatility(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * 计算Hurst指数
     */
    private double hurstExponent(double[] ts) {
        if (ts.length < 5) {
            return 0.5;
        }

        try {
            int maxLag = Math.min(20, ts.length / 2);
            List<Integer> lags = new ArrayList<>();
            List<Double> tau = new ArrayList<>();

            for (int lag = 2; lag < maxLag; lag++) {
                lags.add(lag);
                double[] diffs = new double[ts.length - lag];
                for (int i = 0; i < ts.length - lag; i++) {
                    diffs[i] = ts[i + lag] - ts[i];
                }
                tau.add(Math.sqrt(Arrays.stream(diffs).map(d -> d * d).average().orElse(0.0)));
            }

            if (lags.size() < 2) {
                return 0.5;
            }

            // 线性回归计算Hurst指数
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < lags.size(); i++) {
                regression.addData(Math.log(lags.get(i)), Math.log(tau.get(i)));
            }
            return regression.getSlope();
        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * 自动分段
     */
    public void autoSegment() {
        if (dataPoints.isEmpty()) {
            return;
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        // 识别峰值和谷值
        List<Integer> peaks = findPeaks(values, PEAK_PROMINENCE);
        List<Integer> valleys = findPeaks(invertArray(values), PEAK_PROMINENCE);

        // 合并并排序所有转折点
        Set<Integer> turningPoints = new TreeSet<>();
        turningPoints.addAll(peaks);
        turningPoints.addAll(valleys);
        turningPoints.add(0);
        turningPoints.add(values.length - 1);

        // 筛选显著转折点
        List<Integer> significantPoints = new ArrayList<>();
        int lastPoint = 0;
        for (int point : turningPoints) {
            if (point > 0 && point < values.length - 1) {
                double priceChange = Math.abs(values[point] - values[lastPoint]);
                if (priceChange > PRICE_CHANGE_THRESHOLD) {
                    significantPoints.add(point);
                    lastPoint = point;
                }
            }
        }

        // 合并相邻过近的转折点
        List<Integer> mergedPoints = mergeClosePoints(significantPoints);

        // 基于时间筛选分段，确保每个分段至少有MIN_DURATION_MINUTES
        List<Integer> timeFilteredPoints = filterByDuration(mergedPoints);

        // 构建最终分段
        buildSegments(timeFilteredPoints, values.length - 1);
    }

    /**
     * 合并相邻过近的转折点
     */
    private List<Integer> mergeClosePoints(List<Integer> points) {
        List<Integer> mergedPoints = new ArrayList<>();
        for (int point : points) {
            if (mergedPoints.isEmpty() || point - mergedPoints.get(mergedPoints.size() - 1) >= MIN_INTERVAL) {
                mergedPoints.add(point);
            }
        }
        return mergedPoints;
    }

    /**
     * 基于持续时间筛选分段点
     */
    private List<Integer> filterByDuration(List<Integer> points) {
        List<Integer> timeFilteredPoints = new ArrayList<>();
        if (!points.isEmpty()) {
            timeFilteredPoints.add(points.get(0));
            LocalDateTime lastTime = LocalDateTime.parse(
                    dataPoints.get(points.get(0)).getTimestamp(), ISO_FORMATTER);

            for (int i = 1; i < points.size(); i++) {
                int point = points.get(i);
                LocalDateTime currentTime = LocalDateTime.parse(
                        dataPoints.get(point).getTimestamp(), ISO_FORMATTER);
                long duration = java.time.Duration.between(lastTime, currentTime).toMinutes();

                if (duration >= MIN_DURATION_MINUTES) {
                    timeFilteredPoints.add(point);
                    lastTime = currentTime;
                }
            }

            // 确保至少有一个分段
            if (timeFilteredPoints.size() < 2 && !points.isEmpty()) {
                timeFilteredPoints.clear();
                timeFilteredPoints.add(0);
                timeFilteredPoints.add(points.get(points.size() - 1));
            }
        }
        return timeFilteredPoints;
    }

    /**
     * 构建分段
     */
    private void buildSegments(List<Integer> points, int endIndex) {
        segments.clear();
        int start = 0;
        for (int point : points) {
            segments.add(new Segment(start, point));
            start = point;
        }
        segments.add(new Segment(start, endIndex));

        // 最多保留指定数量的分段
        if (segments.size() > MAX_SEGMENTS) {
            segments = segments.subList(0, MAX_SEGMENTS);
        }
    }

    /**
     * 寻找峰值
     */
    private List<Integer> findPeaks(double[] values, double prominence) {
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < values.length - 1; i++) {
            if (values[i] > values[i - 1] && values[i] > values[i + 1]) {
                // 计算突出度（简化实现）
                double leftMin = Arrays.stream(Arrays.copyOfRange(values, 0, i)).min().orElse(0.0);
                double rightMin = Arrays.stream(Arrays.copyOfRange(values, i + 1, values.length)).min().orElse(0.0);
                double peakProminence = values[i] - Math.max(leftMin, rightMin);
                if (peakProminence >= prominence) {
                    peaks.add(i);
                }
            }
        }
        return peaks;
    }

    /**
     * 反转数组（用于寻找谷值）
     */
    private double[] invertArray(double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = -array[i];
        }
        return result;
    }

    /**
     * 分析分段
     */
    public List<SegmentReport> analyzeSegments() {
        List<SegmentReport> reports = new ArrayList<>();
        if (segments.isEmpty() || dataPoints.isEmpty()) {
            return reports;
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        for (Segment segment : segments) {
            int start = segment.getStart();
            int end = segment.getEnd();
            double[] segmentValues = Arrays.copyOfRange(values, start, end + 1);

            LocalDateTime startTimestamp = LocalDateTime.parse(
                    dataPoints.get(start).getTimestamp(), ISO_FORMATTER);
            LocalDateTime endTimestamp = LocalDateTime.parse(
                    dataPoints.get(end).getTimestamp(), ISO_FORMATTER);
            long duration = java.time.Duration.between(startTimestamp, endTimestamp).toMinutes();

            // 计算各种指标
            double meanReturn = Arrays.stream(segmentValues).average().orElse(0.0);
            double maxDrawdown = calculateMaxDrawdown(segmentValues);
            double volatility = calculateVolatility(segmentValues);
            TrendResult trendResult = determineTrendType(segmentValues);
            double hurst = trendResult.getHurst();

            String trendStrength = Math.abs(meanReturn) > 30 ? "强" : (Math.abs(meanReturn) > 15 ? "中" : "弱");

            reports.add(new SegmentReport(
                    startTimestamp.format(OUTPUT_FORMATTER),
                    endTimestamp.format(OUTPUT_FORMATTER),
                    duration,
                    trendResult.getType(),
                    Math.round(meanReturn * 10) / 10.0,
                    Math.round(maxDrawdown * 10) / 10.0,
                    Math.round(volatility * 10) / 10.0,
                    Math.round(hurst * 100) / 100.0,
                    trendStrength
            ));
        }
        return reports;
    }

    /**
     * 计算最大回撤
     */
    private double calculateMaxDrawdown(double[] values) {
        if (values.length == 0) return 0.0;

        double peak = values[0];
        double maxDrawdown = 0.0;

        for (double value : values) {
            if (value > peak) {
                peak = value;
            } else {
                double drawdown = (peak - value) / peak;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return maxDrawdown * 100; // 转换为百分比
    }

    /**
     * 整体评估
     */
    public OverallEvaluation evaluateOverall() {
        if (dataPoints.isEmpty()) {
            return new OverallEvaluation("数据不足", 0.0, 0.5, 0.0, "未知");
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        double slope = safeSlopeCalc(values);
        double hurst = hurstExponent(values);
        double volatility = calculateVolatility(values);
        double averageReturn = Arrays.stream(values).sum() / values.length;

        // 基于波动率的动态阈值判断整体趋势
        double trendThreshold = Math.max(0.10, Math.min(0.20, volatility / 300.0));
        String overallTrend = slope > trendThreshold ? "趋势上涨" : (slope < -trendThreshold ? "趋势下跌" : "震荡行情");

        String persistence = hurst > 0.65 ? "强" : (hurst > 0.55 ? "中" : "弱");

        return new OverallEvaluation(
                overallTrend,
                Math.round(averageReturn * 10) / 10.0,
                Math.round(hurst * 100) / 100.0,
                Math.round(volatility * 10) / 10.0,
                persistence
        );
    }

    /**
     * 获取马尔可夫分析结果
     */
    /**
     * 获取马尔可夫分析结果
     */
    public MarkovAnalysisResult getMarkovResult() {
        this.autoSegment();
        List<SegmentReport> segmentReports = this.analyzeSegments();
        OverallEvaluation overallEvaluation = this.evaluateOverall();
        
        // 生成决策建议
        String decisionAdvice = generateDecisionAdvice(overallEvaluation, segmentReports);
        
        return new MarkovAnalysisResult(segmentReports, overallEvaluation, decisionAdvice);
    }

    /**
     * 生成决策建议
     */
    private String generateDecisionAdvice(OverallEvaluation overall, List<SegmentReport> segments) {
        StringBuilder advice = new StringBuilder();
        
        // 基于整体趋势和Hurst指数生成建议
        if ("趋势上涨".equals(overall.getOverallTrend()) && "强".equals(overall.getPersistence())) {
            advice.append("当前市场呈现强劲上涨趋势，建议持有多头头寸。");
        } else if ("趋势下跌".equals(overall.getOverallTrend()) && "强".equals(overall.getPersistence())) {
            advice.append("当前市场呈现强劲下跌趋势，建议持有空头头寸或离场观望。");
        } else if ("震荡行情".equals(overall.getOverallTrend()) && overall.getVolatility() > 20) {
            advice.append("当前市场处于宽幅震荡，建议采取高抛低吸策略或观望。");
        } else if ("震荡行情".equals(overall.getOverallTrend())) {
            advice.append("当前市场处于窄幅震荡，建议等待明确趋势出现后再入场。");
        }
        
        // 增加关于近期分段的建议
        if (!segments.isEmpty()) {
            SegmentReport latestSegment = segments.get(segments.size() - 1);
            advice.append(" 近期").append(latestSegment.getTrendType());
            advice.append("，").append(latestSegment.getTrendStrength()).append("强度趋势。");
        }
        
        return advice.toString();
    }

    /**
     * 内部类：分段
     */
    public static class Segment {
        private final int start;
        private final int end;

        public Segment(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() { return start; }
        public int getEnd() { return end; }
    }

    /**
     * 内部类：趋势结果
     */
    public static class TrendResult {
        private final String type;
        private final double hurst;

        public TrendResult(String type, double hurst) {
            this.type = type;
            this.hurst = hurst;
        }

        public String getType() { return type; }
        public double getHurst() { return hurst; }
    }

    /**
     * 内部类：分段报告
     */
    public static class SegmentReport {
        private final String timeRangeStart;
        private final String timeRangeEnd;
        private final long duration;
        private final String trendType;
        private final double meanReturn;
        private final double maxDrawdown;
        private final double volatility;
        private final double hurst;
        private final String trendStrength;

        public SegmentReport(String timeRangeStart, String timeRangeEnd, long duration, String trendType, 
                            double meanReturn, double maxDrawdown, double volatility, double hurst, 
                            String trendStrength) {
            this.timeRangeStart = timeRangeStart;
            this.timeRangeEnd = timeRangeEnd;
            this.duration = duration;
            this.trendType = trendType;
            this.meanReturn = meanReturn;
            this.maxDrawdown = maxDrawdown;
            this.volatility = volatility;
            this.hurst = hurst;
            this.trendStrength = trendStrength;
        }

        // Getters
        public String getTimeRange() { return timeRangeStart + "-" + timeRangeEnd; }
        public long getDuration() { return duration; }
        public String getTrendType() { return trendType; }
        public double getMeanReturn() { return meanReturn; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public double getVolatility() { return volatility; }
        public double getHurst() { return hurst; }
        public String getTrendStrength() { return trendStrength; }
    }
}