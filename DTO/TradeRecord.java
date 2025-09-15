package com.demo.extract.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TradeRecord {
    private int id;          // 序号
    private String time;     // 时间
    private String type;     // 类型
    private Integer orderId;     // 订单号
    private double lots;     // 手数
    private double price;    // 价格
    private double profit;   // 获利
}
