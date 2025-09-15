package com.demo.extract.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 定义响应数据结构
@Data
@Getter
@Setter
public class ProphetResponse {
    private String status;
    private List<Prediction> predictions;

    // getters/setters
    @Getter
    @Setter
    public static class Prediction {
        private String time;
        private double predictedValue;
        private double lowerBound;
        private double upperBound;


    }
}


