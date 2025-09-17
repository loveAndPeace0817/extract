package com.demo.extract.timberSource;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.updateOrderDTO;
import com.demo.extract.Main;
import com.demo.extract.WriteOrder;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.zzq.ZZQDataLoader;
import com.demo.extract.zzq.dto.zzqdto;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class updateGBPJPY {
    public static void main(String[] args) throws IOException {
        loadStartData();
    }
    public static List<updateOrderDTO> loadStartData() throws IOException {

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 加载数据
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/镑日分仓收益.csv");
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
        List<updateOrderDTO> list = new ArrayList<>();
        // 处理结果
        for (String key : orderMap.keySet()) {
            OrderTimeSeries orderTimeSeries = orderMap.get(key);
            Integer step = orderTimeSeries.getStep();
            if (step!= null &&  orderTimeSeries.getClose().length > step) {
                double openPrice = orderTimeSeries.getClose()[step];//获取最佳步数价格
                System.out.println("id="+orderTimeSeries.getOrderId()+"方向：" + orderTimeSeries.getAction()+" 真实价格="+orderTimeSeries.getInPrice()[0]+" 推荐价格=="+openPrice);

                updateOrderDTO dto = new updateOrderDTO();
                dto.setOrderId(Integer.valueOf(orderTimeSeries.getOrderId()));
                //dto.setEndPrice(close);
                dto.setStartPrice(openPrice);
                dto.setAction(orderTimeSeries.getAction());
                list.add(dto);



            }
        }


       // WriteOrder w  = new WriteOrder();
       // w.updateJBPJPYOrders(list);



        // 记录结束时间
        long endTime = System.currentTimeMillis();
        System.out.println("程序运行时间: " + (endTime - startTime) + "ms");

        return list;
    }



    public static List<zzqdto> getZZQData() throws IOException {
        ZZQDataLoader loaderNew = new ZZQDataLoader();
        return loaderNew.loadFromCsv("D:/data/章铮奇/gbpjpy_15min.csv");
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
     * 判断订单是多单还是空单（简化版，只比较特定索引点的值）
     * @param orderTimeSeries 订单时间序列数据
     * @return "多" 或 "空"，无法判断时返回"未知"
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


    /**
     * 计算输入时间所属的15分钟区间的起始时间
     */
    private static LocalDateTime calculate15MinInterval(LocalDateTime dateTime) {
        int minute = dateTime.getMinute();
        int intervalMinute = (minute / 15) * 15; // 取最近的15分钟整点
        return dateTime.withMinute(intervalMinute).withSecond(0).withNano(0);
    }

    public static void mergeList(List<DecisionResult> results ,Map<String, OrderTimeSeries> enhancedDict) throws IOException {
        Map<String,Double> updateData = new HashMap<>();
        List<updateOrderDTO> list = new ArrayList<>();
        for (DecisionResult decisionResult:results){
            if(decisionResult.getDecision().equals("close")){
                updateData.put(decisionResult.getOrderId(),decisionResult.getTime1Value());//第57个价格
                OrderTimeSeries orderTimeSeries = enhancedDict.get(decisionResult.getOrderId());
                double close = orderTimeSeries.getClose()[57];
                double open = orderTimeSeries.getInPrice()[0];
                String action = detectOrderDirection(orderTimeSeries);

                updateOrderDTO dto = new updateOrderDTO();
                dto.setOrderId(Integer.valueOf(decisionResult.getOrderId()));
                dto.setEndPrice(close);
                dto.setStartPrice(open);
                dto.setAction(action);
                list.add(dto);
            }
        }


        List<updateOrderDTO> list1 = loadStartData();
        Map<Integer,updateOrderDTO> map = new HashMap<>();
        for(updateOrderDTO updateOrderDTO:list1){
            map.put(updateOrderDTO.getOrderId(),updateOrderDTO);
        }


        for (int i = 0; i < list.size(); i++) {
            Integer orderId = list.get(i).getOrderId();
            if(map.containsKey(orderId)){
                updateOrderDTO updateOrderDTO = map.get(orderId);
                list.get(i).setStartPrice(updateOrderDTO.getStartPrice());
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
         w.updateJBPJPYOrders(mergedList);
    }

}
