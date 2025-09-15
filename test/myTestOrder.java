package com.demo.extract.test;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.client.markovClientJava;
import com.demo.extract.services.DataLoader;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.ReturnFeatureExtractor;
import com.demo.extract.util.StandardScaler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class myTestOrder {
    public static void main(String[] args) throws IOException {
        int a = 0;
        DataLoaderNew loaderNew = new DataLoaderNew();
        DataLoader loader = new DataLoader();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        //List<OrderTimeSeries> allSeries = loader.loadFromCsv("D:/data/黄金收益2.csv");
        Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();//原始长度数据
        for(OrderTimeSeries orderTimeSeries:allSeries){
            if(orderTimeSeries.getValues().length>=70){
                enhancedDict.put(orderTimeSeries.getOrderId(),orderTimeSeries);
                profitExtract(orderTimeSeries.getValues());
                //markov(orderTimeSeries.getValues());
            }
        }
        for(String key :enhancedDict.keySet()){
            OrderTimeSeries orderTimeSeries = enhancedDict.get(key);
            double[] values = orderTimeSeries.getValues();
            for (int i = 0; i < values.length; i++) {
                if(values[i] > 1.00){
                    a++;
                    break;
                }
            }
        }
        System.out.println("订单数量"+ enhancedDict.size()+"==正向收益订单数量=="+a);

    }


    public static void profitExtract(double[] returns){
            // 创建特征提取器实例
            ReturnFeatureExtractor featureExtractor = new ReturnFeatureExtractor();



            // 提取特征
            double[] trendFeatures = featureExtractor.extractTrendFeatures(returns);//率为正表示整体上行趋势，为负表示整体下行趋势，绝对值大小反映趋势强度
            double[] volatilityFeatures = featureExtractor.extractVolatilityFeatures(returns);
            double[] extremeFeatures = featureExtractor.extractExtremeFeatures(returns);
            double[] nonlinearFeatures = featureExtractor.extractNonlinearFeatures(returns);//- Hurst≈0.5：随机游走（无记忆性）- Hurst>0.5：趋势延续（长期记忆）  - Hurst<0.5：均值回复（反转倾向）
            System.out.println(999);
    }

    public static void markov(double[] returns){
        // 创建马尔可夫客户端实例（3个状态）
        markovClientJava markovClient = new markovClientJava(3);
        StandardScaler scaler = new StandardScaler();
        double[] scaledReturns = scaler.fitTransform(returns);
        // 训练模型
        markovClient.train(scaledReturns, 1000); // 100次迭代

        // 识别状态
        int[] states = markovClient.identifyStates(scaledReturns);
        // 分析持续性
        Map<String, Double> persistence = markovClient.analyzeStatePersistence(states);
        for (String key : persistence.keySet()){
            Double aDouble = persistence.get(key);
            System.out.println("key=="+key+"value=="+aDouble);
        }

        // 验证（假设有实际状态序列）
        //double accuracy = markovClient.validatePersistence(actualStates, predictedStates);
        //System.out.println("状态识别准确率: " + accuracy);
    }
}
