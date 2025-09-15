package com.demo.extract.test;



import lombok.Getter;

import java.util.List;

@Getter
public class DecisionResult {
    private final String targetOrder;
    private final String decision;
    private final boolean isCorrect;
    private final double holdScore;
    private final double closeScore;
    private final double time1Value;
    private final double time2Value;
    private final List<String> similarOrders;

    public DecisionResult(String targetOrder, String decision, boolean isCorrect,
                          double holdScore, double closeScore,
                          double time1Value, double time2Value,
                          List<String> similarOrders) {
        this.targetOrder = targetOrder;
        this.decision = decision;
        this.isCorrect = isCorrect;
        this.holdScore = holdScore;
        this.closeScore = closeScore;
        this.time1Value = time1Value;
        this.time2Value = time2Value;
        this.similarOrders = similarOrders;
    }

    public void printDetailedReport() {
        System.out.println("\n========== 订单决策明细 ==========");
        System.out.printf("目标订单: %s%n", targetOrder);
        System.out.printf("最终决策: %s%n", decision.equals("hold") ? "持仓" : "平仓");
        System.out.printf("持仓权重: %.2f%%%n", holdScore * 100);
        System.out.printf("平仓权重: %.2f%%%n", closeScore * 100);
        System.out.printf("关键时间点1 (%.2f) 收益: %.2f%n", 0.2, time1Value);
        System.out.printf("关键时间点2 (%.2f) 收益: %.2f%n", 0.25, time2Value);
        System.out.printf("验证结果: %s%n", isCorrect ? "✓" : "✗");
        System.out.println("相似Top5订单: " + similarOrders);
    }
}