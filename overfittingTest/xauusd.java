package com.demo.extract.overfittingTest;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.SimilarityService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class xauusd {
    public static void main(String[] args) throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
        // 分片操作
        int splitIndex = allSeries.size() / 2;
        List<OrderTimeSeries> list1 = allSeries.subList(0, splitIndex);
        List<OrderTimeSeries> list2 = allSeries.subList(splitIndex, allSeries.size());
        Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();//原始长度数据
        for(OrderTimeSeries orderTimeSeries:list1){
            if(orderTimeSeries.getValues().length>=70){
                enhancedDict.put(orderTimeSeries.getOrderId(),orderTimeSeries);
            }
        }

        // 2. 初始化服务
        SimilarityService service = new SimilarityService(4);


    }
}
