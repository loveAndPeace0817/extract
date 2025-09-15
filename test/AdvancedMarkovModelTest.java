package com.demo.extract.test;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.model.AdvancedMarkovModel;
import com.demo.extract.model.AdvancedMarkovModel.SegmentReport;
import com.demo.extract.model.MarkovAnalysisResult;
import com.demo.extract.services.DataLoaderNew;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 高级马尔可夫模型测试类
 * 测试AdvancedMarkovModel的各项功能
 */
public class AdvancedMarkovModelTest {
    public static void main(String[] args) throws IOException {
        System.out.println("=== 高级马尔可夫模型测试开始 ===");
        
        // 读取测试数据
        AdvancedMarkovModel model = readData();
        if (model == null) {
            System.out.println("无法加载测试数据，测试终止。");
            return;
        }
        
        // 执行自动分段
        model.autoSegment();
        
        // 分析分段
        List<SegmentReport> segmentReports = model.analyzeSegments();
        
        // 打印分段报告
        printSegmentReports(segmentReports);
        
        // 整体评估
        OverallEvaluation overall = model.evaluateOverall();
        
        // 打印整体评估
        printOverallEvaluation(overall);
        
        // 测试getMarkovResult方法
        testMarkovResult(model);
        
        System.out.println("=== 高级马尔可夫模型测试结束 ===");
    }
    
    /**
     * 从CSV文件加载测试数据
     */
    public static AdvancedMarkovModel readData() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        
        for (OrderTimeSeries orderTimeSeries : allSeries) {
            if (orderTimeSeries.getValues().length >= 70) {
                int endIndex = (int)(orderTimeSeries.getValues().length * 0.9); // 计算90%位置
                double[] values = Arrays.copyOfRange(orderTimeSeries.getValues(), 0, endIndex);
                String[] valueTime = Arrays.copyOfRange(orderTimeSeries.getValueTime(), 0, endIndex);
                String orderId = orderTimeSeries.getOrderId();
                
                System.out.println("加载数据成功：订单ID=" + orderId + ", 数据点数=" + values.length);
                return new AdvancedMarkovModel(values, valueTime, orderId);
            }
        }
        return null;
    }
    
    /**
     * 打印分段报告
     */
    private static void printSegmentReports(List<SegmentReport> segmentReports) {
        System.out.println("\n=== 高级马尔可夫模型分段分析 ===");
        System.out.printf("%-15s %-10s %-10s %-12s %-12s %-12s %-10s %-10s%n",
                "时间范围", "持续时间(min)", "趋势类型", "平均收益(bps)", "最大回撤(%)",
                "波动率", "Hurst指数", "趋势强度");
        
        for (SegmentReport report : segmentReports) {
            System.out.printf("%-15s %-10d %-10s %-12.1f %-12.1f %-12.1f %-10.2f %-10s%n",
                    report.getTimeRange(),   // 时间范围
                    report.getDuration(),    // 持续时间
                    report.getTrendType(),   // 趋势类型
                    report.getMeanReturn(),  // 平均收益
                    report.getMaxDrawdown(), // 最大回撤
                    report.getVolatility(),  // 波动率
                    report.getHurst(),       // Hurst指数
                    report.getTrendStrength()); // 趋势强度
        }
    }
    
    /**
     * 打印整体评估结果
     */
    private static void printOverallEvaluation(OverallEvaluation overall) {
        System.out.println("\n=== 整体趋势评估 ===");
        System.out.println("整体趋势: " + overall.getOverallTrend());
        System.out.println("平均收益(bps): " + overall.getTotalReturn());
        System.out.println("整体Hurst指数: " + overall.getHurstIndex());
        System.out.println("波动率: " + overall.getVolatility());
        System.out.println("趋势持续性: " + overall.getPersistence());
    }
    
    /**
     * 测试getMarkovResult方法
     */
    private static void testMarkovResult(AdvancedMarkovModel model) {
        System.out.println("\n=== 马尔可夫决策结果测试 ===");
        long startTime = System.currentTimeMillis();
        
        MarkovAnalysisResult result = model.getMarkovResult();
        
        long endTime = System.currentTimeMillis();
        result.setProcessingTime(endTime - startTime);
        
        System.out.println("生成决策结果耗时: " + result.getProcessingTime() + "ms");
        System.out.println("决策建议: " + result.getDecisionAdvice());
        
        // 验证结果是否包含所有必要信息
        System.out.println("分段数量: " + result.getSegmentCount());
        System.out.println("分析是否成功: " + (result.isSuccess() ? "是" : "否"));
        if (result.isSuccess()) {
            System.out.println("整体评估趋势: " + result.getOverallEvaluation().getOverallTrend());
            System.out.println("\n简要分析报告:");
            System.out.println(result.getSummary());
        }
    }
    
    /**
     * 测试不同参数配置下的模型表现
     */
    public static void testParameterSensitivity() throws IOException {
        System.out.println("\n=== 参数敏感性测试 ===");
        
        // 这里可以添加更多测试方法，测试不同参数配置下的模型表现
        // 例如修改波动率阈值、Hurst指数阈值等
    }
}