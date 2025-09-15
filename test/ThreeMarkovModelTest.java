package com.demo.extract.test;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.ThreeMarkovModel;
import com.demo.extract.model.ThreeMarkovModel.OverallEvaluation;
import com.demo.extract.model.ThreeMarkovModel.SegmentReport;
import com.demo.extract.services.DataLoaderNew;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ThreeMarkovModelTest {
    public static void main(String[] args) throws IOException {

        // 创建模型实例
        //ThreeMarkovModel model = new ThreeMarkovModel(testData);
        ThreeMarkovModel model = readData();
        // 执行分段
        model.autoSegment();

        // 分析分段
        List<SegmentReport> segmentReports = model.analyzeSegments();

        // 打印分段报告
        System.out.println("=== 三段式趋势分析 ===");
        System.out.printf("%-15s %-10s %-10s %-12s %-12s %-12s %-10s %-10s%n",
                "时间范围", "持续时间(min)", "趋势类型", "平均收益(bps)", "最大回撤(bps)",
                "波动率", "Hurst指数", "趋势强度");
        for (SegmentReport report : segmentReports) {
            System.out.printf("%-15s %-10d %-10s %-12.1f %-12.1f %-12.1f %-10.2f %-10s%n",
                    report.getTimeRange(),//时间范围
                    report.getDuration(),//持续时间
                    report.getTrendType(),//趋势类型
                    report.getMeanReturn(),//平均收益
                    report.getMaxDrawdown(),//最大回撤
                    report.getVolatility(),//波动率
                    report.getHurst(),//Hurst指数
                    report.getTrendStrength());//趋势强度
        }

        // 整体评估
        OverallEvaluation overall = model.evaluateOverall();

        // 打印整体评估
        System.out.println("\n=== 整体趋势评估 ===");
        System.out.println("整体趋势: " + overall.getOverallTrend());
        System.out.println("累计收益(bps): " + overall.getCumulativeReturn());
        System.out.println("整体Hurst指数: " + overall.getHurst());
        System.out.println("趋势持续性: " + overall.getPersistence());

        // 可视化（可选，如需运行可视化请取消注释）
        //model.visualize();
    }

    public static  ThreeMarkovModel readData() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        for (int i = 0; i < allSeries.size(); i++) {
            OrderTimeSeries orderTimeSeries = allSeries.get(i);
            if(orderTimeSeries.getValues().length>=70){
                int endIndex = (int)(orderTimeSeries.getValues().length * 0.9);     // 计算80%位置
                double[] values = Arrays.copyOfRange(orderTimeSeries.getValues(), 0, endIndex);
                String[] valueTime = Arrays.copyOfRange(orderTimeSeries.getValueTime(), 0, endIndex);
                String orderId = orderTimeSeries.getOrderId();
                ThreeMarkovModel model = new ThreeMarkovModel(values,valueTime,orderId);
                return model;
            }
        }
        return null;

    }
}