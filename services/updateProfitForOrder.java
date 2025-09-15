package com.demo.extract.services;

import com.demo.extract.DTO.TradeRecord;

import java.util.List;

//修改交割单业务逻辑
public class updateProfitForOrder {
    // 根据订单号更新获利金额
    public static void updateProfitForOrder(List<TradeRecord> records, int orderId, double newProfit) {
        for (int i = 0; i < records.size(); i++) {
            if ("close".equals(records.get(i).getType()) && records.get(i).getOrderId() == orderId) {
                if(i > 1 && records.get(i-1).getLots() ==records.get(i).getLots()){
                    records.get(i).setProfit(newProfit);
                    System.out.println("已更新订单号 " + orderId + " 的获利金额为: " + newProfit);
                    return;
                }
                /*if(i > 1 && records.get(i-1).getLots() !=records.get(i).getLots()){
                    records.get(i).setProfit(newProfit);
                    System.out.println("已更新订单号 " + orderId + " 的获利金额为: " + newProfit);
                    for (int j = 0; j <  records.size(); j++) {
                        if ("close".equals(records.get(j).getType()) && records.get(j).getOrderId() == orderId+1){
                            records.get(j).setProfit(0.00);
                            System.out.println("已更新半仓订单号 " + orderId+1 + " 的获利金额为: " + 0.00);
                            return;
                        }
                    }
                    return;
                }*/


            }
        }

        System.out.println("未找到订单号为 " + orderId + " 的平仓记录");
    }
}
