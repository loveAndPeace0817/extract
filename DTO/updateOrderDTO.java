package com.demo.extract.DTO;

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
public class updateOrderDTO {
    private Integer orderId;
    private Double startPrice;
    private Double endPrice;
    private String action;
}
