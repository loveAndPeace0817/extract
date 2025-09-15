package com.demo.extract.services;

import com.demo.extract.model.DecisionResult;
import com.demo.extract.DTO.OrderTimeSeries;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 多因子加权决策评估器
 * 1. 根据相似订单的收益变化趋势生成决策建议
 * 2. 验证决策结果的正确性
 */





/**
 * 多因子加权决策评估器（兼容Java 8+）
 */
public class DecisionEvaluator {
    private final double keyTime1;
    private final double keyTime2;
    private final double decisionThreshold;

    public DecisionEvaluator(double keyTime1, double keyTime2, double decisionThreshold) {
        this.keyTime1 = keyTime1;
        this.keyTime2 = keyTime2;
        this.decisionThreshold = decisionThreshold;
    }

    public DecisionEvaluator() {
        this(0.2, 0.25, 0.5); // 默认参数
    }

    // --------------------------
    // 内部类替代record
    // --------------------------

    private static final class WeightScores {
        final double holdScore;
        final double closeScore;

        WeightScores(double holdScore, double closeScore) {
            this.holdScore = holdScore;
            this.closeScore = closeScore;
        }
    }

    private static final class ValidationMetrics {
        final double time1Value;
        final double time2Value;
        final boolean isCorrect;

        ValidationMetrics(double time1Value, double time2Value, boolean isCorrect) {
            this.time1Value = time1Value;
            this.time2Value = time2Value;
            this.isCorrect = isCorrect;
        }
    }

    // --------------------------
    // 公开API
    // --------------------------

    /*public DecisionResult evaluate(
            OrderTimeSeries target,
            List<SimilarityService.SimilarityResult> similarOrders,
            List<OrderTimeSeries> allSeries) {

        // 1. 计算权重
        WeightScores scores = calculateWeightedScores(similarOrders, allSeries);

        // 2. 生成决策
        String decision = makeDecision(scores);

        // 3. 验证结果
        ValidationMetrics metrics = validateDecision(target, decision);

        return new DecisionResult(
                target.getOrderId(),
                decision,
                metrics.isCorrect,
                scores.holdScore,
                scores.closeScore,
                metrics.time1Value,
                metrics.time2Value,
                similarOrders.stream()
                        .map(SimilarityService.SimilarityResult::getOrderId)
                        .collect(Collectors.toList())
        );
    }*/

    // --------------------------
    // 核心逻辑
    // --------------------------

   /* private WeightScores calculateWeightedScores(
            List<SimilarityService.SimilarityResult> similarOrders,
            List<OrderTimeSeries> allSeries) {

        double holdScore = 0;
        double closeScore = 0;

        for (SimilarityService.SimilarityResult order : similarOrders) {
            OrderTimeSeries refSeries = findSeriesById(allSeries, order.getOrderId());
            double val1 = refSeries.getValueAtTime(keyTime1);
            double val2 = refSeries.getValueAtTime(keyTime2);

            if (val2 < val1) {
                holdScore += order.getSimilarity();
            } else {
                closeScore += order.getSimilarity();
            }
        }

        // 归一化
        double total = holdScore + closeScore;
        if (total > 0) {
            holdScore /= total;
            closeScore /= total;
        }

        return new WeightScores(holdScore, closeScore);
    }*/

    private String makeDecision(WeightScores scores) {
        return scores.holdScore >= decisionThreshold ? "hold" : "close";
    }

    private ValidationMetrics validateDecision(OrderTimeSeries target, String decision) {
        double t1Val = target.getValueAtTime(keyTime1);
        double t2Val = target.getValueAtTime(keyTime2);
        boolean isCorrect = "hold".equals(decision) ?
                (t2Val > t1Val) : (t2Val <= t1Val);
        return new ValidationMetrics(t1Val, t2Val, isCorrect);
    }

    // --------------------------
    // 工具方法
    // --------------------------

    private OrderTimeSeries findSeriesById(List<OrderTimeSeries> series, String orderId) {
        return series.stream()
                .filter(s -> s.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未找到订单: " + orderId));
    }

    private boolean isTrendPositive(double val1, double val2) {
        return val2 > val1;
    }
}