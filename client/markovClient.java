package com.demo.extract.client;


import com.demo.extract.DTO.AnalysisResult;
import com.demo.extract.DTO.FinancialDataPoint;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * 金融分析服务客户端
 */
@NoArgsConstructor
public class markovClient {

    private static final String PYTHON_SERVICE_URL = "http://localhost:5000/api/analyze";

    private RestTemplate restTemplate;

    public void FinancialAnalysisClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public markovClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用Python分析服务
     * @param dataPoints 金融数据点列表
     * @return 分析结果
     */
    public AnalysisResult analyzeMarketData(List<FinancialDataPoint> dataPoints) {
        // 1. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        // 2. 构建请求实体
        HttpEntity<List<FinancialDataPoint>> requestEntity =
                new HttpEntity<>(dataPoints, headers);

        try {
            // 3. 发送POST请求
            ResponseEntity<AnalysisResult> response = restTemplate.exchange(
                    PYTHON_SERVICE_URL,
                    HttpMethod.POST,
                    requestEntity,
                    AnalysisResult.class
            );

            // 4. 检查响应状态
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("服务调用失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            // 5. 异常处理
            AnalysisResult errorResult = new AnalysisResult();
            errorResult.setStatus("error");
            errorResult.setMessage("调用分析服务失败: " + e.getMessage());
            errorResult.setTimestamp(java.time.LocalDateTime.now().toString());
            return errorResult;
        }
    }

    /**
     * 带重试机制的调用方法
     * @param dataPoints 金融数据点列表
     * @param maxRetries 最大重试次数
     * @return 分析结果
     */
    public AnalysisResult analyzeWithRetry(List<FinancialDataPoint> dataPoints, int maxRetries) {
        int attempts = 0;
        AnalysisResult result;

        do {
            attempts++;
            result = analyzeMarketData(dataPoints);

            if ("success".equals(result.getStatus())) {
                return result;
            }

            try {
                Thread.sleep(1000 * attempts); // 指数退避
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } while (attempts < maxRetries);

        return result;
    }


}