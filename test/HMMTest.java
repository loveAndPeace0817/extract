package com.demo.extract.test;

import com.demo.extract.client.markovClientJava;
import com.demo.extract.util.StandardScaler;
import java.util.Arrays;
import java.util.Map;

public class HMMTest {
    public static void main(String[] args) {
        // 用户提供的原始数据
        double[] originalReturns = {
            7.5, 30.45, 26.1, 27, 11.55, -1.2, 17.4, 12.3, 15.6, 4.05, -8.85, -21.15, -12.6, -10.35, -18.6, -13.2, 1.2, 9.3, 5.1, 0.45, -19.5, -22.65, -2.7, 6.15, 7.05, 13.2, 35.85, 34.65, 28.2, 36.6, 36, 36.3, 25.35, 37.5, 19.35, 11.55, 14.7, 4.5, 14.25, 18.3, 4.65, 19.2, 49.8, 56.85, 33.9, 32.1, 70.8, 62.85, 69, 84.6, 70.5, 80.1, 69.45, 36.6, 49.8, 81.9, 172.35, 168.3, 190.95, 166.8, 181.2, 147.45, 129.15, 125.7, 134.25, 140.55, 127.05, 116.85, 103.05, 116.7, 145.8, 154.5, 151.8, 124.95, 124.5, 108.15, 80.1, 57, 75.9, 102.45, 55.65, 41.4, 78.3, 78.9, 93.15, 125.1, 121.8, 133.65, 146.55, 177, 197.7, 186.75, 166.65, 215.4, 234.75, 222.75, 236.25, 223.35
        };

        // 数据标准化
        StandardScaler scaler = new StandardScaler();
        double[] scaledReturns = scaler.fitTransform(originalReturns);
        System.out.println("标准化后的数据前5个值: " + Arrays.toString(Arrays.copyOfRange(scaledReturns, 0, 5)));
        System.out.println("标准化数据统计: 均值=" + scaler.getMeans()[0] + ", 标准差=" + scaler.getStds()[0]);
        System.out.println("观察序列长度: " + scaledReturns.length);

        // 创建HMM模型，尝试4个状态
        int numStates = 4;
        markovClientJava markovClient = new markovClientJava(numStates);

        // 打印初始参数
        System.out.println("初始参数:");
        System.out.println("初始概率: " + Arrays.toString(markovClient.getInitialProbabilities()));
        System.out.println("转移矩阵:");
        double[][] transMatrix = markovClient.getTransitionMatrix();
        for (int i = 0; i < numStates; i++) {
            System.out.println(Arrays.toString(transMatrix[i]));
        }
        System.out.println("发射分布均值:");
        double[][] means = markovClient.getEmissionMeans();
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + means[i][0]);
        }

        // 训练模型，增加迭代次数到1500
        int maxIterations = 1500;
        System.out.println("开始训练模型...");
        markovClient.train(scaledReturns, maxIterations);
        System.out.println("模型训练完成，迭代次数: " + maxIterations);

        // 打印初始概率
        System.out.println("初始概率: " + Arrays.toString(markovClient.getInitialProbabilities()));

        // 打印转移矩阵
        System.out.println("转移矩阵:");
        double[][] transitionMatrix = markovClient.getTransitionMatrix();
        for (int i = 0; i < numStates; i++) {
            System.out.println(Arrays.toString(transitionMatrix[i]));
        }

        // 打印发射分布均值和方差
        System.out.println("发射分布均值:");
        double[][] emissionMeans = markovClient.getEmissionMeans();
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + emissionMeans[i][0]);
        }

        System.out.println("发射分布方差:");
        double[][] emissionVars = markovClient.getEmissionVars();
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + emissionVars[i][0]);
        }

        // 识别状态
        int[] states = markovClient.identifyStates(scaledReturns);
        System.out.println("识别的状态序列前20个值: " + Arrays.toString(Arrays.copyOfRange(states, 0, 20)));

        // 检查gamma值（需要markovClientJava类实现getGamma方法）
        System.out.println("\n尝试获取gamma矩阵...");
        try {
            double[][] gamma = markovClient.getGamma(scaledReturns);
            System.out.println("Gamma矩阵分析 (前10个时间步):");
            for (int t = 0; t < Math.min(10, gamma.length); t++) {
                System.out.print("时间步 " + t + ": [");
                double maxProb = -Double.MAX_VALUE;
                int mostProbableState = 0;
                for (int i = 0; i < numStates; i++) {
                    System.out.printf("%.6f", gamma[t][i]);
                    if (i < numStates - 1) System.out.print(", ");
                    if (gamma[t][i] > maxProb) {
                        maxProb = gamma[t][i];
                        mostProbableState = i;
                    }
                }
                System.out.println("] 最可能状态: " + mostProbableState);
            }
        } catch (UnsupportedOperationException e) {
            System.out.println("markovClientJava类尚未实现getGamma方法");
        }

        // 检查状态分布
        int[] stateCounts = new int[numStates];
        for (int state : states) {
            stateCounts[state]++;
        }
        System.out.println("状态分布:");
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + stateCounts[i] + " 次");
        }

        // 分析状态持续性
        Map<String, Double> persistence = markovClient.analyzeStatePersistence(states);
        System.out.println("状态持续性分析:");
        persistence.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}