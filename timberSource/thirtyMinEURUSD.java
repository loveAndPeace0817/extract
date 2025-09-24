package com.demo.extract.timberSource;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.updateOrderDTO;
import com.demo.extract.WriteOrder;
import com.demo.extract.model.DecisionResult;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.zzq.ZZQDataLoader;
import com.demo.extract.zzq.dto.zzqdto;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class thirtyMinEURUSD {

    public static void main(String[] args) throws IOException {
        loadStartData();
    }
    public static List<updateOrderDTO> loadStartData() throws IOException {
        int ycount = 0;
        int ncount = 0;
        double yamount = 0.00;
        double namount = 0.00;
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 加载数据
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/欧美收益分仓69胜率明细.csv");
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
                for (int i = 0; i < valueTime.length; i=i+5) {
                    // 使用优化后的方法判断
                    LocalDateTime indexTime = findIntervalIndex(valueTime[i], timeToIndexMap);
                    Integer index= timeToIndexMap.get(indexTime);
                    // 确保索引有效
                    if (index >= 0 && index < zzqData.size()) {
                        String nextAction = getZZQOptimized(zzqData.get(index).getDate(), action, zzqMap);
                        if (nextAction.equals("1")) {
                            if(i != 0){

                                int timeIndex = findTimeIndex(valueTime, indexTime);
                                if(timeIndex != -1){
                                    orderTimeSeries.setStep(timeIndex);
                                    break;
                                }

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
            double[] close = orderTimeSeries.getClose();
            if (step!= null &&  orderTimeSeries.getClose().length > step) {
                double openPrice = orderTimeSeries.getClose()[step];//获取最佳步数价格
                System.out.println("id="+orderTimeSeries.getOrderId()+"方向：" + orderTimeSeries.getAction()+" 真实价格="+orderTimeSeries.getInPrice()[0]+" 推荐价格=="+openPrice);

                updateOrderDTO dto = new updateOrderDTO();
                dto.setOrderId(Integer.valueOf(orderTimeSeries.getOrderId()));
                dto.setEndPrice(close[close.length-1]);
                dto.setStartPrice(openPrice);
                dto.setAction(orderTimeSeries.getAction());
                list.add(dto);



                if(!StringUtils.isEmpty(orderTimeSeries.getAction()) && "多".equals(orderTimeSeries.getAction())){
                    if(orderTimeSeries.getOpen()[0] > openPrice){
                        ycount++;
                        double abs = Math.abs(orderTimeSeries.getInPrice()[0] - openPrice);
                        yamount += abs;
                    }else {
                        ncount++;
                        double abs = Math.abs(orderTimeSeries.getInPrice()[0] - openPrice);
                        namount += abs;
                    }
                }
                if(!StringUtils.isEmpty(orderTimeSeries.getAction()) && "空".equals(orderTimeSeries.getAction())){
                    if( openPrice > orderTimeSeries.getOpen()[0]){
                        ycount++;
                        double abs = Math.abs(orderTimeSeries.getInPrice()[0] - openPrice);
                        yamount += abs;
                    }else {
                        ncount++;
                        double abs = Math.abs(orderTimeSeries.getInPrice()[0] - openPrice);
                        namount += abs;
                    }
                }



            }
        }


        WriteOrder w  = new WriteOrder();
        w.updateEURUSDOrders(list);

        System.out.println("正确数量"+ycount);
        System.out.println("错误数量"+ncount);
        System.out.println("正确amount"+yamount);
        System.out.println("错误amount"+namount);

        // 记录结束时间
        long endTime = System.currentTimeMillis();
        System.out.println("程序运行时间: " + (endTime - startTime) + "ms");

        return list;
    }



    public static List<zzqdto> getZZQData() throws IOException {
        ZZQDataLoader loaderNew = new ZZQDataLoader();
        return loaderNew.loadFromCsv("D:/data/章铮奇/eurusd_30min.csv");
    }

    /**
     * 预处理zzqData列表，创建从LocalDateTime到索引的映射
     */
    private static Map<LocalDateTime, Integer> preprocessTimeIndexMap(List<zzqdto> zzqData) {
        Map<LocalDateTime, Integer> timeToIndexMap = new LinkedHashMap<>();
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



    public static LocalDateTime findIntervalIndex(String inputTime, Map<LocalDateTime, Integer> timePoints) {
        // 1. 解析输入时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(inputTime, formatter);

        // 检查时间是否在第一个时间点之前
        LocalDateTime firstKey = timePoints.keySet().iterator().next();
        if (dateTime.isBefore(firstKey)) {
            throw new RuntimeException("不在范围时间周期内"+inputTime);
        }

        // 遍历时间点Map，找到合适的区间
        LocalDateTime previousKey = null;
        for (LocalDateTime key : timePoints.keySet()) {
            if (dateTime.isBefore(key)) {
                return previousKey; // 返回前一个key（区间起点）
            }
            previousKey = key;
        }

        // 如果时间在所有时间点之后，返回最后一个key
        return previousKey;
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

    public static void mergeList(List<DecisionResult> results , Map<String, OrderTimeSeries> enhancedDict) throws IOException {
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
        w.updateAllOrders(mergedList);
    }


    public static int findTimeIndex(String[] valueTime, LocalDateTime indexTime) {
        // 定义日期时间格式（必须与valueTime中的格式一致）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

        for (int i = 0; i < valueTime.length; i++) {
            // 将字符串解析为LocalDateTime
            LocalDateTime dateTime = LocalDateTime.parse(valueTime[i], formatter);

            // 比较时间是否相同
            if (dateTime.equals(indexTime)) {
                return i; // 返回匹配的索引
            }
        }

        return -1; // 未找到返回-1
    }
}
