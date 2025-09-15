package com.demo.extract.services;

import com.demo.extract.DTO.AnalysisResult;
import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OrderFeatures;
import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.OverallEvaluation;
import com.demo.extract.DTO.ProphetResponse;
import com.demo.extract.DTO.SegmentAnalysis;
import com.demo.extract.client.ProphetClient;
import com.demo.extract.client.markovClient;
import com.demo.extract.model.AdvancedMarkovModel;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.model.ThreeMarkovModel;
import com.demo.extract.test.KlineDataTest;
import com.demo.extract.util.StandardScaler;
import com.demo.extract.zzq.ZZQDataLoader;
import com.demo.extract.zzq.dto.zzqdto;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



public class SimilarityService {
    private final ExecutorService executor;
    private final FeatureService featureService;
    private final StandardScaler scaler;
    private final markovClient markovClient;

    // 实际Spring Boot应用中通过依赖注入
    RestTemplate restTemplate = new RestTemplate();
    markovClient client = new markovClient(restTemplate);

    public SimilarityService(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.markovClient =client;
        this.featureService = new FeatureService();
        this.scaler = new StandardScaler();

    }

    /*public List<DecisionResult> batchTestAllOrders(
            Map<String, OrderTimeSeries> enhancedDict,
            double testRatio,
            int limit) {

        List<String> orderIds = new ArrayList<>(enhancedDict.keySet());
        return orderIds.stream()
                .limit(limit)
                .map(orderId -> evaluateOrder(orderId, enhancedDict, orderIds, testRatio))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }*/

    private DecisionResult evaluateOrder(
            String targetOrderId,
            Map<String, OrderTimeSeries> enhancedDict,Map<String, OrderTimeSeries> enhancedDictLength,
            List<String> orderIds,
            double testRatio) throws IOException {

        OrderTimeSeries target = enhancedDictLength.get(targetOrderId);
        if (target == null) {
            //throw new  RuntimeException("空值id=="+targetOrderId);
            return null;
        }
        List<OrderFeatures> features = new ArrayList<>(); // 1. 提取特征(收益)
        List<OrderFeatures> featuresClose = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresOpen = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresAtr = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTH = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTL = new ArrayList<>(); //1-1 提取特征(close)
        for(String id : orderIds){
            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(id);
            OrderFeatures time = featureService.extractFeatures(orderTimeSeries, 1);
            features.add(time);
            OrderFeatures close = featureService.extractFeatures(orderTimeSeries, 2);
            featuresClose.add(close);
            OrderFeatures open = featureService.extractFeatures(orderTimeSeries, 3);
            featuresOpen.add(open);
            OrderFeatures atr = featureService.extractFeatures(orderTimeSeries, 4);
            featuresAtr.add(atr);
            OrderFeatures th = featureService.extractFeatures(orderTimeSeries, 5);
            featuresTH.add(th);
            OrderFeatures tl = featureService.extractFeatures(orderTimeSeries, 6);
            featuresTL.add(tl);
        }


        // 2. 标准化特征（收益）
        double[][] featureMatrix = features.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeatures = scaler.standardize(featureMatrix);

        // 2-1. 标准化特征（收益） 5.添加因子步骤 标准化
        double[][] featureMatrixClose = featuresClose.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesClose = scaler.preprocessClosePrices(featureMatrixClose);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixOpen = featuresOpen.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesOpen = scaler.preprocessClosePrices(featureMatrixOpen);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixAtr = featuresAtr.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesAtr = scaler.preprocessClosePrices(featureMatrixAtr);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTH = featuresTH.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTH = scaler.preprocessClosePrices(featureMatrixTH);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTL = featuresTL.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTL = scaler.preprocessClosePrices(featureMatrixTL);



        // 3. 计算相似度(收益)
        double[] dtwDistances = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,1);
        // 3.1 计算相似度(close)6.添加因子步骤 计算相似度
        double[] dtwDistancesClose = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,2);

        double[] dtwDistancesOpen = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,3);

        double[] dtwDistancesAtr = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,4);

        double[] dtwDistancesTH = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,5);

        double[] dtwDistancesTL = computeDtwDistances(target, enhancedDictLength, orderIds, testRatio,6);



        double[][] cosineSim = new CosineSimilarity().compute(scaledFeatures);
        double[][] cosineSimClose = new CosineSimilarity().compute(scaledFeaturesClose);
        double[][] cosineSimOpen = new CosineSimilarity().compute(scaledFeaturesOpen);
        double[][] cosineSimAtr = new CosineSimilarity().compute(scaledFeaturesAtr);
        double[][] cosineSimTH = new CosineSimilarity().compute(scaledFeaturesTH);
        double[][] cosineSimTL = new CosineSimilarity().compute(scaledFeaturesTL);



        // 4. 查找相似订单
        int targetIdx = orderIds.indexOf(targetOrderId);
        List<SimilarOrder> similarOrders = findSimilarOrders(
                targetIdx,
                orderIds,
                dtwDistances,
                cosineSim[targetIdx],
                11,dtwDistancesClose,
                cosineSimClose[targetIdx],
                dtwDistancesOpen,
                cosineSimOpen[targetIdx],
                dtwDistancesAtr,
                cosineSimAtr[targetIdx],dtwDistancesTH,
                cosineSimTH[targetIdx],dtwDistancesTL,cosineSimTL[targetIdx]
                );

        // 5. 评估决策
        return evaluateDecision(targetOrderId, enhancedDict, similarOrders);
    }

    private DecisionResult evaluateOrderMHT(
            String targetOrderId,
            Map<String, OrderTimeSeries> enhancedDict,Map<String, OrderTimeSeries> enhancedDictLength,
            List<String> orderIds,
            double testRatio) throws IOException {

        OrderTimeSeries target = enhancedDictLength.get(targetOrderId);
        if (target == null) {
            //throw new  RuntimeException("空值id=="+targetOrderId);
            return null;
        }
        List<OrderFeatures> features = new ArrayList<>(); // 1. 提取特征(收益)
        List<OrderFeatures> featuresClose = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresOpen = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresAtr = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTH = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTL = new ArrayList<>(); //1-1 提取特征(close)
        for(String id : orderIds){
            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(id);
            OrderFeatures time = featureService.extractFeatures(orderTimeSeries, 1);
            features.add(time);
            OrderFeatures close = featureService.extractFeatures(orderTimeSeries, 2);
            featuresClose.add(close);
            OrderFeatures open = featureService.extractFeatures(orderTimeSeries, 3);
            featuresOpen.add(open);
            OrderFeatures atr = featureService.extractFeatures(orderTimeSeries, 4);
            featuresAtr.add(atr);
            OrderFeatures th = featureService.extractFeatures(orderTimeSeries, 5);
            featuresTH.add(th);
            OrderFeatures tl = featureService.extractFeatures(orderTimeSeries, 6);
            featuresTL.add(tl);
        }


        // 2. 标准化特征（收益）
        double[][] featureMatrix = features.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeatures = scaler.standardize(featureMatrix);

        // 2-1. 标准化特征（收益） 5.添加因子步骤 标准化
        double[][] featureMatrixClose = featuresClose.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesClose = scaler.preprocessClosePrices(featureMatrixClose);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixOpen = featuresOpen.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesOpen = scaler.preprocessClosePrices(featureMatrixOpen);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixAtr = featuresAtr.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesAtr = scaler.preprocessClosePrices(featureMatrixAtr);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTH = featuresTH.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTH = scaler.preprocessClosePrices(featureMatrixTH);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTL = featuresTL.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTL = scaler.preprocessClosePrices(featureMatrixTL);



        // 3. 计算相似度(收益)
        double[] mhtDistances = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,1);
        // 3.1 计算相似度(close)6.添加因子步骤 计算相似度
        double[] mhtDistancesClose = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,2);

        double[] mhtDistancesOpen = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,3);

        double[] mhtDistancesAtr = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,4);

        double[] mhtDistancesTH = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,5);

        double[] mhtDistancesTL = computeMHTDistances(target, enhancedDictLength, orderIds, testRatio,6);



        double[][] cosineSim = new CosineSimilarity().compute(scaledFeatures);
        double[][] cosineSimClose = new CosineSimilarity().compute(scaledFeaturesClose);
        double[][] cosineSimOpen = new CosineSimilarity().compute(scaledFeaturesOpen);
        double[][] cosineSimAtr = new CosineSimilarity().compute(scaledFeaturesAtr);
        double[][] cosineSimTH = new CosineSimilarity().compute(scaledFeaturesTH);
        double[][] cosineSimTL = new CosineSimilarity().compute(scaledFeaturesTL);



        // 4. 查找相似订单
        int targetIdx = orderIds.indexOf(targetOrderId);
        List<SimilarOrder> similarOrders = findSimilarOrders(
                targetIdx,
                orderIds,
                mhtDistances,
                cosineSim[targetIdx],
                11,mhtDistancesClose,
                cosineSimClose[targetIdx],
                mhtDistancesOpen,
                cosineSimOpen[targetIdx],
                mhtDistancesAtr,
                cosineSimAtr[targetIdx],mhtDistancesTH,
                cosineSimTH[targetIdx],mhtDistancesTL,cosineSimTL[targetIdx]
        );

        // 5. 评估决策
        return evaluateDecision(targetOrderId, enhancedDict, similarOrders);
    }
    private DecisionResult evaluateOrderPC(
            String targetOrderId,
            Map<String, OrderTimeSeries> enhancedDict,Map<String, OrderTimeSeries> enhancedDictLength,
            List<String> orderIds,
            double testRatio) throws IOException {

        OrderTimeSeries target = enhancedDictLength.get(targetOrderId);
        if (target == null) {
            //throw new  RuntimeException("空值id=="+targetOrderId);
            return null;
        }
        List<OrderFeatures> features = new ArrayList<>(); // 1. 提取特征(收益)
        List<OrderFeatures> featuresClose = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresOpen = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresAtr = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTH = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTL = new ArrayList<>(); //1-1 提取特征(close)
        for(String id : orderIds){
            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(id);
            OrderFeatures time = featureService.extractFeatures(orderTimeSeries, 1);
            features.add(time);
            OrderFeatures close = featureService.extractFeatures(orderTimeSeries, 2);
            featuresClose.add(close);
            OrderFeatures open = featureService.extractFeatures(orderTimeSeries, 3);
            featuresOpen.add(open);
            OrderFeatures atr = featureService.extractFeatures(orderTimeSeries, 4);
            featuresAtr.add(atr);
            OrderFeatures th = featureService.extractFeatures(orderTimeSeries, 5);
            featuresTH.add(th);
            OrderFeatures tl = featureService.extractFeatures(orderTimeSeries, 6);
            featuresTL.add(tl);
        }


        // 2. 标准化特征（收益）
        double[][] featureMatrix = features.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeatures = scaler.standardize(featureMatrix);

        // 2-1. 标准化特征（收益） 5.添加因子步骤 标准化
        double[][] featureMatrixClose = featuresClose.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesClose = scaler.preprocessClosePrices(featureMatrixClose);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixOpen = featuresOpen.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesOpen = scaler.preprocessClosePrices(featureMatrixOpen);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixAtr = featuresAtr.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesAtr = scaler.preprocessClosePrices(featureMatrixAtr);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTH = featuresTH.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTH = scaler.preprocessClosePrices(featureMatrixTH);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTL = featuresTL.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTL = scaler.preprocessClosePrices(featureMatrixTL);



        // 3. 计算相似度(收益)
        double[] mhtDistances = computePCDistances(target, enhancedDictLength, orderIds, testRatio,1);
        // 3.1 计算相似度(close)6.添加因子步骤 计算相似度
        double[] mhtDistancesClose = computePCDistances(target, enhancedDictLength, orderIds, testRatio,2);

        double[] mhtDistancesOpen = computePCDistances(target, enhancedDictLength, orderIds, testRatio,3);

        double[] mhtDistancesAtr = computePCDistances(target, enhancedDictLength, orderIds, testRatio,4);

        double[] mhtDistancesTH = computePCDistances(target, enhancedDictLength, orderIds, testRatio,5);

        double[] mhtDistancesTL = computePCDistances(target, enhancedDictLength, orderIds, testRatio,6);



        double[][] cosineSim = new CosineSimilarity().compute(scaledFeatures);
        double[][] cosineSimClose = new CosineSimilarity().compute(scaledFeaturesClose);
        double[][] cosineSimOpen = new CosineSimilarity().compute(scaledFeaturesOpen);
        double[][] cosineSimAtr = new CosineSimilarity().compute(scaledFeaturesAtr);
        double[][] cosineSimTH = new CosineSimilarity().compute(scaledFeaturesTH);
        double[][] cosineSimTL = new CosineSimilarity().compute(scaledFeaturesTL);



        // 4. 查找相似订单
        int targetIdx = orderIds.indexOf(targetOrderId);
        List<SimilarOrder> similarOrders = findSimilarOrders(
                targetIdx,
                orderIds,
                mhtDistances,
                cosineSim[targetIdx],
                11,mhtDistancesClose,
                cosineSimClose[targetIdx],
                mhtDistancesOpen,
                cosineSimOpen[targetIdx],
                mhtDistancesAtr,
                cosineSimAtr[targetIdx],mhtDistancesTH,
                cosineSimTH[targetIdx],mhtDistancesTL,cosineSimTL[targetIdx]
        );

        // 5. 评估决策
        return evaluateDecision(targetOrderId, enhancedDict, similarOrders);
    }
    private DecisionResult evaluateOrderDTW(
            String targetOrderId,
            Map<String, OrderTimeSeries> enhancedDict,Map<String, OrderTimeSeries> enhancedDictLength,
            List<String> orderIds,
            double testRatio) throws IOException {

        OrderTimeSeries target = enhancedDictLength.get(targetOrderId);
        if (target == null) {
            //throw new  RuntimeException("空值id=="+targetOrderId);
            return null;
        }
        List<OrderFeatures> features = new ArrayList<>(); // 1. 提取特征(收益)
        List<OrderFeatures> featuresClose = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresOpen = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresAtr = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTH = new ArrayList<>(); //1-1 提取特征(close)
        List<OrderFeatures> featuresTL = new ArrayList<>(); //1-1 提取特征(close)
        for(String id : orderIds){
            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(id);
            OrderFeatures time = featureService.extractFeatures(orderTimeSeries, 1);
            features.add(time);
            OrderFeatures close = featureService.extractFeatures(orderTimeSeries, 2);
            featuresClose.add(close);
            OrderFeatures open = featureService.extractFeatures(orderTimeSeries, 3);
            featuresOpen.add(open);
            OrderFeatures atr = featureService.extractFeatures(orderTimeSeries, 4);
            featuresAtr.add(atr);
            OrderFeatures th = featureService.extractFeatures(orderTimeSeries, 5);
            featuresTH.add(th);
            OrderFeatures tl = featureService.extractFeatures(orderTimeSeries, 6);
            featuresTL.add(tl);
        }


        // 2. 标准化特征（收益）
        double[][] featureMatrix = features.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeatures = scaler.standardize(featureMatrix);

        // 2-1. 标准化特征（收益） 5.添加因子步骤 标准化
        double[][] featureMatrixClose = featuresClose.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesClose = scaler.preprocessClosePrices(featureMatrixClose);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixOpen = featuresOpen.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesOpen = scaler.preprocessClosePrices(featureMatrixOpen);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixAtr = featuresAtr.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesAtr = scaler.preprocessClosePrices(featureMatrixAtr);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTH = featuresTH.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTH = scaler.preprocessClosePrices(featureMatrixTH);

        // 2-1. 标准化特征（收益）
        double[][] featureMatrixTL = featuresTL.stream()
                .map(this::convertToArray)
                .toArray(double[][]::new);
        double[][] scaledFeaturesTL = scaler.preprocessClosePrices(featureMatrixTL);



        // 3. 计算相似度(收益)
        double[] mhtDistances = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,1);
        // 3.1 计算相似度(close)6.添加因子步骤 计算相似度
        double[] mhtDistancesClose = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,2);

        double[] mhtDistancesOpen = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,3);

        double[] mhtDistancesAtr = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,4);

        double[] mhtDistancesTH = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,5);

        double[] mhtDistancesTL = computeDistancesDTW(target, enhancedDictLength, orderIds, testRatio,6);



        double[][] cosineSim = new CosineSimilarity().compute(scaledFeatures);
        double[][] cosineSimClose = new CosineSimilarity().compute(scaledFeaturesClose);
        double[][] cosineSimOpen = new CosineSimilarity().compute(scaledFeaturesOpen);
        double[][] cosineSimAtr = new CosineSimilarity().compute(scaledFeaturesAtr);
        double[][] cosineSimTH = new CosineSimilarity().compute(scaledFeaturesTH);
        double[][] cosineSimTL = new CosineSimilarity().compute(scaledFeaturesTL);



        // 4. 查找相似订单
        int targetIdx = orderIds.indexOf(targetOrderId);
        List<SimilarOrder> similarOrders = findSimilarOrders(
                targetIdx,
                orderIds,
                mhtDistances,
                cosineSim[targetIdx],
                11,mhtDistancesClose,
                cosineSimClose[targetIdx],
                mhtDistancesOpen,
                cosineSimOpen[targetIdx],
                mhtDistancesAtr,
                cosineSimAtr[targetIdx],mhtDistancesTH,
                cosineSimTH[targetIdx],mhtDistancesTL,cosineSimTL[targetIdx]
        );

        // 5. 评估决策
        return evaluateDecision(targetOrderId, enhancedDict, similarOrders);
    }

    private double[] computeDtwDistances(
            OrderTimeSeries target,
            Map<String, OrderTimeSeries> enhancedDict,
            List<String> orderIds,
            double testRatio,Integer type) {

        double[] distances = new double[orderIds.size()];
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i++) {
            if (orderIds.get(i).equals(target.getOrderId())) {
                continue;
            }

            final int idx = i;
            tasks.add(() -> {
                double s1 = DtwCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type);
                double s2 = ManhattanDistanceCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type)*0.1;
                double s3 = EuclideanDistanceCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type);
                double s4 = PearsonCorrelationCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type);
                distances[idx] = s1;  //  67  52
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("MHD计算中断", e);
        }
        return distances;
    }

    private double[] computeMHTDistances(
            OrderTimeSeries target,
            Map<String, OrderTimeSeries> enhancedDict,
            List<String> orderIds,
            double testRatio,Integer type) {

        double[] distances = new double[orderIds.size()];
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i++) {
            if (orderIds.get(i).equals(target.getOrderId())) {
                continue;
            }

            final int idx = i;
            tasks.add(() -> {

                double s2 = ManhattanDistanceCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type)*0.1;

                distances[idx] = s2;  //  67  52
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DTW计算中断", e);
        }
        System.out.println(distances);
        return distances;
    }

    private double[] computePCDistances(
            OrderTimeSeries target,
            Map<String, OrderTimeSeries> enhancedDict,
            List<String> orderIds,
            double testRatio,Integer type) {

        double[] distances = new double[orderIds.size()];
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i++) {
            if (orderIds.get(i).equals(target.getOrderId())) {
                continue;
            }

            final int idx = i;
            tasks.add(() -> {

                double s2 = PearsonCorrelationCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,type)*0.1;

                distances[idx] = s2;  //  67  52
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DTW计算中断", e);
        }
        System.out.println(distances);
        return distances;
    }
    private double[] computeDistancesDTW(
            OrderTimeSeries target,
            Map<String, OrderTimeSeries> enhancedDict,
            List<String> orderIds,
            double testRatio,Integer type) {

        double[] distances = new double[orderIds.size()];
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i++) {
            if (orderIds.get(i).equals(target.getOrderId())) {
                continue;
            }

            final int idx = i;
            tasks.add(() -> {

                double s2 = ConstrainedDtwCalculator.computePartial(target, enhancedDict.get(orderIds.get(idx)), testRatio,15,type);

                distances[idx] = s2;  //  67  52
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DTW WINDOWS 计算中断", e);
        }
        return distances;
    }


    private List<SimilarOrder> findSimilarOrders(
            int targetIdx,
            List<String> orderIds,
            double[] dtwDistances,
            double[] cosineSim,
            int topN,
            double[] dtwDistancesClose,
            double[] cosineSimClose,
            double[] dtwDistancesOpen,
            double[] cosineSimOpen,
            double[]dtwDistancesAtr,
            double[]cosineSimAtr, double[]dtwDistancesTH,
            double[]cosineSimTH, double[]dtwDistancesTL,
            double[]cosineSimTL) {

        // 计算综合评分  7.添加因子寻找相似订单
        double[] combined1 = new double[dtwDistances.length];
        double[] combined2= new double[dtwDistances.length];
        double[] combined3 = new double[dtwDistances.length];
        double[] combined4 = new double[dtwDistances.length];
        double[] combined5 = new double[dtwDistances.length];
        double[] combined6 = new double[dtwDistances.length];
        double[] combined7 = new double[dtwDistances.length];
        double[] combined8 = new double[dtwDistances.length];
        double[] combined9 = new double[dtwDistances.length];
        double[] combined10 = new double[dtwDistances.length];
        double[] combined11 = new double[dtwDistances.length];
        double[] combined12 = new double[dtwDistances.length];

        for (int i = 0; i < combined1.length; i++) {
            if (i == targetIdx) {
                continue;
            }
            double dtwSim = 1 / (1 + dtwDistances[i]);
            double cosineSimA = 1 / (1 + cosineSim[i]);

            double dtwSimClose = 1 / (1 + dtwDistancesClose[i]);
            double cosineSimB = 1 / (1 + cosineSimClose[i]);

            double dtwSimOpen = 1 / (1 + dtwDistancesOpen[i]);
            double cosineSimC = 1 / (1 + cosineSimOpen[i]);

            double dtwSimAtr = 1 / (1 + dtwDistancesAtr[i]);
            double cosineSimD = 1 / (1 + cosineSimAtr[i]);

            double dtwSimTH = 1 / (1 + dtwDistancesTH[i]);
            double cosineSimE = 1 / (1 + cosineSimTH[i]);

            double dtwSimTL= 1 / (1 + dtwDistancesTL[i]);
            double cosineSimF = 1 / (1 + cosineSimTL[i]);

            combined1[i] = 1.0 * dtwSim;//时间
            combined3[i] = 1.0 * cosineSimA;//

            combined2[i] = 1.0 * dtwSimClose;//收盘价
            combined4[i] = 1.0 * cosineSimB;//

            combined5[i] = 1.0 * dtwSimOpen;//开盘价
            combined6[i] = 1.0 * cosineSimC;//

            combined7[i] = 1.0 * dtwSimAtr;//atr
            combined8[i] = 1.0 * cosineSimD;//

            combined9[i] = 1.0 * dtwSimTH;//唐安琪通道高价格
            combined10[i] = 1.0 * cosineSimE;//

            combined11[i] = 1.0 * dtwSimTL;//唐安琪通道低价格
            combined12[i] = 1.0 * cosineSimF;//
        }



        // 2. 计算Softmax权重
        double[] softmaxWeights1 = calculateSoftmax(combined1);
        double[] softmaxWeights2 = calculateSoftmax(combined2);
        double[] softmaxWeights3 = calculateSoftmax(combined3);
        double[] softmaxWeights4 = calculateSoftmax(combined4);
        double[] softmaxWeights5 = calculateSoftmax(combined5);
        double[] softmaxWeights6 = calculateSoftmax(combined6);
        double[] softmaxWeights7 = calculateSoftmax(combined7);
        double[] softmaxWeights8 = calculateSoftmax(combined8);
        double[] softmaxWeights9 = calculateSoftmax(combined9);
        double[] softmaxWeights10 = calculateSoftmax(combined10);
        double[] softmaxWeights11 = calculateSoftmax(combined11);
        double[] softmaxWeights12 = calculateSoftmax(combined12);
        //double[] rbf = calculateRBFWeights(combined);

        List<SimilarOrder> collect1 = collect(softmaxWeights1, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect2 = collect(softmaxWeights2, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect3 = collect(softmaxWeights3, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect4 = collect(softmaxWeights4, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect5 = collect(softmaxWeights5, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect6 = collect(softmaxWeights6, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect7 = collect(softmaxWeights7, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect8 = collect(softmaxWeights8, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect9 = collect(softmaxWeights9, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect10 = collect(softmaxWeights10, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect11 = collect(softmaxWeights11, targetIdx, topN, orderIds, dtwDistances, cosineSim);
        List<SimilarOrder> collect12 = collect(softmaxWeights12, targetIdx, topN, orderIds, dtwDistances, cosineSim);





        List<SimilarOrder> r1 = new ArrayList<>();
        Map<String, List<SimilarOrder>> andRankDuplicateOrders = findAndRankDuplicateOrders( collect2, collect5, collect6,collect7,collect8,collect9,collect10,collect11);
        for (String key:andRankDuplicateOrders.keySet()){
            List<SimilarOrder> similarOrders = andRankDuplicateOrders.get(key);
            if(r1.size() < 6){
                r1.add(similarOrders.get(0));
            }

        }


       /* if(result.size() < 18){
            List<SimilarOrder> similarOrders = mergeAndSelectTopOrders(collect1, collect2, collect3, collect4,collect5,collect6);
            for (SimilarOrder similarOrder:similarOrders){
                if (!result.contains(similarOrder)){
                    result.add(similarOrder);
                }
                if(result.size() >= 18){
                    continue;
                }
            }
        }*/
        //4维矢量    1.（归一化后）每个矢量元素加一个系数   2.（各个矢量元素dtw距离做归一化） 3.得到新的四维矢量  求模  4.取前十八个  5. 分类
        //光谱 1.0-2 抽了很多时间点 衰减 e的负伽马t
        // 全量考虑（选择样本点  最近的  最远的 适中的）  --暂定



        // 归一化
        //double min = Arrays.stream(combined).min().orElse(0);
       // double max = Arrays.stream(combined).max().orElse(1);
        //double[] normalized = Arrays.stream(combined)
               // .map(s -> (s - min) / (max - min ))
               // .toArray();

        // 获取TopN
        return r1;
    }


    private List<SimilarOrder> WriteSimilarOrders(
            int targetIdx, List<String> orderIds,
            double[] dtwDistances, double[] cosineSim, int topN,double[] dtwDistancesClose, double[] cosineSimClose) {

        // 计算综合评分
        double[] combined = new double[dtwDistances.length];
        for (int i = 0; i < combined.length; i++) {
            if (i == targetIdx) {
                continue;
            }
            double dtwSim = 1 / (1 + dtwDistances[i]);
            combined[i] = 0.6 * dtwSim + 0.4 * dtwDistancesClose[i];
            //combined[i] = 1.0 * dtwSim;//n+1 对 n+1  （n+1）2   i=2  （1500）1.聚类分析 2.5个样本够么？

        }



        // 2. 计算Softmax权重
        double[] softmaxWeights = calculateSoftmax(combined);
        //double[] rbf = calculateRBFWeights(combined);

        // 归一化
        //double min = Arrays.stream(combined).min().orElse(0);
        // double max = Arrays.stream(combined).max().orElse(1);
        //double[] normalized = Arrays.stream(combined)
        // .map(s -> (s - min) / (max - min ))
        // .toArray();

        // 获取TopN
        return IntStream.range(0, softmaxWeights.length)
                .filter(i -> i != targetIdx)
                .boxed()
                .sorted(Comparator.comparingDouble(i -> -softmaxWeights[i]))
                .limit(softmaxWeights.length)
                .map(i -> new SimilarOrder(
                        orderIds.get(i),
                        dtwDistances[i],
                        cosineSim[i],
                        softmaxWeights[i]
                ) )
                .collect(Collectors.toList());
    }

    // Softmax计算工具方法（算样本距离）
    private double[] calculateSoftmax(double[] scores) {
        // 防溢出：减去最大值
        double maxScore = Arrays.stream(scores).max().orElse(1);
        double[] expScores = Arrays.stream(scores)
                .map(s -> Math.exp(s - maxScore))
                .toArray();

        double sumExp = Arrays.stream(expScores).sum();

        // 返回Softmax权重
        return Arrays.stream(expScores)
                .map(exp -> exp / (sumExp + 1e-8)) // 防止除零
                .toArray();
    }

    // 高斯核权重计算
    private double[] calculateRBFWeights(double[] distances) {
        // 自动计算带宽sigma（常用中位数或均值）
        double sigma = Arrays.stream(distances).average().orElse(1.0);

        return Arrays.stream(distances)
                .map(d -> Math.exp(-0.5 * Math.pow(d / sigma, 2)))
                .toArray();
    }





    private DecisionResult evaluateDecision(
            String targetOrderId,
            Map<String, OrderTimeSeries> enhancedDict,
            List<SimilarOrder> similarOrders) throws IOException {

        OrderTimeSeries target = enhancedDict.get(targetOrderId);
        double keyTime1 = 0.2, keyTime2 = 0.25;


        double holdScore = 0, closeScore = 0;
        for (SimilarOrder order : similarOrders) {
            OrderTimeSeries ref = enhancedDict.get(order.orderId);
            double val1 = ref.getValueAtTime(keyTime1);
            double val2 = ref.getValues()[ref.getValues().length - 1];

            if (val2 > val1) {
                holdScore += order.normalizedScore;
            } else {
                closeScore += order.normalizedScore;
            }

        }

        // 归一化权重
        double total = holdScore + closeScore;
        holdScore /= total;
        closeScore /= total;

        // 验证决策
        double t1 = target.getValueAtTime(keyTime1);

        double t2 = target.getValues()[target.getValues().length-1];


        //boolean decision = holdScore >= 0.5;
        String[] split = getResult(target,t1, similarOrders, enhancedDict).split(",");
        boolean decision = split[0].equals("T")? true:false;
        //boolean decision = true;
        //if(decision1==false && decision2 == false){decision = false;}

        boolean isCorrect = decision ? (t2 > t1) : (t2 <= t1);
        DecisionResult decisionResult = new DecisionResult(
                targetOrderId,
                decision ? "hold" : "close",
                isCorrect,
                holdScore,
                closeScore,
                t1,
                t2,
                similarOrders.stream().map(o -> o.orderId).collect(Collectors.toList()), similarOrders.stream().map(o -> o.orderId + "_" + o.normalizedScore).collect(Collectors.toList()), Double.parseDouble(split[1])
        );
        if(target.getTargetOrder()){
            decisionResult.setTargetOrder(true);
        }
        return decisionResult;
    }

    private double[] convertToArray(OrderFeatures feature) {
        return new double[]{
                feature.getMean(), feature.getStd(),
                feature.getMin(), feature.getMax(),
                feature.getMedian(), feature.getQ25(),
                feature.getQ75(), feature.getSkewness(),
                feature.getKurtosis(), feature.getRange(),
                feature.getFirstValue(), feature.getLastValue(),
                feature.getAbsMax(), feature.getAbsMean(),
                feature.getPosCount(), feature.getNegCount(),
                feature.getZeroCount(),
                Optional.ofNullable(feature.getTrendSlope()).orElse(0.0),
                Optional.ofNullable(feature.getTrendIntercept()).orElse(0.0),
                Optional.ofNullable(feature.getTrendRValue()).orElse(0.0)
        };
    }

    @Getter
    private static class SimilarOrder {
        final String orderId;
        final double dtwDistance;
        final double cosineSim;
        final double normalizedScore;

        SimilarOrder(String orderId, double dtwDistance, double cosineSim, double normalizedScore) {
            this.orderId = orderId;
            this.dtwDistance = dtwDistance;
            this.cosineSim = cosineSim;
            this.normalizedScore = normalizedScore;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    public List<DecisionResult> batchTestAllOrders(
            Map<String, OrderTimeSeries> enhancedDict, Map<String, OrderTimeSeries> enhancedDictLength,
            double testRatio,
            int limit) {

        List<String> orderIds = new ArrayList<>(enhancedDictLength.keySet());
        Map<String, OrderTimeSeries> upMap = new HashMap<>();
        Map<String, OrderTimeSeries> downMap = new HashMap<>();
        for(String key:enhancedDictLength.keySet()){


            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(key);
            double[] values = orderTimeSeries.getValues();
                if(values.length>1){
                    if( values[values.length-1] >=0.00){
                        upMap.put(key,orderTimeSeries);
                    }else {
                        downMap.put(key,orderTimeSeries);
                    }

                }



        }

        return orderIds.stream()
                .limit(limit)
                .map(orderId -> {
                    int a = -1;
                    if(upMap.containsKey(orderId)){a=1;}
                    List<String> upIds = new ArrayList<>(upMap.keySet());
                    List<String> downIds = new ArrayList<>(downMap.keySet());
                    DecisionResult result = null;
                    DecisionResult result1 = null;
                    try {
                        result = evaluateOrder(orderId,enhancedDict,a==1?upMap:downMap /*a==1?upMap:downMap*/, a==1?upIds:downIds/*a==1?upIds:downIds*/, testRatio);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (result != null) {
                        // 打印每笔订单明细
                        printOrderEvaluationDetails(orderId, enhancedDict, result, testRatio);
                        result.printDetailedReport();
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    public List<DecisionResult> batchTestAllOrdersMHT(
            Map<String, OrderTimeSeries> enhancedDict, Map<String, OrderTimeSeries> enhancedDictLength,
            double testRatio,
            int limit) {

        List<String> orderIds = new ArrayList<>(enhancedDictLength.keySet());

        Map<String, OrderTimeSeries> upMap = new HashMap<>();
        Map<String, OrderTimeSeries> downMap = new HashMap<>();
        for(String key:enhancedDictLength.keySet()){


            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(key);
            double[] values = orderTimeSeries.getValues();
            if(values.length>1){
                if( values[values.length-1] >=0.00){
                    upMap.put(key,orderTimeSeries);
                }else {
                    downMap.put(key,orderTimeSeries);
                }

            }



        }

        return orderIds.stream()
                .limit(limit)
                .map(orderId -> {
                    int a = -1;
                    if(upMap.containsKey(orderId)){a=1;}
                    List<String> upIds = new ArrayList<>(upMap.keySet());
                    List<String> downIds = new ArrayList<>(downMap.keySet());
                    DecisionResult result = null;
                    DecisionResult result1 = null;
                    try {
                        result = evaluateOrderMHT(orderId,enhancedDict,a==1?upMap:downMap /*a==1?upMap:downMap*/, a==1?upIds:downIds/*a==1?upIds:downIds*/, testRatio);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (result != null) {
                        // 打印每笔订单明细
                        printOrderEvaluationDetails(orderId, enhancedDict, result, testRatio);
                        result.printDetailedReport();
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    public List<DecisionResult> batchTestAllOrdersPC(
            Map<String, OrderTimeSeries> enhancedDict, Map<String, OrderTimeSeries> enhancedDictLength,
            double testRatio,
            int limit) {

        List<String> orderIds = new ArrayList<>(enhancedDictLength.keySet());

        Map<String, OrderTimeSeries> upMap = new HashMap<>();
        Map<String, OrderTimeSeries> downMap = new HashMap<>();
        for(String key:enhancedDictLength.keySet()){


            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(key);
            double[] values = orderTimeSeries.getValues();
            if(values.length>1){
                if( values[values.length-1] >=0.00){
                    upMap.put(key,orderTimeSeries);
                }else {
                    downMap.put(key,orderTimeSeries);
                }

            }



        }

        return orderIds.stream()
                .limit(limit)
                .map(orderId -> {
                    int a = -1;
                    if(upMap.containsKey(orderId)){a=1;}
                    List<String> upIds = new ArrayList<>(upMap.keySet());
                    List<String> downIds = new ArrayList<>(downMap.keySet());
                    DecisionResult result = null;
                    DecisionResult result1 = null;
                    try {
                        result = evaluateOrderPC(orderId,enhancedDict,a==1?upMap:downMap /*a==1?upMap:downMap*/, a==1?upIds:downIds/*a==1?upIds:downIds*/, testRatio);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (result != null) {
                        // 打印每笔订单明细
                        printOrderEvaluationDetails(orderId, enhancedDict, result, testRatio);
                        result.printDetailedReport();
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    public List<DecisionResult> batchTestAllOrdersDTW(
            Map<String, OrderTimeSeries> enhancedDict, Map<String, OrderTimeSeries> enhancedDictLength,
            double testRatio,
            int limit) {

        List<String> orderIds = new ArrayList<>(enhancedDictLength.keySet());

        Map<String, OrderTimeSeries> upMap = new HashMap<>();
        Map<String, OrderTimeSeries> downMap = new HashMap<>();
        for(String key:enhancedDictLength.keySet()){


            OrderTimeSeries orderTimeSeries = enhancedDictLength.get(key);
            double[] values = orderTimeSeries.getValues();
            if(values.length>1){
                if( values[values.length-1] >=0.00){
                    upMap.put(key,orderTimeSeries);
                }else {
                    downMap.put(key,orderTimeSeries);
                }

            }



        }

        return orderIds.stream()
                .limit(limit)
                .map(orderId -> {
                    int a = -1;
                    if(upMap.containsKey(orderId)){a=1;}
                    List<String> upIds = new ArrayList<>(upMap.keySet());
                    List<String> downIds = new ArrayList<>(downMap.keySet());
                    DecisionResult result = null;
                    DecisionResult result1 = null;
                    try {
                        result = evaluateOrderDTW(orderId,enhancedDict,a==1?upMap:downMap /*a==1?upMap:downMap*/, a==1?upIds:downIds/*a==1?upIds:downIds*/, testRatio);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (result != null) {
                        // 打印每笔订单明细
                        printOrderEvaluationDetails(orderId, enhancedDict, result, testRatio);
                        result.printDetailedReport();
                    }
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void printOrderEvaluationDetails(
            String orderId,
            Map<String, OrderTimeSeries> enhancedDict,
            DecisionResult result,
            double testRatio) {

        OrderTimeSeries target = enhancedDict.get(orderId);
        int actualPoints = (int)(target.getValues().length * testRatio);

        //System.out.printf("\n【订单 %s】", orderId);
        //System.out.printf("\n测试数据: 前%.0f%% (实际%d/%d点)",
                //testRatio * 100, actualPoints, target.getValues().length);

        // 打印相似订单的贡献明细
        /*System.out.println("\n相似订单贡献权重:");
        result.getSimilarOrders().forEach(similarOrderId -> {
            OrderTimeSeries similar = enhancedDict.get(similarOrderId);
            double val1 = similar.getValueAtTime(0.2);
            double val2 = similar.getValueAtTime(0.25);




            String trend = val2 > val1 ? "↑" : "↓";
            System.out.printf("  - %s: %.2f → %.2f (趋势%s)%n",
                    similarOrderId, val1, val2, trend);
        });*/

    }

    public String getResult(OrderTimeSeries target,double t1, List<SimilarOrder> similarOrders,Map<String, OrderTimeSeries> enhancedDict) throws IOException {
        double scoreResult = new Double(0.00);
        double sumVal1 = 0.00;
        List<List<Double>> simData = new ArrayList<>();
        List<List<String>> orderTimeValue = new ArrayList<>();
        List<FinancialDataPoint> markovData = new ArrayList<>();
        for (SimilarOrder similarOrder:similarOrders){
            scoreResult += similarOrder.normalizedScore;
        }
        for(SimilarOrder similarOrder:similarOrders){
            OrderTimeSeries ref = enhancedDict.get(similarOrder.orderId);
            double val1 = ref.getValueAtTime(0.2);//取时间值 也可以取其他特征
            double normalizedScore = similarOrder.normalizedScore;
            sumVal1 += val1 * (normalizedScore/ scoreResult);
            List<Double> list = Arrays.stream(ref.getValues())  // 将数组转为DoubleStream
                    .boxed()          // 将double转换为Double
                    .collect(Collectors.toList());
            simData.add(list);
            List<String> orderTime = Arrays.stream(ref.getValueTime())  // 将数组转为DoubleStream
                    // 将double转换为Double
                    .collect(Collectors.toList());
            orderTimeValue.add(orderTime);
        }


        //return extract+","+sumVal1;
        //String s = getmarkovResult(target); //   0.63 5933 黄金    0.5994 9755 胡0.4
        String s = getAdmarkovResult(target);//0.64 6129 黄金    0.63 9469   胡0.4
        //targetMarkov(target);
        //String action = detectOrderDirection(target);
        if(t1  > sumVal1  ){

            if(s.equals("趋势上涨") ){
                return "T"+","+sumVal1;
            }

            return "F"+","+sumVal1;
        }
        //超过类似订单的预期则平仓
        return "T"+","+sumVal1;

    }

    public void targetMarkov(OrderTimeSeries target) throws IOException {
        KlineDataTest klineDataTest = new KlineDataTest();
        String action = klineDataTest.determineOrderType(target);
        klineDataTest.analyzeKlineDataBeforeOrder(target,action);

    }

    // 需引入Apache Commons Math
    public ProphetResponse getPyResult(List<List<Double>> simData,List<Double> targetList) throws JsonProcessingException {
        ProphetClient prophetClient = new ProphetClient();
        ProphetResponse responseObj = prophetClient.sendData(simData,targetList);
        return responseObj;
    }

    public List<String> getMarkovResult(List<List<Double>> simData,List<List<String>> orderTimeValue){
        List<List<FinancialDataPoint>> finMarkovData = new ArrayList<>();
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i <simData.size(); i++) {
            List<Double> doubles = simData.get(i);
            List<String> list = orderTimeValue.get(i);
            List<FinancialDataPoint> marketData = new ArrayList<>();
            for (int j = 0; j < doubles.size(); j++) {
                FinancialDataPoint point = new FinancialDataPoint();
                if(!StringUtils.isEmpty(list.get(j))){
                    point.setTimestamp(list.get(j));
                    point.setValue( doubles.get(j));
                    marketData.add(point);
                }

            }
            finMarkovData.add(marketData);
        }
        // 2. 调用分析服务（带重试）
        for (int i = 0; i < finMarkovData.size(); i++) {
            List<FinancialDataPoint> marketData = finMarkovData.get(i);
            AnalysisResult result = markovClient.analyzeWithRetry(marketData, 3);
            // 3. 处理结果
            if ("success".equals(result.getStatus())) {
                //System.out.println("=== 市场阶段分析 ==="+i);
                //阶段结果
                List<SegmentAnalysis> segmentAnalysis = result.getSegmentAnalysis();
                //整体结果
                OverallEvaluation overall = result.getOverallEvaluation();
                resultList.add(overall.getOverallTrend());
                /*result.getSegmentAnalysis().forEach(segment -> {
                    System.out.printf(
                            "阶段%d: %s - %s | 类型: %s | 强度: %.2f | Hurst: %.3f%n",
                            segment.getPhase(),
                            segment.getTimeRange().getStart(),
                            segment.getTimeRange().getEnd(),
                            segment.getTrendType(),
                            segment.getStrength(),
                            segment.getHurst()
                    );
                });*/

                //System.out.println("\n=== 整体评估 ==="+i);

                /*System.out.printf(
                        "趋势: %s | 累计收益: %.1f | 波动率: %.2f | 持续性: %s%n",
                        overall.getOverallTrend(),
                        overall.getTotalReturn(),
                        overall.getVolatility(),
                        overall.getPersistence()
                );*/
            } else {
                System.err.println("分析失败: " + result.getMessage());
            }
        }

        return  resultList;
    }





    public List<SimilarOrder> collect(double[] softmaxWeights1,int targetIdx,int topN,List<String> orderIds,
                                      double[] dtwDistances,double[] cosineSim){
        return IntStream.range(0, softmaxWeights1.length)
                .filter(i -> i != targetIdx)
                .boxed()
                .sorted(Comparator.comparingDouble(i -> -softmaxWeights1[i]))
                .limit(topN)
                .map(i -> new SimilarOrder(
                        orderIds.get(i),
                        dtwDistances[i],
                        cosineSim[i],
                        softmaxWeights1[i]
                ))
                .collect(Collectors.toList());
    }

    public static List<SimilarOrder> findCommonOrderIds(List<List<SimilarOrder>> orderLists) {
        return orderLists.stream()
                .map(list -> list.stream().map(SimilarOrder::getOrderId).collect(Collectors.toList()))
                .reduce((set1, set2) -> {
                    set1.retainAll(set2);
                    return set1;
                })
                .orElse(Collections.EMPTY_LIST);
    }

    public List<SimilarOrder> mergeAndSelectTopOrders(
            List<SimilarOrder> collect1,
            List<SimilarOrder> collect2,
            List<SimilarOrder> collect3,
            List<SimilarOrder> collect4,
            List<SimilarOrder> collect5,
            List<SimilarOrder> collect6) {

        // 1. 合并所有列表
        List<SimilarOrder> allOrders = new ArrayList<>();
        allOrders.addAll(collect1);
        allOrders.addAll(collect2);
        allOrders.addAll(collect3);
        allOrders.addAll(collect4);
        allOrders.addAll(collect5);
        allOrders.addAll(collect6);

        // 2. 按orderId分组，每组保留normalizedScore最高的订单
        Map<String, SimilarOrder> mergedOrders = allOrders.stream()
                .collect(Collectors.toMap(
                        SimilarOrder::getOrderId,
                        order -> order,
                        (order1, order2) ->
                                order1.getNormalizedScore() >= order2.getNormalizedScore() ? order1 : order2
                ));

        // 3. 转换为List并按分数降序排序
        List<SimilarOrder> result = new ArrayList<>(mergedOrders.values());
        result.sort(Comparator.comparingDouble(SimilarOrder::getNormalizedScore).reversed());

        // 5. 最终截取前minSize个
        return result;
    }


    public static Map<String, List<SimilarOrder>> findAndRankDuplicateOrders(List<SimilarOrder>... lists) {
        // 合并所有 List
        List<SimilarOrder> allOrders = new ArrayList<>();
        for (List<SimilarOrder> list : lists) {
            allOrders.addAll(list);
        }

        // 按 orderId 分组
        Map<String, List<SimilarOrder>> orderIdMap = allOrders.stream()
                .collect(Collectors.groupingBy(SimilarOrder::getOrderId));

        // 过滤出重复的 orderId（出现次数 ≥ 2）
        Map<String, List<SimilarOrder>> duplicateOrders = orderIdMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 按出现次数降序排序
        Map<String, List<SimilarOrder>> sortedByCount = duplicateOrders.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size())) // 降序
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new  // 保持排序顺序
                ));

        return sortedByCount;
    }

    public String getExtract(double[] returns){
        // 创建特征提取器实例
        ReturnFeatureExtractor featureExtractor = new ReturnFeatureExtractor();
        double[] trendFeatures = featureExtractor.extractTrendFeatures(returns);//率为正表示整体上行趋势，为负表示整体下行趋势，绝对值大小反映趋势强度
        if(trendFeatures[0] < 0.6 && trendFeatures[0] > -0.45){
            return "S";
        }
        if (trendFeatures[0] > 0.6){
            return "T";
        }else {
            return "F";
        }
    }

    public String getmarkovResult(OrderTimeSeries target){
        String result = "";
        int endIndex = (int)(target.getValues().length * 0.8);     // 计算80%位置
        double[] values = Arrays.copyOfRange(target.getValues(), 0, endIndex);
        String[] valueTime = Arrays.copyOfRange(target.getValueTime(), 0, endIndex);
        String orderId = target.getOrderId();

        ThreeMarkovModel model = new ThreeMarkovModel(values,valueTime,orderId);


        // 执行分段
        model.autoSegment();

        // 分析分段
        List<ThreeMarkovModel.SegmentReport> segmentReports = model.analyzeSegments();

        // 打印分段报告
       /* System.out.println("=== 三段式趋势分析 ===");
        System.out.printf("%-15s %-10s %-10s %-12s %-12s %-12s %-10s %-10s%n",
                "时间范围", "持续时间(min)", "趋势类型", "平均收益(bps)", "最大回撤(bps)",
                "波动率", "Hurst指数", "趋势强度");*/
        for (ThreeMarkovModel.SegmentReport report : segmentReports) {
            result = report.getTrendType();//趋势类型
            /*System.out.printf("%-15s %-10d %-10s %-12.1f %-12.1f %-12.1f %-10.2f %-10s%n",
                    report.getTimeRange(),//时间范围
                    report.getDuration(),//持续时间
                    result = report.getTrendType(),//趋势类型
                    report.getMeanReturn(),//平均收益
                    report.getMaxDrawdown(),//最大回撤
                    report.getVolatility(),//波动率
                    report.getHurst(),//Hurst指数
                    report.getTrendStrength());//趋势强度*/
        }

        // 整体评估
        ThreeMarkovModel.OverallEvaluation overall = model.evaluateOverall();

        // 打印整体评估
       /* System.out.println("\n=== 整体趋势评估 ===");
        System.out.println("整体趋势: " + overall.getOverallTrend());
        System.out.println("累计收益(bps): " + overall.getCumulativeReturn());
        System.out.println("整体Hurst指数: " + overall.getHurst());
        System.out.println("趋势持续性: " + overall.getPersistence());*/
        //String overallTrend = slope > 0.15 ? "趋势上涨" : (slope < -0.15 ? "趋势下跌" : "震荡行情"); // 调整整体趋势判断阈值，基于profit数据分析
        return result;
    }

    public String getAdmarkovResult(OrderTimeSeries target){
        String result = "";
        System.out.println("=== 高级马尔可夫模型测试开始 ===");
        AdvancedMarkovModel model = null;
        if (target.getValues().length >= 70) {
            int endIndex = (int)(target.getValues().length * 0.8); // 计算90%位置
            double[] values = Arrays.copyOfRange(target.getValues(), 0, endIndex);
            String[] valueTime = Arrays.copyOfRange(target.getValueTime(), 0, endIndex);
            String orderId = target.getOrderId();

            System.out.println("加载数据成功：订单ID=" + orderId + ", 数据点数=" + values.length);
            model = new AdvancedMarkovModel(values, valueTime, orderId);
        }




        if (model == null) {
            System.out.println("无法加载测试数据，测试终止。");
            return "";
        }

        // 执行自动分段
        model.autoSegment();

        // 分析分段
        List<AdvancedMarkovModel.SegmentReport> segmentReports = model.analyzeSegments();

        for (AdvancedMarkovModel.SegmentReport report : segmentReports) {
            result = report.getTrendType();
        }

        // 打印分段报告
        //printSegmentReports(segmentReports);

        // 整体评估
        OverallEvaluation overall = model.evaluateOverall();

        // 打印整体评估
        //printOverallEvaluation(overall);

        // 测试getMarkovResult方法
       // testMarkovResult(model);

        System.out.println("=== 高级马尔可夫模型测试结束 ===");

        return result;
    }

    public Boolean getZZQ(String date,String action) throws IOException {
        ZZQDataLoader loaderNew = new ZZQDataLoader();
        List<zzqdto> zzqdtos = loaderNew.loadFromCsv("D:/data/章铮奇/gbpjpy_15min.csv");
        for (int i = 0; i < zzqdtos.size(); i++) {
            if(date.equals(zzqdtos.get(i).getDate())){
                String fluctuation = "";
                Double value = zzqdtos.get(i).getTrueValue();
                Double value1 = zzqdtos.get(i+11).getTrueValue();

                if(value > value1){fluctuation = "涨";}else {fluctuation = "跌";}

                if(action.equals("多") && fluctuation.equals("涨")){
                    return Boolean.TRUE;
                }
                if(action.equals("空") && fluctuation.equals("跌")){
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }


    /**
     * 判断订单是多单还是空单（简化版，只比较特定索引点的值）
     * @param orderTimeSeries 订单时间序列数据
     * @return "多单" 或 "空单"，无法判断时返回"未知"
     */
    public static String detectOrderDirection(OrderTimeSeries orderTimeSeries) {
        if (orderTimeSeries == null) {
            throw new IllegalArgumentException("订单时间序列数据不能为空");
        }

        double[] values = orderTimeSeries.getValues();
        double[] close = orderTimeSeries.getClose();

        // 确保数组不为空且长度足够（至少有索引10的元素）
        if (values == null || close == null || values.length < 11 || close.length < 11) {
            return "未知";
        }

        // 比较索引2和10处的收盘价和收益值
        int index1 = 2;
        int index2 = 10;

        double closeAt1 = close[index1];
        double closeAt2 = close[index2];
        double valueAt1 = values[index1];
        double valueAt2 = values[index2];

        // 判断逻辑：
        // 1. 收益持续升高(valueAt2 > valueAt1)且收盘价持续上升(closeAt2 > closeAt1) → 多单
        // 2. 收益持续升高(valueAt2 > valueAt1)且收盘价持续下降(closeAt2 < closeAt1) → 空单

        if (valueAt2 > valueAt1) {
            // 收益持续升高
            if (closeAt2 > closeAt1) {
                // 收盘价持续上升 → 多单
                return "多";
            } else if (closeAt2 < closeAt1) {
                // 收盘价持续下降 → 空单
                return "空";
            }
        }

        return "未知";
    }
}