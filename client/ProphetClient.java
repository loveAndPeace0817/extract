package com.demo.extract.client;

import com.demo.extract.DTO.ProphetResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

import org.springframework.http.*;


public class ProphetClient {
    private static final String PYTHON_API_URL = "http://localhost:5000/predict";
    private static final ObjectMapper mapper = new ObjectMapper();

    public  ProphetResponse sendData(List<List<Double>> l1,List<Double> l2) throws JsonProcessingException {
        // 1. 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "your-secret-key"); // 可选认证

        // 2. 准备请求体
        Map<String, Object> requestData = new HashMap<>();
        /*requestData.put("historyData", Arrays.asList(
                Arrays.asList(1.1, 2.2, 3.3), // 示例历史数据1
                Arrays.asList(4.4, 5.5, 6.6)  // 示例历史数据2
        ));*/
        requestData.put("historyData",l1);
        requestData.put("targetData",l2);
        //requestData.put("targetData", Arrays.asList(7.7, 8.8, 9.9)); // 示例目标数据

        // 3. 创建请求实体
        HttpEntity<String> request = new HttpEntity<>(
                mapper.writeValueAsString(requestData),
                headers
        );

        // 4. 发送请求
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    PYTHON_API_URL,
                    request,
                    String.class
            );

            // 5. 处理响应
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("预测结果: " + response.getBody());

                // 方式2：转换为强类型对象（推荐）
                ProphetResponse responseObj = mapper.readValue(
                        response.getBody(),
                        ProphetResponse.class
                );
                return responseObj;
            } else {
                System.out.println("请求失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("API调用异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}