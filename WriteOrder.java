package com.demo.extract;

import com.demo.extract.DTO.TradeRecord;
import com.demo.extract.DTO.updateOrderDTO;
import com.demo.extract.services.updateProfitForOrder;
import com.demo.extract.util.CsvWriter;

import javax.print.DocFlavor;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteOrder {

    public static void main(String[] args) {
        String filePath = "D:/pyfile/results/辅助前订单黄金.xlsx"; // Excel文件路径

        try {
            // 1. 从Excel文件读取交易记录
            List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);

            // 2. 修改订单号为2的获利金额为50.00
            updateProfitForOrder.updateProfitForOrder(records, 2, 50.00);

            // 3. 将修改后的记录写回Excel文件
            CsvWriter.writeRecordsToExcel(records, filePath);

            System.out.println("交易记录已成功更新并保存到Excel文件: " + filePath);

        } catch (  IOException e) {
            System.err.println("处理Excel文件时出错: " + e.getMessage());
        }

    }


    public void updateData( Map<String,Double> updateData){
        String filePath = "D:/data/高胜率/镑日最大胜率版 - 副本.xlsx"; // Excel文件路径

        try {
            // 1. 从Excel文件读取交易记录
            List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);

            // 2. 修改订单号为2的获利金额为50.00
            for (String key :updateData.keySet()){

                updateProfitForOrder.updateProfitForOrder(records, Integer.valueOf(key), updateData.get(key));

                // 3. 将修改后的记录写回Excel文件
                CsvWriter.writeRecordsToExcel(records, filePath);

                System.out.println("交易记录已成功更新并保存到Excel文件: " + filePath);
            }


        } catch (  IOException e) {
            System.err.println("处理Excel文件时出错: " + e.getMessage());
        }
    }


    public void updateAllOrders( List<updateOrderDTO> mergedList){

        String filePath = "D:/data/高胜率/黄金最大胜率版 - 副本.xlsx"; // Excel文件路径

        try {
            // 1. 从Excel文件读取交易记录
            List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);
            Map<Integer,TradeRecord> map = new HashMap<>();
            for (TradeRecord tradeRecord: records){
                map.put(tradeRecord.getOrderId(),tradeRecord);
            }

            for (updateOrderDTO updateOrderDTO:mergedList){
                if(map.get(updateOrderDTO.getOrderId()) != null){
                    double lots = map.get(updateOrderDTO.getOrderId()).getLots();


                    Double xauusdProfit = getXAUUSDProfit(updateOrderDTO.getStartPrice(), updateOrderDTO.getEndPrice(), updateOrderDTO.getAction(), lots);



                    updateProfitForOrder.updateProfitForOrder(records, updateOrderDTO.getOrderId(), xauusdProfit);
                    // 3. 将修改后的记录写回Excel文件
                    CsvWriter.writeRecordsToExcel(records, filePath);

                    System.out.println("交易记录已成功更新并保存到Excel文件: " + filePath);
                }


            }


        } catch (  IOException e) {
            System.err.println("处理Excel文件时出错: " + e.getMessage());
        }

    }


    public void updateEURUSDOrders( List<updateOrderDTO> mergedList){

        String filePath = "D:/data/高胜率/欧美69胜率 - 副本.xlsx"; // Excel文件路径

        try {
            // 1. 从Excel文件读取交易记录
            List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);
            Map<Integer,TradeRecord> map = new HashMap<>();
            for (TradeRecord tradeRecord: records){
                if(tradeRecord.getType().equals("close")){
                    map.put(tradeRecord.getOrderId(),tradeRecord);
                }

            }

            for (updateOrderDTO updateOrderDTO:mergedList){
                if(map.get(updateOrderDTO.getOrderId()) != null){
                    TradeRecord tradeRecord = map.get(updateOrderDTO.getOrderId());
                    double lots = tradeRecord.getLots();
                    updateOrderDTO.setEndPrice(tradeRecord.getPrice());
                    Double eurusdProfit = getEURUSDProfit(updateOrderDTO.getStartPrice(), updateOrderDTO.getEndPrice(), updateOrderDTO.getAction(), lots);



                    updateProfitForOrder.updateProfitForOrder(records, updateOrderDTO.getOrderId(), eurusdProfit);
                    // 3. 将修改后的记录写回Excel文件
                    CsvWriter.writeRecordsToExcel(records, filePath);

                    System.out.println("交易记录已成功更新并保存到Excel文件: " + filePath);
                }


            }


        } catch (  IOException e) {
            System.err.println("处理Excel文件时出错: " + e.getMessage());
        }

    }


    public void updateJBPJPYOrders( List<updateOrderDTO> mergedList){

        String filePath = "D:/data/高胜率/镑日最大胜率版 - 副本.xlsx"; // Excel文件路径

        try {
            // 1. 从Excel文件读取交易记录
            List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);
            Map<Integer,TradeRecord> map = new HashMap<>();
            for (TradeRecord tradeRecord: records){
                if(tradeRecord.getType().equals("close")){
                    map.put(tradeRecord.getOrderId(),tradeRecord);
                }

            }

            for (updateOrderDTO updateOrderDTO:mergedList){
                if(map.get(updateOrderDTO.getOrderId()) != null){
                    TradeRecord tradeRecord = map.get(updateOrderDTO.getOrderId());
                    double lots = tradeRecord.getLots();
                    if(updateOrderDTO.getEndPrice() == null || updateOrderDTO.getEndPrice() == 0.0){
                        updateOrderDTO.setEndPrice(tradeRecord.getPrice());
                    }

                    Double eurusdProfit = getJBPJPYProfit(updateOrderDTO.getStartPrice(), updateOrderDTO.getEndPrice(), updateOrderDTO.getAction(), lots);



                    updateProfitForOrder.updateProfitForOrder(records, updateOrderDTO.getOrderId(), eurusdProfit);
                    // 3. 将修改后的记录写回Excel文件
                    CsvWriter.writeRecordsToExcel(records, filePath);

                    System.out.println("交易记录已成功更新并保存到Excel文件: " + filePath);
                }


            }


        } catch (  IOException e) {
            System.err.println("处理Excel文件时出错: " + e.getMessage());
        }

    }

    public Double getXAUUSDProfit( Double startPrice,Double endPrice,  String action,Double lots){
        Double result=0.00;
        if(action.equals("多")){
            // 1. 减法：a - b
            BigDecimal subtractResult = BigDecimal.valueOf(endPrice).subtract(BigDecimal.valueOf(startPrice));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots)).multiply(new BigDecimal(100));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }else if(action.equals("空")){
            // 1. 减法：a - b
            BigDecimal subtractResult = BigDecimal.valueOf(startPrice).subtract(BigDecimal.valueOf(endPrice));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots)).multiply(new BigDecimal(100));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        return result;
    }

    public Double getEURUSDProfit( Double startPrice,Double endPrice,  String action,Double lots){
        Double result=0.00;
        if(action.equals("多")){
            // 1. 减法：a - b
            BigDecimal subtractResult = BigDecimal.valueOf(endPrice).subtract(BigDecimal.valueOf(startPrice));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots)).multiply(new BigDecimal(100000));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }else if(action.equals("空")){
            // 1. 减法：a - b
            BigDecimal subtractResult = BigDecimal.valueOf(startPrice).subtract(BigDecimal.valueOf(endPrice));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots)).multiply(new BigDecimal(100000));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        return result;
    }

    public Double getJBPJPYProfit( Double startPrice,Double endPrice,  String action,Double lots){
        Double result=0.00;
        if(action.equals("多")){
            // 1. 减法：a - b
            double s1 = (double) Math.round((endPrice / 143.00) * 1000) / 1000;
            double e1 = (double) Math.round((startPrice / 143.00) * 1000) / 1000;

            BigDecimal subtractResult = BigDecimal.valueOf(s1).subtract(BigDecimal.valueOf(e1));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots));

            finalResult = finalResult.multiply(new BigDecimal(100000));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }else if(action.equals("空")){
            // 1. 减法：a - b
            double s1 = (double) Math.round((endPrice / 143.00) * 100) / 100;
            double e1 = (double) Math.round((startPrice / 143.00) * 100) / 100;
            BigDecimal subtractResult = BigDecimal.valueOf(e1).subtract(BigDecimal.valueOf(s1));
            // 2. 乘法：结果 * multiplier
            BigDecimal finalResult = subtractResult.multiply(BigDecimal.valueOf(lots)).multiply(new BigDecimal(100000));

            // 3. 保留 2 位小数（四舍五入）
            result = finalResult.setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        return result;
    }
}
