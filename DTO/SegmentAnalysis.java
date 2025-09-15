package com.demo.extract.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分段分析结果实体
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentAnalysis {
    private Integer phase;         // 阶段序号
    @JsonProperty("time_range")
    private TimeRange timeRange;    // 时间范围
    @JsonProperty("duration_min")
    private Double durationMin;     // 持续时间（分钟）
    @JsonProperty("trend_type")
    private String trendType;       // 趋势类型
    private Double strength;        // 趋势强度
    private Double hurst;           // Hurst指数
    @JsonProperty("start_price")
    private Double startPrice;      // 起始价格
    @JsonProperty("end_price")
    private Double endPrice;        // 结束价格
    @JsonProperty("max_drawdown")
    private Double maxDrawdown;     // 最大回撤
}
