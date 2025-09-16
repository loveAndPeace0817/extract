package com.demo.extract;






import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.updateOrderDTO;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.services.DataLoader;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.SimilarityService;
import com.demo.extract.util.CsvWriter;
import com.demo.extract.zzq.ZZQDataLoader;
import com.demo.extract.zzq.dto.zzqdto;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {

    private static  Map<String, OrderTimeSeries> dtMap ;
    public static void main(String[] args) throws IOException {
        // 1. 加载数据
        DataLoader loader = new DataLoader();
        DataLoaderNew loaderNew = new DataLoaderNew();
        //List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/欧美收益分仓.csv");
        //List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/胡版本阈值0.4明细.csv");
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
        //数据比例
        double testRatio = new Double(0.8);
        Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();//原始长度数据
        Map<String, OrderTimeSeries> enhancedDictLength = new HashMap<>();//截取后的长度
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

        // 2. 初始化服务
        SimilarityService service = new SimilarityService(4);

        dtMap = enhancedDict;

        // 3. 批量测试
        //List<DecisionResult> results = service.batchTestAllOrders(enhancedDict,enhancedDictLength, testRatio, 3000);//7260
        //List<DecisionResult> results = service.batchTestAllOrdersMHT(enhancedDict,enhancedDictLength, testRatio, 3000);//10030
        //List<DecisionResult> results = service.batchTestAllOrdersDTW(enhancedDict,enhancedDictLength, testRatio, 3000);7798
        List<DecisionResult> results = service.batchTestAllOrdersPC(enhancedDict,enhancedDictLength, testRatio, 3000);//9077
        //  皮尔逊 黄金（0.63）8164   切比雪夫距离 磅日 0.51 445   曼哈顿距离  欧美 0.55 1399
        //updateDecisions(results,results1);

        // 4. 打印汇总结果
        //printSummary(results);

        //5.掐头去尾
        Map<String, OrderTimeSeries> odMap = getODMap(allSeries);
        updateAllOrders(results,odMap,enhancedDict);

        service.shutdown();
    }

    private static void printSummary(List<DecisionResult> results) throws IOException {
        long correctCount = results.stream().filter(r -> r.isCorrect()).count();
        long holdCount = results.stream().filter(r -> r.getDecision().equals("hold")).count();
        long closeCount = results.stream().filter(r -> r.getDecision().equals("close")).count();

        List<DecisionResult > ids = new ArrayList<>();
        List<String> currencyPairs = new ArrayList<>();
        List<String> operations = new ArrayList<>();
        List<String > ids1 = new ArrayList<>();
        String filePath = "D:/Decode Global MT4 Terminal/tester/files/trades.csv";

        Map<String,Double> updateData = new HashMap<>();

        double yb = 0.00;
        double yb1 = 0.00;
        double yb2 = 0.00;
        int closeCounts = 0;
        for (DecisionResult decisionResult:results){
            if(decisionResult.getDecision().equals("close")){

                updateData.put(decisionResult.getOrderId(),decisionResult.getTime1Value());//第57个价格



                ids.add(decisionResult);
                currencyPairs.add("EURUSD");
                operations.add("平仓");
                ids1.add(decisionResult.getOrderId());
                yb1 += decisionResult.getTime1Value();
                yb += decisionResult.getTime2Value();
                if(decisionResult.getTime2Value() >decisionResult.getTime1Value() ){
                    yb2 += decisionResult.getTime2Value();
                }else {
                    yb2 += decisionResult.getTime1Value();
                    closeCounts++;
                }
            }

        }

        System.out.println("\n========== 测试汇总 ==========");
        System.out.printf("测试订单数: %d\n", results.size());

        System.out.println("辅助订单总量"+closeCount);
        System.out.println("辅助正确量"+closeCounts);
        System.out.println("辅助正确率:"+(double) closeCounts/closeCount);
        System.out.println("不辅助订单金额"+yb);
        System.out.println("辅助订单金额"+yb1);
        System.out.println("理想目标辅助订单金额"+yb2);
        WriteOrder w  = new WriteOrder();
        w.updateData(updateData);

    }

    public static void updateDecisions(List<DecisionResult> results, List<DecisionResult> results1) {
        // 将results1转换为以orderId为键的Map
        Map<String, String> result1Map = results1.stream()
                .collect(Collectors.toMap(DecisionResult::getOrderId, DecisionResult::getDecision));

        // 遍历results并更新符合条件的元素
        for (DecisionResult result : results) {
            String orderId = result.getOrderId();

            if (result1Map.containsKey(orderId)
                    && "hold".equalsIgnoreCase(result.getDecision())
                    && "close".equalsIgnoreCase(result1Map.get(orderId))) {
                System.out.println("orderid =="+result);
                result.setDecision("close"); // 假设有setDecision方法
            }
        }
    }



    public static void updateAllOrders(List<DecisionResult> results ,Map<String, OrderTimeSeries> orderMap,Map<String, OrderTimeSeries> enhancedDict){

        Map<String,Double> updateData = new HashMap<>();
        List<updateOrderDTO> list = new ArrayList<>();
        List<updateOrderDTO> list1 = new ArrayList<>();
        for (DecisionResult decisionResult:results){
            if(decisionResult.getDecision().equals("close")){
                updateData.put(decisionResult.getOrderId(),decisionResult.getTime1Value());//第57个价格
                OrderTimeSeries orderTimeSeries = enhancedDict.get(decisionResult.getOrderId());
                double close = orderTimeSeries.getClose()[57];
                double open = orderTimeSeries.getOpen()[0];
                String action = detectOrderDirection(orderTimeSeries);

                updateOrderDTO dto = new updateOrderDTO();
                dto.setOrderId(Integer.valueOf(decisionResult.getOrderId()));
                dto.setEndPrice(close);
                dto.setStartPrice(open);
                dto.setAction(action);
                list.add(dto);
            }
        }



        for (String key : orderMap.keySet()) {
            OrderTimeSeries orderTimeSeries = orderMap.get(key);
            Integer step = orderTimeSeries.getStep();
            if (step!= null &&  orderTimeSeries.getClose().length > step) {
                double openPrice = orderTimeSeries.getOpen()[step];//获取最佳步数价格
                String orderId = orderTimeSeries.getOrderId();
                    if(!hasOrder(list,Integer.valueOf(orderId),openPrice)){
                        updateOrderDTO dto1 = new updateOrderDTO();
                        dto1.setOrderId(Integer.valueOf(orderId));
                        dto1.setStartPrice(openPrice);
                        OrderTimeSeries orderTimeSeries1 = enhancedDict.get(orderId);
                        double[] close = orderTimeSeries1.getClose();
                        String action = detectOrderDirection(orderTimeSeries1);
                        dto1.setEndPrice(close[close.length-1]);
                        dto1.setAction(action);
                        list1.add(dto1);
                    }
            }
        }

        // 使用 Stream 合并并去重
        List<updateOrderDTO> mergedList = Stream.concat(list.stream(), list1.stream())
                .collect(Collectors.toMap(
                        updateOrderDTO::getOrderId,
                        dto -> dto,
                        (existing, replacement) -> existing // 如果 orderId 冲突，保留 existing（list 的项）
                ))
                .values()
                .stream()
                .collect(Collectors.toList());

        WriteOrder w  = new WriteOrder();
        w.updateAllOrders(mergedList);
    }


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

        if (valueAt2 > valueAt1) {//盈利了
            // 收益持续升高
            if (closeAt2 > closeAt1) {
                // 收盘价持续上升 → 多单
                return "多";
            } else if (closeAt2 < closeAt1) {
                // 收盘价持续下降 → 空单
                return "空";
            }
        }
        if (valueAt2 < valueAt1) {//亏损了
            // 收益持续升高
            if (closeAt2 > closeAt1) {
                // 收盘价持续上升 → 多单
                return "空";
            } else if (closeAt2 < closeAt1) {
                // 收盘价持续下降 → 空单
                return "多";
            }
        }
        return "未知";
    }




    public static Map<String, OrderTimeSeries>  getODMap(List<OrderTimeSeries> allSeries) throws IOException {

        // 记录开始时间
        long startTime = System.currentTimeMillis();


        List<zzqdto> zzqData = getZZQData();

        // 预处理zzq数据，转换为Map以提高查询效率
        Map<String, zzqdto> zzqMap = new HashMap<>();
        for (zzqdto dto : zzqData) {
            zzqMap.put(dto.getDate(), dto);
        }

        // 预处理：创建从LocalDateTime到索引的映射，提高findIntervalIndex的查找效率
        Map<LocalDateTime, Integer> timeToIndexMap = preprocessTimeIndexMap(zzqData);

        Map<String, OrderTimeSeries> orderMap = new HashMap<>();

        for (OrderTimeSeries orderTimeSeries : allSeries) {
            if (orderTimeSeries.getValues().length >= 70) {
                String action = detectOrderDirection(orderTimeSeries);
                orderTimeSeries.setAction(action);
                String[] valueTime = orderTimeSeries.getValueTime();

                // 遍历时间序列，找到匹配的开仓点
                for (int i = 0; i < valueTime.length; i = i+2) {
                    // 使用优化后的方法判断
                    int index = findIntervalIndex(valueTime[i], timeToIndexMap);

                    // 确保索引有效
                    if (index >= 0 && index < zzqData.size()) {
                        String nextAction = getZZQOptimized(zzqData.get(index).getDate(), action, zzqMap);
                        if (nextAction.equals("1")) {
                            if(i != 0){
                                orderTimeSeries.setStep(i);
                                break;
                            }
                            break;
                        }
                    }
                }

                orderMap.put(orderTimeSeries.getOrderId(), orderTimeSeries);
            }
        }

        return orderMap;
    }


    public static List<zzqdto> getZZQData() throws IOException {
        ZZQDataLoader loaderNew = new ZZQDataLoader();
        return loaderNew.loadFromCsv("D:/data/章铮奇/xauusd_15min.csv");
    }

    /**
     * 预处理zzqData列表，创建从LocalDateTime到索引的映射
     */
    private static Map<LocalDateTime, Integer> preprocessTimeIndexMap(List<zzqdto> zzqData) {
        Map<LocalDateTime, Integer> timeToIndexMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

        for (int i = 0; i < zzqData.size(); i++) {
            zzqdto dto = zzqData.get(i);
            LocalDateTime dateTime = LocalDateTime.parse(dto.getDate(), formatter);
            timeToIndexMap.put(dateTime, i);
        }

        return timeToIndexMap;
    }


    /**
     * 优化版的findIntervalIndex方法，使用预处理的Map进行O(1)时间复杂度的查找
     */
    public static int findIntervalIndex(String inputTime, Map<LocalDateTime, Integer> timeToIndexMap) {
        // 1. 解析输入时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        LocalDateTime inputDateTime = LocalDateTime.parse(inputTime, formatter);

        // 2. 计算所属的15分钟区间
        LocalDateTime intervalStartTime = calculate15MinInterval(inputDateTime);

        // 3. 使用预处理的Map直接查找，时间复杂度O(1)
        return timeToIndexMap.getOrDefault(intervalStartTime, -1);
    }

    /**
     * 计算输入时间所属的15分钟区间的起始时间
     */
    private static LocalDateTime calculate15MinInterval(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int intervalMinute = (minute / 15) * 15; // 取最近的15分钟整点
        return dateTime.withMinute(intervalMinute).withSecond(0).withNano(0);
    }

    /**
     * 优化版的ZZQ判断方法，使用HashMap提高查找效率
     */
    public static String getZZQOptimized(String date, String action, Map<String, zzqdto> zzqMap) {
        // 使用HashMap快速查找date对应的zzqdto
        zzqdto currentDto = zzqMap.get(date);
        if (currentDto == null) {
            return "0";
        }

        if(action.equals("多")){
            if(currentDto.getFluctuation().equals("涨")){
                return "1";
            }else {
                return "2";
            }
        }

        if(action.equals("空")){
            if(currentDto.getFluctuation().equals("跌")){
                return "1";
            }else {
                return "2";
            }
        }

        return "0";
    }

    public static Boolean hasOrder(List<updateOrderDTO> list,Integer orderId,Double openPrice){
        for (updateOrderDTO dto:list){
            if(dto.getOrderId().toString().equals(orderId.toString())){
                dto.setStartPrice(openPrice);
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }


}

