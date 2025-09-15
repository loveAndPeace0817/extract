package com.demo.extract.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 完整分析结果响应实体
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private String status;          // 状态（success/error）
    private String message;         // 附加信息
    private String timestamp;       // 分析时间
    @JsonProperty("segment_analysis")
    private List<SegmentAnalysis> segmentAnalysis; // 分段分析
    @JsonProperty("overall_evaluation")
    private OverallEvaluation overallEvaluation;   // 整体评估 111
}