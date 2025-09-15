package com.demo.extract.services;



import java.util.Arrays;

/**
 * 实现与 sklearn.metrics.pairwise.cosine_similarity 相同的余弦相似度计算
 */
public class CosineSimilarity {

    /**
     * 计算余弦相似度矩阵
     * @param vectors 标准化后的特征矩阵 [样本数][特征数]
     * @return 相似度矩阵 [样本数][样本数]
     */
    public double[][] compute(double[][] vectors) {
        int n = vectors.length;
        double[][] similarityMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double similarity = computePairwise(vectors[i], vectors[j]);
                similarityMatrix[i][j] = similarity;
                similarityMatrix[j][i] = similarity; // 对称矩阵
            }
        }
        return similarityMatrix;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double computePairwise(double[] v1, double[] v2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            if (Double.isNaN(v1[i]) || Double.isNaN(v2[i])) {
                continue; // 跳过含NaN的索引
            }
            dotProduct += v1[i] * v2[i];
            norm1 += Math.pow(v1[i], 2);
            norm2 += Math.pow(v2[i], 2);

        }

        // 处理零向量情况
        if (norm1 <= 1e-10 || norm2 <= 1e-10) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算向量与矩阵的余弦相似度（Python中的行向量比对）
     */
    public double[] computeVectorToMatrix(double[] vector, double[][] matrix) {
        return Arrays.stream(matrix)
                .mapToDouble(row -> computePairwise(vector, row))
                .toArray();
    }
}
