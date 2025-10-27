package com.demo.extract.services;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.model.DecisionResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {
    // 存储增强数据的字典
    private static Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();
    // 存储增强数据长度的字典
    private static Map<String, OrderTimeSeries> enhancedDictLength = new HashMap<>();
    // 存储决策结果的字典
    private static List<DecisionResult> results = new ArrayList<>();

    private static Set<String> targetIdsSet = new HashSet<>();

    private static final String tar = "9999";


    public  void initMapsPublic() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        //List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/镑日分仓收益.csv");
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
     * 解析时间字符串为Date对象，尝试多种常见格式
     * @param timeStr 时间字符串
     * @return 解析后的Date对象，失败返回null
     */
    public  Date parseDateTime(String timeStr) {
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



    /**
     * 更新增强字典
     */
    public  void updateEnhancedDicts(List<OrderTimeSeries> orders) {
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
}
