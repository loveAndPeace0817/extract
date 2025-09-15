package com.demo.extract.util;

import java.util.Arrays;


public class ScoreUtils {
    /**
     * 计算Softmax权重
     * @param scores 原始分数数组
     * @return 权重数组（总和为1）
     */
    public static double[] softmax(double[] scores) {
        double[] expScores = Arrays.stream(scores)
                .map(score -> Math.exp(score - max(scores))) // 防溢出优化
                .toArray();

        double sum = Arrays.stream(expScores).sum();
        return Arrays.stream(expScores)
                .map(exp -> exp / (sum + 1e-8)) // 防止除零
                .toArray();
    }

    private static double max(double[] arr) {
        return Arrays.stream(arr).max().orElse(0);
    }
}
