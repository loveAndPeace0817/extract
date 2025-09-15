package com.demo.extract.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 金融数据点实体类
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDataPoint {
    private Double value;          // 价格值（bps）
    private String timestamp;      // 时间戳（格式：yyyy-MM-dd HH:mm:ss）
    private String id;

    public FinancialDataPoint(Double value, String timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }
}






