package com.demo.extract.model;

import com.demo.extract.DTO.OverallEvaluation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 马尔可夫模型分析结果
 * 存储AdvancedMarkovModel的分析结果数据
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarkovAnalysisResult {
    private List<AdvancedMarkovModel.SegmentReport> segmentReports; // 分段分析报告
    private OverallEvaluation overallEvaluation; // 整体评估结果
    private String decisionAdvice; // 决策建议
    private long processingTime; // 处理时间（毫秒）
    private String orderId; // 订单ID
    private int dataPointsCount; // 数据点数量
    
    /**
     * 构造函数
     * @param segmentReports 分段分析报告
     * @param overallEvaluation 整体评估结果
     * @param decisionAdvice 决策建议
     */
    public MarkovAnalysisResult(List<AdvancedMarkovModel.SegmentReport> segmentReports, 
                              OverallEvaluation overallEvaluation, 
                              String decisionAdvice) {
        this.segmentReports = segmentReports;
        this.overallEvaluation = overallEvaluation;
        this.decisionAdvice = decisionAdvice;
        this.processingTime = 0;
        this.orderId = "";
        this.dataPointsCount = 0;
    }
    
    /**
     * 获取分段数量
     */
    public int getSegmentCount() {
        return segmentReports != null ? segmentReports.size() : 0;
    }
    
    /**
     * 判断分析是否成功
     */
    public boolean isSuccess() {
        return segmentReports != null && !segmentReports.isEmpty() && overallEvaluation != null;
    }
    
    /**
     * 获取简要报告信息
     */
    public String getSummary() {
        if (!isSuccess()) {
            return "分析失败：缺少必要的分析结果数据";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("马尔可夫模型分析结果\n");
        summary.append("订单ID: ").append(orderId).append("\n");
        summary.append("整体趋势: ").append(overallEvaluation.getOverallTrend()).append("\n");
        summary.append("平均收益: ").append(overallEvaluation.getTotalReturn()).append(" bps\n");
        summary.append("波动率: ").append(overallEvaluation.getVolatility()).append("\n");
        summary.append("趋势持续性: ").append(overallEvaluation.getPersistence()).append("\n");
        summary.append("分段数量: ").append(getSegmentCount()).append("\n");
        summary.append("决策建议: ").append(decisionAdvice).append("\n");
        
        return summary.toString();
    }
}