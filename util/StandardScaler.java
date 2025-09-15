package com.demo.extract.util;



import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.Arrays;

/**
 * 实现与sklearn.preprocessing.StandardScaler相同的标准化逻辑
 * 特征标准化：(X - mean) / std
 */
public class StandardScaler {
    private double[] means;
    private double[] stds;

    /**
     * 标准化特征矩阵（列方向标准化）
     * @param data 输入矩阵[样本数][特征数]
     * @return 标准化后的矩阵
     */
    public double[][] standardize(double[][] data) {
        if (data == null || data.length == 0) {
            return new double[0][];
        }

        int numFeatures = data[0].length;
        this.means = new double[numFeatures];
        this.stds = new double[numFeatures];

        // 计算每列的均值和标准差
        for (int col = 0; col < numFeatures; col++) {
            double[] column = getColumn(data, col);
            means[col] = StatUtils.mean(column);
            stds[col] = new StandardDeviation().evaluate(column, means[col]);
        }

        // 执行标准化
        double[][] result = new double[data.length][numFeatures];
        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < numFeatures; col++) {
                // 处理标准差为0的情况（添加epsilon防止除零）
                result[row][col] = (data[row][col] - means[col]) / (stds[col] + 1e-8);
            }
        }
        return result;
    }
    // 先转换为对数收益率，再标准化(处理收盘价标准化)
    public double[][] preprocessClosePrices(double[][] prices) {
        double[][] returns = new double[prices.length][];
        for (int i = 0; i < prices.length; i++) {
            returns[i] = calculateLogReturns(prices[i]);
        }
        return new StandardScaler().standardize(returns);
    }

    private double[] calculateLogReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            returns[i-1] = Math.log(prices[i] / prices[i-1]);
        }
        return returns;
    }


    /**
     * 标准化单个样本（使用预计算的均值和标准差）
     */
    public double[] transform(double[] sample) {
        if (means == null || stds == null) {
            throw new IllegalStateException("必须先调用fit()方法");
        }

        double[] result = new double[sample.length];
        for (int i = 0; i < sample.length; i++) {
            result[i] = (sample[i] - means[i]) / (stds[i] + 1e-8);
        }
        return result;
    }

    /**
     * 获取矩阵的列数据
     */
    private double[] getColumn(double[][] matrix, int colIndex) {
        return Arrays.stream(matrix)
                .mapToDouble(row -> row[colIndex])
                .toArray();
    }

    /**
     * 对一维数组进行拟合和标准化转换
     * @param data 输入一维数组
     * @return 标准化后的数组
     */
    public double[] fitTransform(double[] data) {
        if (data == null || data.length == 0) {
            return new double[0];
        }

        // 计算均值和标准差
        this.means = new double[1];
        this.stds = new double[1];
        means[0] = StatUtils.mean(data);
        stds[0] = new StandardDeviation().evaluate(data, means[0]);

        // 执行标准化
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (data[i] - means[0]) / (stds[0] + 1e-8);
        }
        return result;
    }

    // Getter方法（用于保存模型参数）
    public double[] getMeans() {
        return means != null ? Arrays.copyOf(means, means.length) : null;
    }

    public double[] getStds() {
        return stds != null ? Arrays.copyOf(stds, stds.length) : null;
    }
}
