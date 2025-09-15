package com.demo.extract.model;



import com.demo.extract.util.CsvWriter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 决策结果模型
 * 存储订单决策测试的完整结果
 */
@Setter
@Getter
@Data
public class DecisionResult {
    private final String orderId;
    private  String decision;
    private final boolean isCorrect;
    private final double holdScore;
    private final double closeScore;
    private final double time1Value;
    private final double time2Value;
    private final List<String> similarOrders;
    private final List<String> scoreMap;
    private double simlierScore;
    private Boolean targetOrder = false;

    /**
     * 构造函数
     * @param orderId 订单ID
     * @param decision 决策结果 ("hold"/"close")
     * @param isCorrect 判断是否正确
     * @param holdScore 持仓权重
     * @param closeScore 平仓权重
     * @param time1Value 时间点1的收益
     * @param time2Value 时间点2的收益
     * @param similarOrders 相似订单ID列表
     */
    public DecisionResult(String orderId,
                          String decision,
                          boolean isCorrect,
                          double holdScore,
                          double closeScore,
                          double time1Value,
                          double time2Value,
                          List<String> similarOrders,  List<String> scoreMap,double simlierScore) {
        this.orderId = Objects.requireNonNull(orderId);
        this.decision = validateDecision(decision);
        this.isCorrect = isCorrect;
        this.holdScore = validateScore(holdScore);
        this.closeScore = validateScore(closeScore);
        this.time1Value = time1Value;
        this.time2Value = time2Value;
        this.similarOrders =similarOrders;
        this.scoreMap = scoreMap;
        this.simlierScore = simlierScore;
    }

    private String validateDecision(String decision) {
        if (!"hold".equals(decision) && !"close".equals(decision)) {
            throw new IllegalArgumentException("决策必须是'hold'或'close'");
        }
        return decision;
    }

    private double validateScore(double score) {
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("权重必须在[0,1]范围内");
        }
        return score;
    }

    // Getter 方法
    public String getOrderId() {
        return orderId;
    }

    public String getDecision() {
        return decision;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public double getHoldScore() {
        return holdScore;
    }

    public double getCloseScore() {
        return closeScore;
    }

    public double getTime1Value() {
        return time1Value;
    }

    public double getTime2Value() {
        return time2Value;
    }

    public List<String> getSimilarOrders() {
        return similarOrders;
    }

    /**
     * 获取决策的中文描述
     */
    public String getDecisionInChinese() {
        return "hold".equals(decision) ? "持仓" : "平仓";
    }

    /**
     * 获取验证结果符号
     */
    public String getValidationSymbol() {
        return isCorrect ? "✓" : "✗";
    }

    @Override
    public String toString() {
        return String.format(
                "DecisionResult[orderId=%s, decision=%s, correct=%b, hold=%.2f%%, close=%.2f%%]",
                orderId, decision, isCorrect, holdScore * 100, closeScore * 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecisionResult that = (DecisionResult) o;
        return isCorrect == that.isCorrect &&
                Double.compare(that.holdScore, holdScore) == 0 &&
                Double.compare(that.closeScore, closeScore) == 0 &&
                Double.compare(that.time1Value, time1Value) == 0 &&
                Double.compare(that.time2Value, time2Value) == 0 &&
                orderId.equals(that.orderId) &&
                decision.equals(that.decision) &&
                similarOrders.equals(that.similarOrders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, decision, isCorrect, holdScore,
                closeScore, time1Value, time2Value, similarOrders);
    }

    public void printDetailedReport() {
      /*  System.out.println("\n========== 决策分析报告 ==========");
        System.out.printf("目标订单: %s%n", orderId);
        System.out.printf("最终决策: %s%n", decision.equals("hold") ? "持仓" : "平仓");
        //System.out.printf("持仓权重: %.2f%%%n", holdScore * 100);
        //System.out.printf("平仓权重: %.2f%%%n", closeScore * 100);
        System.out.println("相似订单均值： "+simlierScore);
        System.out.printf("时间点1 (%.2f) 收益: %.2f%n", 0.2, time1Value);
        System.out.printf("时间点2 (%.2f) 收益: %.2f%n", 0.25, time2Value);
        System.out.printf("验证结果: %s%n", isCorrect ? "✓ 正确" : "✗ 错误");*/




       /* System.out.println("\n【相似订单Top5】");
        for (int i = 0; i < Math.min(similarOrders.size(), 5); i++) {
            System.out.printf("%d. %s%n", i+1, similarOrders.get(i));
        }
        System.out.println("\n【相似订单Top5_权重分数】");
        for (int i = 0; i < Math.min(scoreMap.size(), 5); i++) {
            System.out.printf("%d. %s%n", i+1, scoreMap.get(i));
        }

        System.out.println("\n【趋势分析】");
        if (time2Value > time1Value) {
            System.out.printf("目标订单趋势: %.2f → %.2f (↑ 上升)%n", time1Value, time2Value);
        } else {
            System.out.printf("目标订单趋势: %.2f → %.2f (↓ 下降)%n", time1Value, time2Value);
        }

        System.out.printf("决策建议: %s%n",
                decision.equals("hold") ? "建议持仓" : "建议平仓");
*/
    }


}
