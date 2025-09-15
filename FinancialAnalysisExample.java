package com.demo.extract;

import com.demo.extract.DTO.AnalysisResult;
import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.client.markovClient;

import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FinancialAnalysisExample {

    private final markovClient analysisClient;

    public FinancialAnalysisExample(markovClient analysisClient) {
        this.analysisClient = analysisClient;
    }

    /**
     * 模拟生成测试数据
     */
    private List<FinancialDataPoint> generateTestData() {
        List<FinancialDataPoint> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 模拟下跌趋势数据
        for (int i = 0; i < 30; i++) {
            LocalDateTime time = LocalDateTime.now().minusMinutes(30 - i);
            data.add(new FinancialDataPoint(
                    -50.0 + i * 1.5,
                    time.format(formatter)
            ));
        }

        // 模拟震荡数据
        for (int i = 0; i < 20; i++) {
            LocalDateTime time = LocalDateTime.now().plusMinutes(i);
            data.add(new FinancialDataPoint(
                    -10.0 + (i % 3) * 5.0,
                    time.format(formatter)
            ));
        }

        return data;
    }

    /**
     * 执行分析并打印结果
     */
    public void executeAnalysis() {

        // 1. 准备数据
        List<FinancialDataPoint> marketData = generateTestData();

        // 2. 调用分析服务（带重试）
        AnalysisResult result = analysisClient.analyzeWithRetry(marketData, 3);

        // 3. 处理结果
        if ("success".equals(result.getStatus())) {
            System.out.println("=== 市场阶段分析 ===");
            result.getSegmentAnalysis().forEach(segment -> {
                System.out.printf(
                        "阶段%d: %s - %s | 类型: %s | 强度: %.2f | Hurst: %.3f%n",
                        segment.getPhase(),
                        segment.getTimeRange().getStart(),
                        segment.getTimeRange().getEnd(),
                        segment.getTrendType(),
                        segment.getStrength(),
                        segment.getHurst()
                );
            });

            System.out.println("\n=== 整体评估 ===");
            OverallEvaluation overall = result.getOverallEvaluation();

            System.out.printf(
                    "趋势: %s | 累计收益: %.1f | 波动率: %.2f | 持续性: %s%n",
                    overall.getOverallTrend(),
                    overall.getTotalReturn(),
                    overall.getVolatility(),
                    overall.getPersistence()
            );
        } else {
            System.err.println("分析失败: " + result.getMessage());
        }

    }

    public static void main(String[] args) {
        // 实际Spring Boot应用中通过依赖注入
        RestTemplate restTemplate = new RestTemplate();
        markovClient client = new markovClient(restTemplate);

        FinancialAnalysisExample example = new FinancialAnalysisExample(client);
        example.executeAnalysis();
    }
}
