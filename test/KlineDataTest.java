package com.demo.extract.test;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.AdvancedMarkovModel;
import com.demo.extract.model.KlineData;
import com.demo.extract.model.ImprovedAdvancedMarkovModel;
import com.demo.extract.model.MarkovAnalysisResult;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.model.ImprovedAdvancedMarkovModel.SegmentReport;
import com.demo.extract.services.KlineDataLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * K线数据测试类，用于验证KlineData和KlineDataLoader的功能
 * 并实现按天分组的马尔可夫模型分析
 */
public class KlineDataTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public static void main(String[] args) {
        try {
            // 初始化K线数据加载器
            KlineDataLoader loader = new KlineDataLoader();
            String symbol = "XAUUSD";
            
            // 从CSV文件加载K线数据
            List<KlineData> klines = loader.loadFromCsv("D:/data/K线/XAUUSD_5.csv", symbol);
            
            // 打印K线数据统计信息
            System.out.println(loader.getKlineStatistics(klines));
            
            // 按天分组K线数据
            Map<LocalDate, List<KlineData>> klinesByDay = groupKlinesByDay(klines);
            System.out.println("\n按天分组统计：共" + klinesByDay.size() + "天数据");
            
            // 为每一天的数据进行马尔可夫模型分析
            for (Map.Entry<LocalDate, List<KlineData>> entry : klinesByDay.entrySet()) {
                LocalDate date = entry.getKey();
                List<KlineData> dailyKlines = entry.getValue();
                
                System.out.println("\n========== 分析日期：" + date.format(DATE_FORMATTER) + " (" + dailyKlines.size() + "条数据) ==========");
                
                // 使用收盘价进行马尔可夫模型分析
                analyzeDailyDataWithMarkov(dailyKlines, date.format(DATE_FORMATTER));
            }
            
            System.out.println("\n测试完成");
            
        } catch (IOException e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 按天分组K线数据
     * @param klines K线数据列表
     * @return 按日期分组的K线数据
     */
    private static Map<LocalDate, List<KlineData>> groupKlinesByDay(List<KlineData> klines) {
        return klines.stream()
                .collect(Collectors.groupingBy(kline -> kline.getTimestamp().toLocalDate()));
    }
    
    /**
     * 使用ImprovedAdvancedMarkovModel分析每日K线数据
     * @param dailyKlines 每日K线数据
     * @param dateStr 日期字符串
     */
    private static void analyzeDailyDataWithMarkov(List<KlineData> dailyKlines, String dateStr) {
        if (dailyKlines.isEmpty()) {
            System.out.println("没有可用的K线数据进行分析");
            return;
        }
        
        // 准备ImprovedAdvancedMarkovModel所需的数据格式
        int dataSize = dailyKlines.size();
        double[] closeValues = new double[dataSize];
        String[] timeStrings = new String[dataSize];
        
        // 填充数据
        for (int i = 0; i < dataSize; i++) {
            KlineData kline = dailyKlines.get(i);
            closeValues[i] = kline.getClose();
            // 格式化为适合ImprovedAdvancedMarkovModel的时间格式
            timeStrings[i] = kline.getTimestamp().format(DATE_FORMATTER) + " " + 
                            kline.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        // 创建ImprovedAdvancedMarkovModel并进行分析
        ImprovedAdvancedMarkovModel model = new ImprovedAdvancedMarkovModel(closeValues, timeStrings, "KLINE_" + dateStr);
        MarkovAnalysisResult result = model.getMarkovResult();
        
        // 打印分析结果
        printMarkovAnalysisResult(result);
    }
    
    /**
     * 打印马尔可夫模型分析结果
     * @param result 分析结果
     */
    private static void printMarkovAnalysisResult(MarkovAnalysisResult result) {
        // 打印整体评估
        OverallEvaluation overallEval = result.getOverallEvaluation();
        System.out.println("\n【整体评估】");
        System.out.println("- 整体趋势: " + overallEval.getOverallTrend());
        //System.out.println("- 平均收益率: " + String.format("%.2f", overallEval.getCumulativeReturn()));
        System.out.println("- Hurst指数: " + String.format("%.2f", overallEval.getHurstIndex()));
        System.out.println("- 波动率: " + String.format("%.2f", overallEval.getVolatility()));
        System.out.println("- 趋势持续性: " + overallEval.getPersistence());
        
        // 打印分段分析报告
        List<AdvancedMarkovModel.SegmentReport> segmentReports = result.getSegmentReports();
        System.out.println("\n【分段分析报告】");
        System.out.println("共识别出" + segmentReports.size() + "个趋势分段");
        
        for (int i = 0; i < segmentReports.size(); i++) {
            AdvancedMarkovModel.SegmentReport report = segmentReports.get(i);
            System.out.println("\n分段" + (i + 1) + ":");
            System.out.println("- 时间范围: " + report.getTimeRange());
            System.out.println("- 持续时间: " + report.getDuration() + "分钟");
            System.out.println("- 趋势类型: " + report.getTrendType());
            System.out.println("- 趋势强度: " + report.getTrendStrength());
            System.out.println("- 平均收益率: " + String.format("%.2f", report.getMeanReturn()));
            System.out.println("- 最大回撤: " + String.format("%.2f", report.getMaxDrawdown()));
            System.out.println("- 波动率: " + String.format("%.2f", report.getVolatility()));
            System.out.println("- Hurst指数: " + String.format("%.2f", report.getHurst()));
        }
        
        // 打印决策建议
        System.out.println("\n【决策建议】");
        System.out.println(result.getDecisionAdvice());
    }



    /**
     * 计算额外的K线数据统计指标
     * @param klines K线数据列表
     */
    private static void calculateAdditionalMetrics(List<KlineData> klines) {
        if (klines == null || klines.size() < 2) {
            System.out.println("数据不足，无法计算额外指标");
            return;
        }

        // 计算累计收益率
        double startPrice = klines.get(0).getClose();
        double endPrice = klines.get(klines.size() - 1).getClose();
        double cumulativeReturn = ((endPrice - startPrice) / startPrice) * 100;
        
        // 计算涨跌分布
        int upCount = 0;
        int downCount = 0;
        int flatCount = 0;
        
        for (KlineData kline : klines) {
            double change = kline.getPriceChange();
            if (change > 0) {
                upCount++;
            } else if (change < 0) {
                downCount++;
            } else {
                flatCount++;
            }
        }
        
        System.out.println("\n额外统计指标:");
        System.out.println("- 累计收益率: " + String.format("%.2f%%", cumulativeReturn));
        System.out.println("- 上涨K线数: " + upCount + " (" + String.format("%.1f%%", (double)upCount/klines.size()*100) + ")");
        System.out.println("- 下跌K线数: " + downCount + " (" + String.format("%.1f%%", (double)downCount/klines.size()*100) + ")");
        System.out.println("- 平盘K线数: " + flatCount + " (" + String.format("%.1f%%", (double)flatCount/klines.size()*100) + ")");
    }
    
    /**
     * 分析订单时间点之前的K线数据并进行马尔可夫过程分析，判断是否适合当前多空方向
     * @param orderTimeSeries 订单时间序列对象
     * @param action 方向（"做多"或"做空"）
     * @return 马尔可夫分析结果
     * @throws IOException 如果文件读取失败
     */
    public MarkovAnalysisResult analyzeKlineDataBeforeOrder(OrderTimeSeries orderTimeSeries, String action) throws IOException {
        if (orderTimeSeries == null || orderTimeSeries.getValueTime() == null || orderTimeSeries.getValueTime().length == 0) {
            throw new IllegalArgumentException("订单对象或其时间序列数据不能为空");
        }
        
        // 读取valueTime数组的第一个值
        String firstDateTimeStr = orderTimeSeries.getValueTime()[0];
        System.out.println("分析订单ID: " + orderTimeSeries.getOrderId() + " 的前置K线数据，起始时间: " + firstDateTimeStr);
        
        // 解析时间字符串获取日期
        DateTimeFormatter formatter;
        LocalDateTime orderDateTime;
        try {
            // 首先尝试包含秒的格式
            formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
            orderDateTime = LocalDateTime.parse(firstDateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            try {
                // 尝试不包含秒的格式
                formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                orderDateTime = LocalDateTime.parse(firstDateTimeStr, formatter);
            } catch (DateTimeParseException e2) {
                // 尝试使用连字符分隔的格式
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                orderDateTime = LocalDateTime.parse(firstDateTimeStr, formatter);
            }
        }
        
        // 获取当天日期
        LocalDate targetDate = orderDateTime.toLocalDate();
        
        // 初始化K线数据加载器
        KlineDataLoader loader = new KlineDataLoader();
        String symbol = "XAUUSD";
        
        // 从CSV文件加载K线数据
        List<KlineData> allKlines = loader.loadFromCsv("D:/data/K线/XAUUSD_5.csv", symbol);
        
        // 筛选当天的数据
        List<KlineData> dailyKlines = allKlines.stream()
                .filter(kline -> kline.getTimestamp().toLocalDate().equals(targetDate))
                .collect(Collectors.toList());
        
        System.out.println("获取到" + targetDate.format(DATE_FORMATTER) + "的K线数据共" + dailyKlines.size() + "条");
        
        // 截取当天00:00到订单时间点的数据
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime finalOrderDateTime = orderDateTime;
        List<KlineData> filteredKlines = dailyKlines.stream()
                .filter(kline -> !kline.getTimestamp().isAfter(finalOrderDateTime) && !kline.getTimestamp().isBefore(startOfDay))
                .collect(Collectors.toList());
        
        System.out.println("截取时间段: " + startOfDay + " 到 " + orderDateTime + " 的数据共" + filteredKlines.size() + "条");
        
        // 对截取的数据进行马尔可夫过程分析
        if (filteredKlines.isEmpty()) {
            System.out.println("没有找到符合条件的K线数据进行马尔可夫分析");
            return null;
        }
        
        // 进行初始分析
        MarkovAnalysisResult result = analyzeKlineDataWithMarkov(filteredKlines, targetDate.format(DATE_FORMATTER) + "_ORDER_PRE_ANALYSIS");
        
        // 检查当前趋势是否适合订单方向
        if (result != null && result.isSuccess() && action != null && !action.isEmpty()) {
            boolean isSuitable = false;
            int currentIndex = filteredKlines.size() - 1;
            int dailyKlinesSize = dailyKlines.size();
            
            // 寻找该K线在全天数据中的索引位置
            for (int i = 0; i < dailyKlinesSize; i++) {
                if (dailyKlines.get(i).getTimestamp().equals(filteredKlines.get(currentIndex).getTimestamp())) {
                    currentIndex = i;
                    break;
                }
            }
            
            // 获取订单的结束时间（使用valueTime数组的最后一个元素）
            int orderEndIndex = dailyKlinesSize - 1; // 默认使用当天结束时间
            String[] valueTimeArray = orderTimeSeries.getValueTime();
            if (valueTimeArray != null && valueTimeArray.length > 0) {
                String lastOrderTime = valueTimeArray[valueTimeArray.length - 1];
                // 尝试解析lastOrderTime为LocalDateTime对象
                LocalDateTime lastOrderDateTime = null;
                try {
                    // 尝试多种时间格式解析
                    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
                    lastOrderDateTime = LocalDateTime.parse(lastOrderTime, formatter1);
                } catch (DateTimeParseException e1) {
                    try {
                        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                        lastOrderDateTime = LocalDateTime.parse(lastOrderTime, formatter2);
                    } catch (DateTimeParseException e2) {
                        try {
                            DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            lastOrderDateTime = LocalDateTime.parse(lastOrderTime, formatter3);
                        } catch (DateTimeParseException e3) {
                            System.err.println("无法解析订单结束时间: " + lastOrderTime);
                        }
                    }
                }
                
                // 在dailyKlines中找到订单结束时间对应的索引
                if (lastOrderDateTime != null) {
                    for (int i = 0; i < dailyKlinesSize; i++) {
                        LocalDateTime klineTime = dailyKlines.get(i).getTimestamp();
                        // 比较时间是否相等，或者klineTime是最接近lastOrderDateTime的下一个时间点
                        if (klineTime.isEqual(lastOrderDateTime) || klineTime.isAfter(lastOrderDateTime)) {
                            orderEndIndex = i;
                            break;
                        }
                    }
                    System.out.println("订单结束时间: " + lastOrderTime + "，对应索引: " + orderEndIndex);
                }
            }
            
            // 逐步尝试增加数据点，直到找到合适的时间点或达到订单结束时间
            while (!isSuitable && currentIndex < orderEndIndex) {
                // 获取最后一个分段的趋势类型
                List<AdvancedMarkovModel.SegmentReport> segmentReports = result.getSegmentReports();
                if (segmentReports != null && !segmentReports.isEmpty()) {
                    AdvancedMarkovModel.SegmentReport lastSegment = segmentReports.get(segmentReports.size() - 1);
                    String trendType = lastSegment.getTrendType();
                    
                    System.out.println("当前分析时间点: " + filteredKlines.get(filteredKlines.size() - 1).getTimestamp() + ", 趋势类型: " + trendType);
                    
                    // 判断当前趋势是否适合订单方向
                    if ("多单".equals(action)) {
                        if ("趋势上涨".equals(trendType)) {
                            isSuitable = true;
                            System.out.println("✓ 当前时间节点适合做多"+"订单id=="+orderTimeSeries.getOrderId());
                            break;
                        } else {
                            System.out.println("✗ 当前时间节点不适合做多，尝试增加数据点...id===="+orderTimeSeries.getOrderId());
                        }
                    } else if ("空单".equals(action)) {
                        if ("趋势下跌".equals(trendType)) {
                            isSuitable = true;
                            System.out.println("✓ 当前时间节点适合做空"+"订单id=="+orderTimeSeries.getOrderId());
                            break;
                        } else {
                            System.out.println("✗ 当前时间节点不适合做空，尝试增加数据点... id==="+orderTimeSeries.getOrderId());
                        }
                    }
                }
                
                // 如果当前时间点不适合，且还有更多数据点可用，则增加数据点并重新分析
                if (!isSuitable && currentIndex + 1 < orderEndIndex) {
                    currentIndex++;
                    List<KlineData> extendedKlines = dailyKlines.subList(0, currentIndex + 1);
                    result = analyzeKlineDataWithMarkov(extendedKlines, targetDate.format(DATE_FORMATTER) + "_ORDER_PRE_ANALYSIS_EXTENDED");
                } else {
                    break;
                }
            }
            
            if (!isSuitable) {
                System.out.println("本订单无合适时间点买入");
            }
        }
        
        return result;
    }
    
    /**
     * 使用ImprovedAdvancedMarkovModel分析K线数据
     * @param klines K线数据列表
     * @param analysisId 分析标识
     * @return 马尔可夫分析结果
     */
    private static MarkovAnalysisResult analyzeKlineDataWithMarkov(List<KlineData> klines, String analysisId) {
        // 准备ImprovedAdvancedMarkovModel所需的数据格式
        int dataSize = klines.size();
        double[] closeValues = new double[dataSize];
        String[] timeStrings = new String[dataSize];
        
        // 填充数据
        for (int i = 0; i < dataSize; i++) {
            KlineData kline = klines.get(i);
            closeValues[i] = kline.getClose();
            // 格式化为适合ImprovedAdvancedMarkovModel的时间格式
            timeStrings[i] = kline.getTimestamp().format(DATE_FORMATTER) + " " + 
                            kline.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        
        // 创建ImprovedAdvancedMarkovModel并进行分析
        ImprovedAdvancedMarkovModel model = new ImprovedAdvancedMarkovModel(closeValues, timeStrings, analysisId);
        MarkovAnalysisResult result = model.getMarkovResult();
        
        // 打印分析结果
        printMarkovAnalysisResult(result);
        
        return result;
    }
    
    /**
     * 判断订单是多单还是空单
     * @param orderTimeSeries 订单时间序列对象
     * @return 订单类型："多单"、"空单"或"无法判断"
     */
    public  String determineOrderType(OrderTimeSeries orderTimeSeries) {
        if (orderTimeSeries == null || orderTimeSeries.getClose() == null || orderTimeSeries.getValues() == null) {
            return "无法判断: 订单数据为空";
        }
        
        double[] close = orderTimeSeries.getClose();
        double[] values = orderTimeSeries.getValues();
        
        // 检查数据长度是否足够
        if (close.length < 2 || values.length < 2) {
            return "无法判断: 数据点不足";
        }
        
        // 首先尝试使用索引0和1进行比较
        String result = determineOrderTypeByIndices(close, values, 0, 1);
        
        // 如果返回"无法判断"（即close[0] == close[1]且values[0] == values[1]），尝试使用索引1和3
        if ("无法判断".equals(result) && close.length >= 4 && values.length >= 4) {
            result = determineOrderTypeByIndices(close, values, 1, 3);
        }
        
        return result;
    }
    
    /**
     * 根据指定索引位置判断订单类型
     * @param close 收盘价数组
     * @param values 收益值数组
     * @param index1 第一个索引
     * @param index2 第二个索引
     * @return 订单类型："多单"、"空单"或"无法判断"
     */
    private static String determineOrderTypeByIndices(double[] close, double[] values, int index1, int index2) {
        double close1 = close[index1];
        double close2 = close[index2];
        double value1 = values[index1];
        double value2 = values[index2];
        
        // 处理浮点数比较，使用一个小的误差范围
        final double EPSILON = 1e-6;
        
        // 判断close值的关系
        if (Math.abs(close1 - close2) < EPSILON) {
            // close值相等
            return "无法判断";
        } else if (close1 > close2) {
            // close1 > close2的情况
            if (Math.abs(value1 - value2) < EPSILON) {
                return "无法判断";
            } else if (value1 > value2) {
                // values变化方向与close相同，是多单
                return "多单";
            } else {
                // values变化方向与close相反，是空单
                return "空单";
            }
        } else {
            // close1 < close2的情况
            if (Math.abs(value1 - value2) < EPSILON) {
                return "无法判断";
            } else if (value1 < value2) {
                // values变化方向与close相同，是多单
                return "多单";
            } else {
                // values变化方向与close相反，是空单
                return "空单";
            }
        }
    }
}