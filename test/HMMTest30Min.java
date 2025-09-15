package com.demo.extract.test;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.client.markovClient30Min;
import com.demo.extract.services.DataLoader;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.util.StandardScaler;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HMMTest30Min {
    public static void main(String[] args) throws IOException {
        // 1. 加载数据（假设是30分钟收益数据）

        double[] returns = new double[18];
        double[] returns1 = new double[18];
        DataLoader loader = new DataLoader();
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        //List<OrderTimeSeries> allSeries = loader.loadFromCsv("D:/data/黄金收益2.csv");
        Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();//原始长度数据
        for(OrderTimeSeries orderTimeSeries:allSeries){
            if(orderTimeSeries.getValues().length>=70){
                enhancedDict.put(orderTimeSeries.getOrderId(),orderTimeSeries);
                double[] values = orderTimeSeries.getValues();


                int newLength = (values.length + 11) / 12; // 向上取整
                double[] result = new double[newLength];
                // 填充新数组
                int resultIndex = 0;
                for (int i = 0; i < values.length; i++) {
                    if (i % 12 == 0) { // 每6个元素取一个
                        result[resultIndex++] = values[i];
                    }
                }
                int endIndex = (int)(result.length * 0.9);     // 计算80%位置
                double[] doubles = Arrays.copyOfRange(result, 0, endIndex);
                String s = Arrays.toString(result);

                minData(doubles,s);
            }
        }

    }

    public static  void minData(double[] returns,String s){

        System.out.println("加载的30分钟收益源数据: " + Arrays.toString(returns));

        // 2. 数据预处理 - 标准化
        StandardScaler scaler = new StandardScaler();
        double[] scaledReturns = scaler.fitTransform(returns);

        // 打印标准化后的前5个数据，均值和标准差
        //System.out.println("标准化后前5个数据: ");
        //for (int i = 0; i < Math.min(5, scaledReturns.length); i++) {
            //System.out.println(scaledReturns[i]);
       // }
        //System.out.println("均值: " + scaler.getMeans()[0]);
        //System.out.println("标准差: " + scaler.getStds()[0]);

        // 3. 设置HMM参数并训练模型（30分钟数据可能需要调整状态数）
        int numStates = 3; // 可以根据30分钟数据特征调整状态数
        int maxIterations = 2000; // 迭代次数

        markovClient30Min hmm = new markovClient30Min(numStates);
        System.out.println("开始训练30分钟HMM模型...");
        //long startTime = System.currentTimeMillis();
        hmm.train(scaledReturns, maxIterations);
        //long endTime = System.currentTimeMillis();
        //System.out.println("模型训练完成，耗时: " + (endTime - startTime) + " 毫秒");

        // 4. 识别状态
        int[] states = hmm.identifyStates(scaledReturns);
        System.out.println("带答案的标准数据"+s);
        System.out.println("状态识别数据: " + Arrays.toString(states));

        // 5. 分析状态持续性
        Map<String, Double> persistenceResult = hmm.analyzeStatePersistence(states);
        System.out.println("状态持续性分析结果:");
        persistenceResult.forEach((key, value) -> System.out.println(key + ": " + value));

        // 6. 输出模型参数，便于分析
        System.out.println("\n训练后的模型参数:");
        System.out.println("初始概率: " + Arrays.toString(hmm.getInitialProbabilities()));
        System.out.println("转移矩阵:");
        double[][] transitionMatrix = hmm.getTransitionMatrix();
        for (int i = 0; i < numStates; i++) {
            System.out.println(Arrays.toString(transitionMatrix[i]));
        }
        System.out.println("发射分布均值:");
        double[][] emissionMeans = hmm.getEmissionMeans();
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + emissionMeans[i][0]);
        }
        System.out.println("发射分布方差:");
        double[][] emissionVars = hmm.getEmissionVars();
        for (int i = 0; i < numStates; i++) {
            System.out.println("状态 " + i + ": " + emissionVars[i][0]);
        }

        // 7. 计算Gamma矩阵，分析每个时间步的状态概率
        double[][] gamma = hmm.getGamma(scaledReturns);
        System.out.println("\nGamma矩阵前5个时间步:");
        for (int t = 0; t < Math.min(5, gamma.length); t++) {
            System.out.println("时间步 " + t + ": " + Arrays.toString(gamma[t]));
        }
    }
}