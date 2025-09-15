package com.demo.extract.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 整体评估结果实体
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OverallEvaluation {
    @JsonProperty("overall_trend")
    private String overallTrend;    // 整体趋势
    @JsonProperty("total_return")
    private Double totalReturn;     // 累计收益
    @JsonProperty("hurst_index")
    private Double hurstIndex;      // Hurst指数
    private Double volatility;      // 波动率
    private String persistence;     // 趋势持续性
}

