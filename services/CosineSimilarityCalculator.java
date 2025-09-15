package com.demo.extract.services;

import com.demo.extract.DTO.OrderFeatures;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CosineSimilarityCalculator {
    public  double[][] computeCosineMatrix(List<OrderFeatures> features) {
        int n = features.size();
        double[][] matrix = new double[n][n];

        // 提取数值特征（跳过orderId）
        double[][] vectors = features.stream()
                .map(s -> new double[]{
                        s.getCount(), s.getMean(), s.getStd(),
                        s.getMin(), s.getMax(), s.getMedian(),
                        s.getQ25(), s.getQ75(), s.getSkewness(),
                        s.getKurtosis(), s.getRange(), s.getFirstValue(),
                        s.getLastValue(), s.getAbsMax(), s.getAbsMean(),
                        s.getPosCount(), s.getNegCount(), s.getZeroCount(),
                        s.getTrendSlope(), s.getTrendIntercept(), s.getTrendRValue()
                })
                .toArray(double[][]::new);

        // 计算余弦相似度
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double dot = 0, normA = 0, normB = 0;
                for (int k = 0; k < vectors[i].length; k++) {
                    dot += vectors[i][k] * vectors[j][k];
                    normA += Math.pow(vectors[i][k], 2);
                    normB += Math.pow(vectors[j][k], 2);
                }
                matrix[i][j] = matrix[j][i] = dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-8);
            }
        }
        return matrix;
    }
}
