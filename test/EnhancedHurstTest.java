package com.demo.extract.test;

import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.EnhancedHurstCalculator;
import com.demo.extract.model.ThreeMarkovModel;
import com.demo.extract.services.DataLoaderNew;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * 测试增强版Hurst指数计算器的效果
 */
public class EnhancedHurstTest {

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void main(String[] args) throws IOException {
        // 加载测试数据

        List<FinancialDataPoint> dataPoints = readData();

        if (dataPoints.isEmpty()) {
            System.out.println("数据加载失败，请检查文件路径和格式");
            return;
        }

        // 提取数据值
        double[] values = dataPoints.stream()
                .mapToDouble(FinancialDataPoint::getValue)
                .toArray();

        // 使用原始方法计算Hurst指数
        ThreeMarkovModel originalModel = new ThreeMarkovModel(values, 
                dataPoints.stream().map(FinancialDataPoint::getTimestamp).toArray(String[]::new), "");
        originalModel.autoSegment();
        List<ThreeMarkovModel.SegmentReport> originalReports = originalModel.analyzeSegments();
        ThreeMarkovModel.OverallEvaluation originalEval = originalModel.evaluateOverall();

        // 使用增强版方法计算Hurst指数
        double enhancedHurst = EnhancedHurstCalculator.calculateHurst(values);
        String enhancedPersistence = EnhancedHurstCalculator.determinePersistence(enhancedHurst);

        // 输出对比结果
        System.out.println("====== 原始方法 vs 增强版方法 对比结果 ======");
        System.out.println("原始方法Hurst指数: " + originalEval.getHurst());
        System.out.println("原始方法趋势持续性: " + originalEval.getPersistence());
        System.out.println("增强版方法Hurst指数: " + enhancedHurst);
        System.out.println("增强版方法趋势持续性: " + enhancedPersistence);
        System.out.println();

        // 为了演示，我们使用简单的等间隔分段来比较
        System.out.println("====== 分段分析对比 (使用等间隔分段) ======");
        System.out.println("分段\t原始方法Hurst\t增强版Hurst\t原始方法强度\t增强版强度");

        int segmentCount = Math.min(5, originalReports.size());
        int segmentSize = values.length / segmentCount;

        for (int i = 0; i < segmentCount; i++) {
            int start = i * segmentSize;
            int end = (i == segmentCount - 1) ? values.length - 1 : (i + 1) * segmentSize - 1;
            double[] segmentValues = Arrays.copyOfRange(values, start, end + 1);

            // 原始方法计算
            double originalSegmentHurst = 0.5; // 模拟值，实际应用中需要获取
            String originalSegmentStrength = "中"; // 模拟值
            if (i < originalReports.size()) {
                originalSegmentHurst = originalReports.get(i).getHurst();
                originalSegmentStrength = originalReports.get(i).getTrendStrength();
            }

            // 增强版方法计算
            double enhancedSegmentHurst = EnhancedHurstCalculator.calculateHurst(segmentValues);
            String enhancedSegmentStrength = EnhancedHurstCalculator.determineTrendStrength(segmentValues, enhancedSegmentHurst);

            System.out.printf("%d\t%.2f\t\t%.2f\t\t%s\t\t%s\n",
                    i + 1,
                    originalSegmentHurst,
                    enhancedSegmentHurst,
                    originalSegmentStrength,
                    enhancedSegmentStrength);
        }
    }


    public static  List<FinancialDataPoint> readData() throws IOException {
        List<FinancialDataPoint> result = new ArrayList<>();
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        for (int i = 0; i < allSeries.size(); i++) {
            OrderTimeSeries orderTimeSeries = allSeries.get(i);
            if(orderTimeSeries.getValues().length>=70){
                int endIndex = (int)(orderTimeSeries.getValues().length * 0.9);     // 计算80%位置
                double[] values = Arrays.copyOfRange(orderTimeSeries.getValues(), 0, endIndex);
                String[] valueTime = Arrays.copyOfRange(orderTimeSeries.getValueTime(), 0, endIndex);
                String orderId = orderTimeSeries.getOrderId();
                result.addAll(parseTrueData(values, valueTime, orderId));
            }
        }
        return result;

    }
    private static List<FinancialDataPoint> parseTrueData(double[] values, String[] valueTime, String orderId) {
        List<FinancialDataPoint>dataPoints = new ArrayList<>();
        for (int i = 0; i < valueTime.length; i++) {
            double value = values[i];
            LocalDateTime timestamp = LocalDateTime.parse(valueTime[i], INPUT_FORMATTER);
            dataPoints.add(new FinancialDataPoint(value, timestamp.format(ISO_FORMATTER),orderId));
        }
        return dataPoints;
    }
}