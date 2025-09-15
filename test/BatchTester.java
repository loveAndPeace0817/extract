package com.demo.extract.test;





import com.demo.extract.model.DecisionResult;
import com.demo.extract.DTO.OrderFeatures;
import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.services.CosineSimilarityCalculator;
import com.demo.extract.services.DecisionEvaluator;
import com.demo.extract.services.SimilarityService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchTester {
    private final SimilarityService similarityService;
    private final DecisionEvaluator evaluator;


    public BatchTester(int threads) {
        this.similarityService = new SimilarityService(threads);
        this.evaluator = new DecisionEvaluator();
    }

    public void runBatchTest(List<OrderFeatures> features,
            List<OrderTimeSeries> allSeries,
            double testRatio,
            int limit) {

        AtomicInteger correctCount = new AtomicInteger();
        AtomicInteger holdCount = new AtomicInteger();
        // 2. 计算余弦相似度矩阵
        CosineSimilarityCalculator cosineSimilarityCalculator = new CosineSimilarityCalculator();
        double[][] cosineSim =cosineSimilarityCalculator.computeCosineMatrix(features);
        /*allSeries.stream()
                .limit(limit)
                .forEach(target -> {

                    List<SimilarityService.SimilarOrder> similarOrders = similarityService.findSimilarOrders(target, allSeries, cosineSim, testRatio, 5);

                    DecisionResult result = evaluator.evaluate(
                            target, similarOrders, allSeries);

                    printTestResult(result, testRatio, target);

                    if (result.isCorrect()) {
                        correctCount.incrementAndGet();
                    }
                    if ("hold".equals(result.getDecision())) {
                        holdCount.incrementAndGet();
                    }
                });*/

        printSummary(correctCount.get(), holdCount.get(), limit);
    }

    private void printTestResult(DecisionResult result, double ratio, OrderTimeSeries target) {
        System.out.printf("\n【订单%s】%n", result.getOrderId());
        System.out.printf("测试数据: 前%.0f%% (实际%d/%d点)%n",
                ratio * 100,
                (int)(target.getValues().length * ratio),
                target.getValues().length);
        System.out.printf("最终决策: %s%n", result.getDecision().equals("hold") ? "持仓" : "平仓");
        System.out.printf("持仓权重: %.2f%%%n", result.getHoldScore() * 100);
        System.out.printf("平仓权重: %.2f%%%n", result.getCloseScore() * 100);
        System.out.printf("0.2时收益: %.2f%n", result.getTime1Value());
        System.out.printf("0.25时收益: %.2f%n", result.getTime2Value());
        System.out.printf("相似Top5订单: %s%n", result.getSimilarOrders());
        System.out.printf("验证结果: %s%n", result.isCorrect() ? "✓" : "✗");

    }

    private void printSummary(int correctCount, int holdCount, int total) {
        System.out.println("\n========== 测试汇总 ==========");
        System.out.printf("测试订单数: %d%n", total);
        System.out.printf("平均准确率: %.2f%%%n", (double)correctCount / total * 100);
        System.out.printf("持仓决策比例: %.2f%%%n", (double)holdCount / total * 100);

    }
}
