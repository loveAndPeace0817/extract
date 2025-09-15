package com.demo.extract.model;

import com.demo.extract.DTO.FinancialDataPoint;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ThreeMarkovModel {
    private List<FinancialDataPoint> dataPoints;
    private List<Segment> segments;
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MIN_INTERVAL = 6; // 增加最小间隔为6个数据点(约30分钟)
    private static final double PEAK_PROMINENCE = 20.0; // 提高峰值突出度门槛
    private static final double PRICE_CHANGE_THRESHOLD = 30.0; // 提高价格变化阈值
    private static final long MIN_DURATION_MINUTES = 30; // 每个分段的最小持续时间(分钟)

    public ThreeMarkovModel(double[] values,String[] valueTime,String orderId) {
        parseTrueData(values,valueTime,orderId);
        this.segments = new ArrayList<>();
    }

    // 解析数据


    private void parseTrueData( double[] values,String[] valueTime,String orderId) {
        dataPoints = new ArrayList<>();
        for (int i = 0; i < valueTime.length; i++) {
            double value = values[i];
            LocalDateTime timestamp = LocalDateTime.parse(valueTime[i], INPUT_FORMATTER);
            dataPoints.add(new FinancialDataPoint(value, timestamp.format(ISO_FORMATTER),orderId));
        }
    }

    // 安全的斜率计算
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

    // 趋势类型判断
    private TrendResult determineTrendType(double[] segmentValues) {
        if (segmentValues.length < 3) {
            return new TrendResult("数据不足", 0.5);
        }

        double slope = safeSlopeCalc(segmentValues);
        double volatility = calculateVolatility(segmentValues);

        if (Math.abs(slope) > 0.25) { // 调整斜率阈值，基于profit数据分析0.25
            return new TrendResult(slope > 0 ? "趋势上涨" : "趋势下跌", 0.65);
        } else {
            return new TrendResult(volatility > 20 ? "宽幅震荡" : "窄幅震荡", 0.5);
        }
    }

    // 计算波动率
    private double calculateVolatility(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    // 计算Hurst指数
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

    // 自动分段
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
        List<Integer> mergedPoints = new ArrayList<>();
        for (int point : significantPoints) {
            if (mergedPoints.isEmpty() || point - mergedPoints.get(mergedPoints.size() - 1) >= MIN_INTERVAL) {
                mergedPoints.add(point);
            }
        }

        // 基于时间筛选分段，确保每个分段至少有MIN_DURATION_MINUTES
        List<Integer> timeFilteredPoints = new ArrayList<>();
        if (!mergedPoints.isEmpty()) {
            timeFilteredPoints.add(mergedPoints.get(0));
            LocalDateTime lastTime = LocalDateTime.parse(
                    dataPoints.get(mergedPoints.get(0)).getTimestamp(), ISO_FORMATTER);

            for (int i = 1; i < mergedPoints.size(); i++) {
                int point = mergedPoints.get(i);
                LocalDateTime currentTime = LocalDateTime.parse(
                        dataPoints.get(point).getTimestamp(), ISO_FORMATTER);
                long duration = java.time.Duration.between(lastTime, currentTime).toMinutes();

                if (duration >= MIN_DURATION_MINUTES) {
                    timeFilteredPoints.add(point);
                    lastTime = currentTime;
                }
            }

            // 确保至少有一个分段
            if (timeFilteredPoints.size() < 2 && !mergedPoints.isEmpty()) {
                timeFilteredPoints.clear();
                timeFilteredPoints.add(0);
                timeFilteredPoints.add(mergedPoints.get(mergedPoints.size() - 1));
            }

            mergedPoints = timeFilteredPoints;
        }

        // 构建最终分段
        segments.clear();
        int start = 0;
        for (int point : mergedPoints) {
            segments.add(new Segment(start, point));
            start = point;
        }
        segments.add(new Segment(start, values.length - 1));

        // 最多保留5段
        if (segments.size() > 5) {
            segments = segments.subList(0, 5);
        }
    }

    // 寻找峰值
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

    // 反转数组（用于寻找谷值）
    private double[] invertArray(double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = -array[i];
        }
        return result;
    }

    // 分析分段
    public List<SegmentReport> analyzeSegments() {
        List<SegmentReport> reports = new ArrayList<>();
        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        for (Segment segment : segments) {
            int start = segment.getStart();
            int end = segment.getEnd();
            double[] segmentValues = Arrays.copyOfRange(values, start, end + 1);

            // 修改后
            LocalDateTime startTimestamp = LocalDateTime.parse(
                    dataPoints.get(start).getTimestamp(), ISO_FORMATTER);
            LocalDateTime endTimestamp = LocalDateTime.parse(
                    dataPoints.get(end).getTimestamp(), ISO_FORMATTER);
            long duration = java.time.Duration.between(startTimestamp, endTimestamp).toMinutes();


            double meanReturn = Arrays.stream(segmentValues).average().orElse(0.0);
            double maxDrawdown = Arrays.stream(segmentValues).min().orElse(0.0);
            double volatility = calculateVolatility(segmentValues);
            TrendResult trendResult = determineTrendType(segmentValues);
            double hurst = hurstExponent(segmentValues);

            String trendStrength = Math.abs(meanReturn) > 30 ? "强" : (Math.abs(meanReturn) > 15 ? "中" : "弱"); // 调整趋势强度判断阈值，基于profit数据分析

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

    // 整体评估
    public OverallEvaluation evaluateOverall() {
        if (dataPoints.isEmpty()) {
            return new OverallEvaluation("数据不足", 0.0, 0.5, "未知");
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        double slope = safeSlopeCalc(values);
        double hurst = hurstExponent(values);

        double cumulativeReturn = Arrays.stream(values).sum() / values.length; // 改为计算平均收益，与Python保持一致

        String overallTrend = slope > 0.15 ? "趋势上涨" : (slope < -0.15 ? "趋势下跌" : "震荡行情"); // 调整整体趋势判断阈值，基于profit数据分析

        String persistence = hurst > 0.65 ? "强" : (hurst > 0.55 ? "中" : "弱"); // Hurst指数阈值保持不变

        return new OverallEvaluation(
                overallTrend,
                Math.round(cumulativeReturn * 10) / 10.0,
                Math.round(hurst * 100) / 100.0,
                persistence
        );
    }

    // 可视化
   /* public void visualize() {
        TimeSeries series = new TimeSeries("收益 (bps)");
        for (FinancialDataPoint point : dataPoints) {
            String timestamp = point.getTimestamp();
            Minute minute = new Minute(
                    timestamp.getMinute(),
                    timestamp.getHour(),
                    timestamp.getDayOfMonth(),
                    timestamp.getMonthValue(),
                    timestamp.getYear()
            );
            series.add(minute, point.getValue());
        }

        XYDataset dataset = new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "市场趋势分析",
                "时间",
                "收益 (bps)",
                dataset,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        plot.setRenderer(renderer);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        // 添加分段背景色
        Map<String, Color> colorMap = new HashMap<>();
        colorMap.put("趋势上涨", new Color(0, 255, 0, 50));
        colorMap.put("趋势下跌", new Color(255, 0, 0, 50));
        colorMap.put("宽幅震荡", new Color(255, 165, 0, 50));
        colorMap.put("窄幅震荡", new Color(0, 0, 255, 50));
        colorMap.put("数据不足", new Color(128, 128, 128, 50));

        List<SegmentReport> reports = analyzeSegments();
        for (int i = 0; i < segments.size() && i < reports.size(); i++) {
            Segment segment = segments.get(i);
            SegmentReport report = reports.get(i);
            FinancialDataPoint startPoint = dataPoints.get(segment.getStart());
            FinancialDataPoint endPoint = dataPoints.get(segment.getEnd());

            Minute startMinute = new Minute(
                    startPoint.getTimestamp().getMinute(),
                    startPoint.getTimestamp().getHour(),
                    startPoint.getTimestamp().getDayOfMonth(),
                    startPoint.getTimestamp().getMonthValue(),
                    startPoint.getTimestamp().getYear()
            );

            Minute endMinute = new Minute(
                    endPoint.getTimestamp().getMinute(),
                    endPoint.getTimestamp().getHour(),
                    endPoint.getTimestamp().getDayOfMonth(),
                    endPoint.getTimestamp().getMonthValue(),
                    endPoint.getTimestamp().getYear()
            );

            IntervalMarker marker = new IntervalMarker(
                    startMinute.getFirstMillisecond(),
                    endMinute.getLastMillisecond()
            );
            marker.setPaint(colorMap.getOrDefault(report.getTrendType(), Color.GRAY));
            plot.addDomainMarker(marker);
        }

        JFrame frame = new JFrame("市场趋势分析");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ChartPanel(chart), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }*/

    // 内部类：分段
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

    // 内部类：趋势结果
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




    // 内部类：分段报告
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

    // 内部类：整体评估
    public static class OverallEvaluation {
        private final String overallTrend;
        private final double cumulativeReturn;
        private final double hurst;
        private final String persistence;

        public OverallEvaluation(String overallTrend, double cumulativeReturn, double hurst, String persistence) {
            this.overallTrend = overallTrend;
            this.cumulativeReturn = cumulativeReturn;
            this.hurst = hurst;
            this.persistence = persistence;
        }

        // Getters
        public String getOverallTrend() { return overallTrend; }
        public double getCumulativeReturn() { return cumulativeReturn; }
        public double getHurst() { return hurst; }
        public String getPersistence() { return persistence; }
    }
}