package com.demo.extract.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 时间范围实体
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange {
    private String start;           // 开始时间（HH:mm）
    private String end;             // 结束时间（HH:mm）
}
