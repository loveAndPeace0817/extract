package com.demo.extract.zzq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class zzqdto {
    private String date;//日期
    private String type;//品种
    private Double value;//预测下一个值
    private String fluctuation;//预测涨跌
    private Double trueValue;//实际下一个值
    private String truefluctuation;//实际涨跌
}
