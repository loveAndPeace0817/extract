package com.demo.extract.scheduler;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.SimilarityService;
import com.demo.extract.util.CsvWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
@Service
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
            String csvFilePath = "C:/Users/Administrator/AppData/Roaming/MetaQuotes/Terminal/Common/Files/黄金收益持仓.csv"; // 请替换为实际的CSV文件路径  镑日收益持仓
            //String csvFilePath = "C:/Users/Administrator/AppData/Roaming/MetaQuotes/Terminal/Common/Files/镑日收益持仓.csv"; // 请替换为实际的CSV文件路径  镑日收益持仓


            DataLoaderNew loaderNew = new DataLoaderNew();
            List<OrderTimeSeries> newOrders1 = loaderNew.loadFromCsv(csvFilePath);
            List<OrderTimeSeries> newOrders = getLatestOrderByValueTime(newOrders1);

            if (newOrders.isEmpty()) {
                System.out.println("没有读取到新的订单数据");
                return;
            }
            if(newOrders.size()>1){
                System.out.println("目标订单数量不对");
                return;
            }
            /*if(targetIdsSet.contains(newOrders.get(0).getOrderId()+tar)){
                System.out.println("已经预测过此订单"+newOrders.get(0).getOrderId());
                return;
            }*/
            for (OrderTimeSeries orderTimeSeries :newOrders ){
                if(orderTimeSeries.getValueTime().length < 40){
                    System.out.println("目标订单持仓时间不足");
                    return;
                }
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
     * 执行定时任务
     */
    public  String executeTaskPublic(List<OrderTimeSeries> newOrders) {
        try {
            System.out.println("执行任务: " + new Date());


            if (newOrders.isEmpty()) {
                System.out.println("没有读取到新的订单数据");
                return "-1";
            }
            if(newOrders.size()>1){
                System.out.println("目标订单数量不对");
                return "-1";
            }
            for (OrderTimeSeries orderTimeSeries :newOrders ){
                targetIdsSet.add(orderTimeSeries.getOrderId()+tar);
            }

            // 2. 更新增强字典
            updateEnhancedDicts(newOrders);

            // 3. 运行批量测试
            String result = runBatchTestPublic();
            return result;

        } catch (Exception e) {
            System.err.println("任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
        return "-1";
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
        results = service.batchTestAllOrdersPC(enhancedDict, enhancedDictLength,0.9,3000);
        for(DecisionResult decisionResult:results){
            if(decisionResult.getOrderId().contains(tar)){
                System.out.println(decisionResult.getDecision());
            }
        }
    }


    /**
     * 运行批量测试
     */
    private static String runBatchTestPublic() {
        // 假设BatchTester有一个batchTestAllOrders方法
        // 并假设该方法会将结果存入results字典
        SimilarityService service = new SimilarityService(4);
        results = service.batchTestAllOrdersPC(enhancedDict, enhancedDictLength,0.9,3000);
        for(DecisionResult decisionResult:results){
            if(decisionResult.getOrderId().contains(tar)){
                return decisionResult.getDecision();
            }
        }
        return "-1";
    }

    /**
     * 输出结果到CSV文件
     */
    private static void outputResultsToCsv() throws IOException {
        // 生成结果文件路径
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String outputFilePath = "C:/Users/Administrator/AppData/Roaming/MetaQuotes/Terminal/Common/Files/特征XAUUSD.csv";

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
                currencyPairs.add("XAUUSD.PRO");
                operations.add(decisionResult.getDecision()); // 假设DecisionResult有getDecision方法
                //operations.add("close"); // 假设DecisionResult有getDecision方法
            }

        }


        // 写入CSV文件
        CsvWriter.writeToCsv(outputFilePath, currencyPairs,operations,ids);
        System.out.println("结果已输出到: " + outputFilePath);
    }

    public static void initMaps() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
        //List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/镑日分仓收益.csv");
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

                int endIndex = (int)(values.length * 0.8);     // 计算80%位置
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

    public  void initMapsPublic() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
        //List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/镑日分仓收益.csv");
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

                int endIndex = (int)(values.length * 0.8);     // 计算80%位置
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

    /**
     * 获取List<OrderTimeSeries>中valueTime第一个索引时间最大的对象，并放入新集合
     * @param newOrders 原始订单时间序列集合
     * @return 包含最新OrderTimeSeries对象的集合
     */
    public static List<OrderTimeSeries> getLatestOrderByValueTime(List<OrderTimeSeries> newOrders) {
        List<OrderTimeSeries> result = new ArrayList<>();

        if (newOrders == null || newOrders.isEmpty()) {
            return result;
        }
        int maxIndex = 0; // 假设第一个元素是最大的
        Date maxDate = parseDateTime(newOrders.get(0).getValueTime()[0]);

        for (int i = 1; i < newOrders.size(); i++) {
            String[] valueTime = newOrders.get(i).getValueTime();
            if (valueTime == null || valueTime.length == 0) {
                continue; // 跳过无效数据
            }

            Date currentDate = parseDateTime(valueTime[0]);
            if (currentDate == null) {
                continue; // 跳过无法解析的时间
            }

            if (maxDate == null || currentDate.after(maxDate)) {
                maxDate = currentDate;
                maxIndex = i;
            }
        }
        result.add(newOrders.get(maxIndex));
        return result;
    }


    /**
     * 获取List<OrderTimeSeries>中valueTime第一个索引时间最大的对象，并放入新集合
     * @param newOrders 原始订单时间序列集合
     * @return 包含最新OrderTimeSeries对象的集合
     */
    public  List<OrderTimeSeries> getLatestOrderByValueTimePublic(List<OrderTimeSeries> newOrders) {
        List<OrderTimeSeries> result = new ArrayList<>();

        if (newOrders == null || newOrders.isEmpty()) {
            return result;
        }
        int maxIndex = 0; // 假设第一个元素是最大的
        Date maxDate = parseDateTime(newOrders.get(0).getValueTime()[0]);

        for (int i = 1; i < newOrders.size(); i++) {
            String[] valueTime = newOrders.get(i).getValueTime();
            if (valueTime == null || valueTime.length == 0) {
                continue; // 跳过无效数据
            }

            Date currentDate = parseDateTime(valueTime[0]);
            if (currentDate == null) {
                continue; // 跳过无法解析的时间
            }

            if (maxDate == null || currentDate.after(maxDate)) {
                maxDate = currentDate;
                maxIndex = i;
            }
        }
        result.add(newOrders.get(maxIndex));
        return result;
    }

    /**
     * 解析时间字符串为Date对象，尝试多种常见格式
     * @param timeStr 时间字符串
     * @return 解析后的Date对象，失败返回null
     */
    private static Date parseDateTime(String timeStr) {
        // 尝试多种日期格式
        String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy.MM.dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy.MM.dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd HH:mm"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                // 保留严格模式，但添加日志以调试解析问题
                sdf.setLenient(false);
                Date date = sdf.parse(timeStr);
                return date;
            } catch (Exception e) {
                // 尝试下一种格式
            }
        }
        
        // 如果所有格式都失败，尝试宽松模式作为备选
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(true);
                return sdf.parse(timeStr);
            } catch (Exception e) {
                // 继续尝试下一种格式
            }
        }

        return null;
    }

}