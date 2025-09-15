package com.demo.extract.scheduler;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.SimilarityService;
import com.demo.extract.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTask {
    // 存储增强数据的字典
    private static Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();
    // 存储增强数据长度的字典
    private static Map<String, OrderTimeSeries> enhancedDictLength = new HashMap<>();
    // 存储决策结果的字典
    private static List<DecisionResult> results = new ArrayList<>();

    private static Set<String> targetIdsSet = new HashSet<>();

    private static final String tar = "9999";

    public static void main(String[] args) throws IOException {
        //初始化数据
        initMaps();
        // 创建定时任务调度器
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 每5分钟执行一次任务
        scheduler.scheduleAtFixedRate(
                ScheduledTask::executeTask,
                0,  // 初始延迟0秒
                5,  // 间隔5分钟
                TimeUnit.MINUTES
        );
    }

    /**
     * 执行定时任务
     */
    private static void executeTask() {
        try {
            System.out.println("执行定时任务: " + new Date());

            // 1. 读取CSV文件
            String csvFilePath = "D:/MT4Default/MQL4/Files/data.csv"; // 请替换为实际的CSV文件路径

            DataLoaderNew loaderNew = new DataLoaderNew();
            List<OrderTimeSeries> newOrders = loaderNew.loadFromCsv(csvFilePath);

            if (newOrders.isEmpty()) {
                System.out.println("没有读取到新的订单数据");
                return;
            }
            if(newOrders.size()>1){
                System.out.println("目标订单数量不对");
                return;
            }
            if(targetIdsSet.contains(newOrders.get(0).getOrderId()+tar)){
                System.out.println("已经预测过此订单"+newOrders.get(0).getOrderId());
                return;
            }
            for (OrderTimeSeries orderTimeSeries :newOrders ){
                targetIdsSet.add(orderTimeSeries.getOrderId()+tar);
            }

            // 2. 更新增强字典
            updateEnhancedDicts(newOrders);

            // 3. 运行批量测试
            runBatchTest();

            // 4. 输出结果到CSV
            outputResultsToCsv();

        } catch (Exception e) {
            System.err.println("定时任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 读取CSV文件内容
     */
    private static List<OrderTimeSeries> readCsvFile(String filePath) throws IOException {
        List<OrderTimeSeries> orders = new ArrayList<>();

        // 检查文件是否存在
        if (!Files.exists(Paths.get(filePath))) {
            System.err.println("CSV文件不存在: " + filePath);
            return orders;
        }

        // 使用DataLoader读取CSV文件（假设DataLoader有类似的方法）
        // 这里假设DataLoader有一个readOrderTimeSeries方法
        DataLoaderNew loaderNew = new DataLoaderNew();
        orders = loaderNew.loadFromCsv("D:/data/测试777.csv");

        // 数据长度校验
        List<OrderTimeSeries> validOrders = new ArrayList<>();
        for (OrderTimeSeries order : orders) {
            if (order.getValues() != null && order.getValues().length >= 40) {
                validOrders.add(order);
            } else {
                System.out.println("订单 " + order.getOrderId() + " 数据长度不足40，已忽略");
            }
        }

        return validOrders;
    }

    /**
     * 更新增强字典
     */
    private static void updateEnhancedDicts(List<OrderTimeSeries> orders) {
        for (OrderTimeSeries order : orders) {
            String orderId = order.getOrderId()+tar;

            order.setTargetOrder(true);
            order.setOrderId(orderId);

            enhancedDict.put(orderId, order);
            enhancedDictLength.put(orderId, order);
            System.out.println("更新订单数据: " + orderId + ", 数据长度: " + order.getValues().length);
        }
    }

    /**
     * 运行批量测试
     */
    private static void runBatchTest() {
        // 假设BatchTester有一个batchTestAllOrders方法
        // 并假设该方法会将结果存入results字典
        SimilarityService service = new SimilarityService(4);
        results = service.batchTestAllOrdersMHT(enhancedDict, enhancedDictLength,0.9,3000);
        System.out.println("批量测试完成，共处理 " + results.size() + " 个订单");
    }

    /**
     * 输出结果到CSV文件
     */
    private static void outputResultsToCsv() throws IOException {
        // 生成结果文件路径
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String outputFilePath = "results/EURUSD_" + timestamp + ".csv";

        // 确保结果目录存在
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }

        // 准备CSV数据

        List<String> currencyPairs= new ArrayList<>();
        List<String> operations= new ArrayList<>();
        List<String> ids= new ArrayList<>();
        // 添加数据行
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (DecisionResult decisionResult : results) {
            if(decisionResult.getTargetOrder()){
                ids.add(decisionResult.getOrderId().replace(tar,""));
                currencyPairs.add("EURUSD");
                operations.add(decisionResult.getDecision()); // 假设DecisionResult有getDecision方法

            }

        }


        // 写入CSV文件
        CsvWriter.writeToCsv(outputFilePath, currencyPairs,operations,ids);
        System.out.println("结果已输出到: " + outputFilePath);
    }

    public static void initMaps() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        for(OrderTimeSeries orderTimeSeries:allSeries){
            if(orderTimeSeries.getValues().length>=70){
                enhancedDict.put(orderTimeSeries.getOrderId(),orderTimeSeries);

                OrderTimeSeries lengthOrder = new OrderTimeSeries();
                double[] values = orderTimeSeries.getValues();
                double[] timestamps = orderTimeSeries.getTimestamps();

                double[] close = orderTimeSeries.getClose();//2.添加因子步骤  new 属性
                double[] open = orderTimeSeries.getOpen();
                double[] atr = orderTimeSeries.getAtr();
                double[] th = orderTimeSeries.getTH();
                double[] tl = orderTimeSeries.getTL();
                String[] valueTime = orderTimeSeries.getValueTime();

                int endIndex = (int)(values.length * 0.9);     // 计算80%位置
                lengthOrder.setValues( Arrays.copyOfRange(values, 0, endIndex));
                lengthOrder.setTimestamps(Arrays.copyOfRange(timestamps, 0, endIndex));
                lengthOrder.setOrderId(orderTimeSeries.getOrderId());

                lengthOrder.setClose(Arrays.copyOfRange(close, 0, endIndex));//3.添加因子步骤 属性注入
                lengthOrder.setOpen(Arrays.copyOfRange(open, 0, endIndex));
                lengthOrder.setAtr(Arrays.copyOfRange(atr, 0, endIndex));
                lengthOrder.setTH(Arrays.copyOfRange(th, 0, endIndex));
                lengthOrder.setTL(Arrays.copyOfRange(tl, 0, endIndex));
                lengthOrder.setValueTime(Arrays.copyOfRange(valueTime, 0, endIndex));
                enhancedDictLength.put(orderTimeSeries.getOrderId(),lengthOrder);
            }

        }
    }
}