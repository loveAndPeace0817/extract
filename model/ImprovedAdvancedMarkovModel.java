package com.demo.extract.model;

import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.model.AdvancedMarkovModel;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 改进版高级马尔可夫模型
 * 专为黄金5分钟K线数据优化，解决原模型分段分析和Hurst指数计算问题
 */
public class ImprovedAdvancedMarkovModel {
    private List<FinancialDataPoint> dataPoints;
    private List<Segment> segments;
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    // 为黄金5分钟K线数据优化的参数
    private static final int MIN_INTERVAL = 6; // 最小间隔为6个数据点(约30分钟)
    private static final double PEAK_PROMINENCE = 5.0; // 降低峰值突出度门槛，使分段更细致
    private static final double PRICE_CHANGE_THRESHOLD = 3.0; // 降低价格变化阈值
    private static final long MIN_DURATION_MINUTES = 15; // 降低每个分段的最小持续时间
    private static final int MAX_SEGMENTS = 8; // 增加最多保留的分段数量
    private static final double HIGH_VOLATILITY_THRESHOLD = 10.0; // 高波动率阈值
    private static final double LOW_VOLATILITY_THRESHOLD = 3.0; // 低波动率阈值

    /**
     * 构造函数
     * @param values 价格数组
     * @param valueTime 时间戳数组
     * @param orderId 订单ID
     */
    public ImprovedAdvancedMarkovModel(double[] values, String[] valueTime, String orderId) {
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
            // 处理时间格式，适配黄金K线数据的时间格式(yyyy.MM.dd HH:mm)
            try {
                LocalDateTime timestamp;
                if (valueTime[i].length() == 16) { // 格式为 yyyy.MM.dd HH:mm
                    timestamp = LocalDateTime.parse(valueTime[i], INPUT_FORMATTER);
                } else {
                    timestamp = LocalDateTime.parse(valueTime[i], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                dataPoints.add(new FinancialDataPoint(value, timestamp.format(ISO_FORMATTER), orderId));
            } catch (Exception e) {
                // 处理时间格式异常
                System.err.println("时间格式解析错误: " + valueTime[i]);
            }
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
     * 计算对数收益率
     */
    private double[] calculateLogReturns(double[] prices) {
        if (prices.length < 2) {
            return new double[0];
        }
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
        }
        return returns;
    }

    /**
     * 趋势类型判断 - 优化版，支持轻微趋势类型
     */
    private TrendResult determineTrendType(double[] segmentPrices) {
        if (segmentPrices.length < 3) {
            return new TrendResult("数据不足", 0.5);
        }

        // 计算对数收益率进行趋势分析
        double[] logReturns = calculateLogReturns(segmentPrices);
        double slope = safeSlopeCalc(segmentPrices); // 使用价格计算斜率
        double volatility = calculateVolatility(logReturns); // 使用对数收益率计算波动率
        double hurst = hurstExponent(logReturns); // 使用对数收益率计算Hurst指数

        // 计算价格变化百分比
        double priceChangePercent = (segmentPrices[segmentPrices.length - 1] - segmentPrices[0]) / 
                                   segmentPrices[0] * 100;
        
        // 基于价格变化幅度的动态阈值
        double priceRange = Arrays.stream(segmentPrices).max().orElse(0.0) - 
                           Arrays.stream(segmentPrices).min().orElse(0.0);
        double trendThreshold = Math.max(0.01, Math.min(0.05, priceRange / 200.0));
        double minorTrendThreshold = trendThreshold / 2; // 轻微趋势的阈值
        
        // 改进的趋势判断逻辑，增加轻微趋势类型
        if (Math.abs(slope) > trendThreshold) {
            return new TrendResult(slope > 0 ? "趋势上涨" : "趋势下跌", hurst);
        } else if (Math.abs(slope) > minorTrendThreshold || Math.abs(priceChangePercent) > 0.1) {
            // 轻微趋势判断：斜率较小但仍有明显方向，或有一定价格变化百分比
            return new TrendResult(slope > 0 ? "轻微上涨" : "轻微下跌", hurst);
        } else {
            // 更细致的震荡行情分类
            if (volatility > HIGH_VOLATILITY_THRESHOLD) {
                return new TrendResult("宽幅震荡", hurst);
            } else if (volatility < LOW_VOLATILITY_THRESHOLD) {
                return new TrendResult("窄幅震荡", hurst);
            } else {
                return new TrendResult("温和震荡", hurst);
            }
        }
    }

    /**
     * 计算波动率 - 优化版
     */
    private double calculateVolatility(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance) * 100; // 放大波动率以便观察
    }

    /**
     * 计算Hurst指数 - 改进版，提高计算准确性
     */
    private double hurstExponent(double[] ts) {
        if (ts.length < 20) { // 增加最小数据量要求以提高准确性
            return 0.5;
        }

        try {
            // 优化滞后阶数的选择
            int maxLag = Math.min(20, ts.length / 4); // 更合理的滞后阶数上限
            List<Integer> lags = new ArrayList<>();
            List<Double> logRSDivLogN = new ArrayList<>();

            // 计算累积离差
            double[] cumulativeDeviations = new double[ts.length];
            double mean = Arrays.stream(ts).average().orElse(0.0);
            cumulativeDeviations[0] = ts[0] - mean;
            for (int i = 1; i < ts.length; i++) {
                cumulativeDeviations[i] = cumulativeDeviations[i - 1] + ts[i] - mean;
            }

            // 对不同的滞后阶数计算R/S值
            for (int lag = 2; lag <= maxLag; lag++) {
                int numBlocks = ts.length / lag;
                double[] rsValues = new double[numBlocks];

                for (int block = 0; block < numBlocks; block++) {
                    int startIndex = block * lag;
                    int endIndex = startIndex + lag;
                    
                    // 计算块内均值
                    double blockMean = 0;
                    for (int i = startIndex; i < endIndex; i++) {
                        blockMean += ts[i];
                    }
                    blockMean /= lag;
                    
                    // 计算块内累积离差
                    double[] blockCumulativeDeviations = new double[lag];
                    blockCumulativeDeviations[0] = ts[startIndex] - blockMean;
                    for (int i = 1; i < lag; i++) {
                        blockCumulativeDeviations[i] = blockCumulativeDeviations[i - 1] + ts[startIndex + i] - blockMean;
                    }
                    
                    // 计算极差
                    double max = Double.MIN_VALUE;
                    double min = Double.MAX_VALUE;
                    for (double dev : blockCumulativeDeviations) {
                        if (dev > max) max = dev;
                        if (dev < min) min = dev;
                    }
                    double range = max - min;
                    
                    // 计算标准差
                    double variance = 0;
                    for (int i = startIndex; i < endIndex; i++) {
                        variance += Math.pow(ts[i] - blockMean, 2);
                    }
                    variance /= lag;
                    double stdDev = Math.sqrt(variance);
                    
                    // 计算R/S值
                    if (stdDev > 0) {
                        rsValues[block] = range / stdDev;
                    } else {
                        rsValues[block] = 0;
                    }
                }
                
                // 计算平均R/S值
                double avgRS = Arrays.stream(rsValues).average().orElse(0.0);
                if (avgRS > 0 && lag > 0) {
                    lags.add(lag);
                    logRSDivLogN.add(Math.log(avgRS) / Math.log(lag));
                }
            }

            if (lags.size() < 3) { // 确保有足够的点进行回归
                return 0.5;
            }

            // 线性回归计算Hurst指数
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < lags.size() && i < logRSDivLogN.size(); i++) {
                regression.addData(Math.log(lags.get(i)), logRSDivLogN.get(i));
            }
            
            // 对Hurst指数进行范围限制和修正
            double hurst = regression.getSlope();
            if (hurst < 0.1) {
                hurst = 0.1; // 下限保护
            } else if (hurst > 0.9) {
                hurst = 0.9; // 上限保护
            }
            
            return hurst;
        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * 自动分段 - 优化版，修复分段不合理问题
     */
    public void autoSegment() {
        if (dataPoints.isEmpty() || dataPoints.size() < 10) { // 确保有足够的数据点
            return;
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        // 识别峰值和谷值 - 使用改进的算法
        List<Integer> peaks = findPeaks(values, PEAK_PROMINENCE / 2); // 降低突出度门槛以识别更多转折点
        List<Integer> valleys = findPeaks(invertArray(values), PEAK_PROMINENCE / 2);

        // 合并并排序所有转折点
        Set<Integer> turningPoints = new TreeSet<>();
        turningPoints.addAll(peaks);
        turningPoints.addAll(valleys);
        turningPoints.add(0);
        turningPoints.add(values.length - 1);

        // 筛选显著转折点 - 使用自适应局部价格变化百分比
        List<Integer> significantPoints = new ArrayList<>();
        significantPoints.add(0);
        
        // 计算数据整体波动率，用于自适应阈值
        double[] logReturns = calculateLogReturns(values);
        double overallVolatility = calculateVolatility(logReturns);
        
        // 根据波动率调整价格变化阈值
        double baseThreshold = 0.1; // 基础阈值
        double adaptiveThreshold = baseThreshold * (1 + Math.min(overallVolatility / 10, 2)); // 自适应调整
        
        // 确保至少有5个数据点间隔，避免过多的小分段
        int lastAddedIndex = 0;
        for (int i = 1; i < values.length - 1; i++) {
            if (turningPoints.contains(i) && i - lastAddedIndex >= 5) { // 至少间隔5个数据点
                double priceChangePercent = Math.abs(values[i] - values[lastAddedIndex]) / 
                                           values[lastAddedIndex] * 100;
                
                // 使用自适应阈值
                if (priceChangePercent > adaptiveThreshold) {
                    significantPoints.add(i);
                    lastAddedIndex = i;
                }
            }
        }
        
        // 如果识别的转折点太少，强制分段
        if (significantPoints.size() < 3) {
            // 等间隔分段
            int segmentCount = Math.min(6, values.length / 20); // 最多6个分段，每段至少20个数据点
            if (segmentCount < 2) segmentCount = 2;
            
            significantPoints.clear();
            significantPoints.add(0);
            
            for (int i = 1; i < segmentCount; i++) {
                int index = (int)(values.length * i / segmentCount);
                significantPoints.add(index);
            }
        }
        
        if (!significantPoints.contains(values.length - 1)) {
            significantPoints.add(values.length - 1);
        }

        // 合并相邻过近的转折点
        List<Integer> mergedPoints = mergeClosePoints(significantPoints);

        // 基于时间筛选分段
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
     * 构建分段 - 修复持续时间为0的分段问题
     */
    private void buildSegments(List<Integer> points, int endIndex) {
        segments.clear();
        if (points.isEmpty() || points.size() < 2) {
            // 如果没有足够的点，创建一个默认分段
            segments.add(new Segment(0, endIndex));
            return;
        }

        int start = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            int point = points.get(i);
            // 确保分段至少有2个数据点，避免持续时间为0的分段
            if (point > start + 1) {
                segments.add(new Segment(start, point));
                start = point;
            }
        }
        
        // 添加最后一个分段，确保包含所有数据
        if (start < endIndex) {
            segments.add(new Segment(start, endIndex));
        }

        // 处理特殊情况：如果没有有效的分段，创建一个默认分段
        if (segments.isEmpty()) {
            segments.add(new Segment(0, endIndex));
        }

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
                // 计算突出度（更精确的实现）
                int leftMinIdx = i - 1;
                while (leftMinIdx > 0 && values[leftMinIdx] >= values[leftMinIdx - 1]) {
                    leftMinIdx--;
                }
                
                int rightMinIdx = i + 1;
                while (rightMinIdx < values.length - 1 && values[rightMinIdx] >= values[rightMinIdx + 1]) {
                    rightMinIdx++;
                }
                
                double leftMin = values[leftMinIdx];
                double rightMin = values[rightMinIdx];
                double peakProminence = values[i] - Math.max(leftMin, rightMin);
                
                // 使用相对突出度
                double avgPrice = Arrays.stream(values).average().orElse(0.0);
                double relativeProminence = peakProminence / avgPrice * 100;
                
                if (relativeProminence >= prominence / 5) { // 调整相对突出度阈值
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
     * 分析分段 - 优化版，确保正确处理小分段
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
            int segmentLength = end - start + 1;
            
            // 确保分段长度合理，避免过小的分段
            if (segmentLength < 2) {
                continue; // 跳过无效分段
            }
            
            double[] segmentValues = Arrays.copyOfRange(values, start, end + 1);

            LocalDateTime startTimestamp = LocalDateTime.parse(
                    dataPoints.get(start).getTimestamp(), ISO_FORMATTER);
            LocalDateTime endTimestamp = LocalDateTime.parse(
                    dataPoints.get(end).getTimestamp(), ISO_FORMATTER);
            long duration = java.time.Duration.between(startTimestamp, endTimestamp).toMinutes();
            
            // 对于持续时间为0的特殊情况，使用合理的默认值
            if (duration <= 0) {
                duration = 5; // 默认5分钟（一个K线周期）
            }

            // 计算各种指标
            double[] logReturns = calculateLogReturns(segmentValues);
            double meanReturn = logReturns.length > 0 ? Arrays.stream(logReturns).average().orElse(0.0) * 100 : 0.0;
            double maxDrawdown = calculateMaxDrawdown(segmentValues);
            double volatility = calculateVolatility(logReturns);
            
            // 确保至少有3个数据点进行趋势判断
            TrendResult trendResult;
            if (segmentLength >= 3) {
                trendResult = determineTrendType(segmentValues);
            } else {
                // 对于小段数据，根据价格变化直接判断趋势
                double priceChange = segmentValues[segmentLength - 1] - segmentValues[0];
                String trendType = priceChange > 0 ? "轻微上涨" : (priceChange < 0 ? "轻微下跌" : "横盘");
                trendResult = new TrendResult(trendType, 0.5); // 默认为随机游走
            }
            
            double hurst = trendResult.getHurst();

            // 改进的趋势强度评估 - 降低阈值并考虑更多因素
            String trendStrength;
            if ("趋势上涨".equals(trendResult.getType()) || "趋势下跌".equals(trendResult.getType()) ||
                "轻微上涨".equals(trendResult.getType()) || "轻微下跌".equals(trendResult.getType())) {
                // 结合平均收益率、价格变化百分比和分段持续时间综合评估
                double priceChangePercent = (segmentValues[segmentValues.length - 1] - segmentValues[0]) / 
                                           segmentValues[0] * 100;
                
                // 降低阈值，使趋势强度判断更合理
                if (Math.abs(meanReturn) > 0.3 || Math.abs(priceChangePercent) > 0.4 || 
                    (Math.abs(meanReturn) > 0.2 && duration > 60)) {
                    trendStrength = "强";
                } else if (Math.abs(meanReturn) > 0.1 || Math.abs(priceChangePercent) > 0.2 || 
                           (Math.abs(meanReturn) > 0.05 && duration > 30)) {
                    trendStrength = "中";
                } else {
                    trendStrength = "弱";
                }
            } else if ("横盘".equals(trendResult.getType())) {
                trendStrength = "弱";
            } else {
                // 震荡行情的强度评估基于波动率
                if (volatility > HIGH_VOLATILITY_THRESHOLD) {
                    trendStrength = "强";
                } else if (volatility < LOW_VOLATILITY_THRESHOLD) {
                    trendStrength = "弱";
                } else {
                    trendStrength = "中";
                }
            }

            reports.add(new SegmentReport(
                    startTimestamp.format(OUTPUT_FORMATTER),
                    endTimestamp.format(OUTPUT_FORMATTER),
                    duration,
                    trendResult.getType(),
                    Math.round(meanReturn * 100) / 100.0, // 保留两位小数
                    Math.round(maxDrawdown * 100) / 100.0,
                    Math.round(volatility * 100) / 100.0,
                    Math.round(hurst * 100) / 100.0,
                    trendStrength
            ));
        }
        return reports;
    }

    /**
     * 计算最大回撤 - 优化版
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
     * 整体评估 - 优化版
     */
    public OverallEvaluation evaluateOverall() {
        if (dataPoints.isEmpty()) {
            return new OverallEvaluation("数据不足", 0.0, 0.5, 0.0, "未知");
        }

        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        double[] logReturns = calculateLogReturns(values);
        double slope = safeSlopeCalc(values);
        double hurst = hurstExponent(logReturns);
        double volatility = calculateVolatility(logReturns);
        double averageReturn = logReturns.length > 0 ? Arrays.stream(logReturns).average().orElse(0.0) * 100 : 0.0;

        // 改进的整体趋势判断
        double priceChange = values[values.length - 1] - values[0];
        double priceChangePercent = priceChange / values[0] * 100;
        String overallTrend;
        
        if (priceChangePercent > 0.5) {
            overallTrend = "趋势上涨";
        } else if (priceChangePercent < -0.5) {
            overallTrend = "趋势下跌";
        } else {
            overallTrend = volatility > HIGH_VOLATILITY_THRESHOLD ? "宽幅震荡" : 
                          (volatility < LOW_VOLATILITY_THRESHOLD ? "窄幅震荡" : "温和震荡");
        }

        // 改进的Hurst指数解释
        String persistence;
        if (hurst > 0.7) {
            persistence = "强趋势";
        } else if (hurst > 0.55) {
            persistence = "中等趋势";
        } else if (hurst < 0.45) {
            persistence = "反转趋势";
        } else {
            persistence = "随机游走";
        }

        return new OverallEvaluation(
                overallTrend,
                Math.round(averageReturn * 100) / 100.0,
                Math.round(hurst * 100) / 100.0,
                Math.round(volatility * 100) / 100.0,
                persistence
        );
    }

    /**
     * 获取马尔可夫分析结果
     */
    public MarkovAnalysisResult getMarkovResult() {
        this.autoSegment();
        List<SegmentReport> segmentReports = this.analyzeSegments();
        OverallEvaluation overallEvaluation = this.evaluateOverall();
        
        // 生成决策建议
        String decisionAdvice = generateDecisionAdvice(overallEvaluation, segmentReports);
        
        // 将ImprovedAdvancedMarkovModel.SegmentReport转换为AdvancedMarkovModel.SegmentReport
        List<AdvancedMarkovModel.SegmentReport> convertedSegmentReports = new ArrayList<>();
        for (SegmentReport report : segmentReports) {
            convertedSegmentReports.add(new AdvancedMarkovModel.SegmentReport(
                    report.getTimeRangeStart(),
                    report.getTimeRangeEnd(),
                    report.getDuration(),
                    report.getTrendType(),
                    report.getMeanReturn(),
                    report.getMaxDrawdown(),
                    report.getVolatility(),
                    report.getHurst(),
                    report.getTrendStrength()
            ));
        }
        
        return new MarkovAnalysisResult(convertedSegmentReports, overallEvaluation, decisionAdvice);
    }

    /**
     * 生成决策建议 - 优化版
     */
    private String generateDecisionAdvice(OverallEvaluation overall, List<SegmentReport> segments) {
        StringBuilder advice = new StringBuilder();
        
        // 基于整体趋势、Hurst指数和波动率生成建议
        if ("趋势上涨".equals(overall.getOverallTrend())) {
            if (overall.getPersistence().contains("趋势")) {
                advice.append("当前市场呈现上涨趋势且有持续性，建议逢低买入或持有多头头寸。");
            } else {
                advice.append("当前市场呈现上涨趋势但持续性较弱，建议谨慎追高，设置好止损。");
            }
        } else if ("趋势下跌".equals(overall.getOverallTrend())) {
            if (overall.getPersistence().contains("趋势")) {
                advice.append("当前市场呈现下跌趋势且有持续性，建议逢高卖出或持有空头头寸。");
            } else {
                advice.append("当前市场呈现下跌趋势但持续性较弱，建议谨慎做空，设置好止损。");
            }
        } else if ("宽幅震荡".equals(overall.getOverallTrend())) {
            advice.append("当前市场处于宽幅震荡，建议采取高抛低吸策略，关注支撑阻力位。");
        } else if ("窄幅震荡".equals(overall.getOverallTrend())) {
            advice.append("当前市场处于窄幅震荡，建议等待突破行情出现后再入场。");
        } else {
            advice.append("当前市场处于温和震荡，建议观望为主，关注量能变化。");
        }
        
        // 增加关于波动率和Hurst指数的建议
        if (overall.getVolatility() > HIGH_VOLATILITY_THRESHOLD * 1.5) {
            advice.append(" 市场波动率较高，注意控制仓位和风险。");
        } else if (overall.getVolatility() < LOW_VOLATILITY_THRESHOLD / 2) {
            advice.append(" 市场波动率较低，可能即将出现突破行情。");
        }
        
        if (overall.getHurstIndex() < 0.4) {
            advice.append(" 市场呈现反持续性，注意趋势反转风险。");
        }
        
        // 增加关于近期分段的建议
        if (!segments.isEmpty()) {
            SegmentReport latestSegment = segments.get(segments.size() - 1);
            advice.append(" 近期市场").append(latestSegment.getTrendType());
            advice.append("，").append(latestSegment.getTrendStrength()).append("强度。");
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
        public String getTimeRangeStart() { return timeRangeStart; }
        public String getTimeRangeEnd() { return timeRangeEnd; }
        public long getDuration() { return duration; }
        public String getTrendType() { return trendType; }
        public double getMeanReturn() { return meanReturn; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public double getVolatility() { return volatility; }
        public double getHurst() { return hurst; }
        public String getTrendStrength() { return trendStrength; }
    }
}