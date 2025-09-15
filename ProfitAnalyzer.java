package com.demo.extract;

import com.demo.extract.DTO.FinancialDataPoint;
import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.services.DataLoader;
import com.demo.extract.services.DataLoaderNew;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ProfitAnalyzer {

    // 需要计算的指标
    public static class ProfitStatistics {
        private int count;
        private double min;
        private double max;
        private double avg;
        private double median;
        private double stdDev;
        private double positivePercentage;
        private double negativePercentage;
        private Map<String, ProfitStatistics> orderGroupStats;

        // 构造函数、getter和setter方法
        public ProfitStatistics() {
            this.orderGroupStats = new HashMap<>();
        }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        public double getAvg() { return avg; }
        public void setAvg(double avg) { this.avg = avg; }
        public double getMedian() { return median; }
        public void setMedian(double median) { this.median = median; }
        public double getStdDev() { return stdDev; }
        public void setStdDev(double stdDev) { this.stdDev = stdDev; }
        public double getPositivePercentage() { return positivePercentage; }
        public void setPositivePercentage(double positivePercentage) { this.positivePercentage = positivePercentage; }
        public double getNegativePercentage() { return negativePercentage; }
        public void setNegativePercentage(double negativePercentage) { this.negativePercentage = negativePercentage; }
        public Map<String, ProfitStatistics> getOrderGroupStats() { return orderGroupStats; }
        public void setOrderGroupStats(Map<String, ProfitStatistics> orderGroupStats) { this.orderGroupStats = orderGroupStats; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Profit Statistics:\n");
            sb.append("  Count: " + count + "\n");
            sb.append("  Min: " + min + "\n");
            sb.append("  Max: " + max + "\n");
            sb.append("  Average: " + avg + "\n");
            sb.append("  Median: " + median + "\n");
            sb.append("  Standard Deviation: " + stdDev + "\n");
            sb.append("  Positive Percentage: " + positivePercentage + "%\n");
            sb.append("  Negative Percentage: " + negativePercentage + "%\n");

            if (!orderGroupStats.isEmpty()) {
                sb.append("\nOrder Group Statistics:\n");
                for (Map.Entry<String, ProfitStatistics> entry : orderGroupStats.entrySet()) {
                    sb.append("Order " + entry.getKey() + ":\n");
                    sb.append("  Count: " + entry.getValue().getCount() + "\n");
                    sb.append("  Min: " + entry.getValue().getMin() + "\n");
                    sb.append("  Max: " + entry.getValue().getMax() + "\n");
                    sb.append("  Average: " + entry.getValue().getAvg() + "\n");
                    sb.append("  Median: " + entry.getValue().getMedian() + "\n");
                    sb.append("  Standard Deviation: " + entry.getValue().getStdDev() + "\n");
                    sb.append("  Positive Percentage: " + entry.getValue().getPositivePercentage() + "%\n");
                    sb.append("  Negative Percentage: " + entry.getValue().getNegativePercentage() + "%\n\n");
                }
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        try {
            // 加载CSV数据
            List<FinancialDataPoint> dataPoints = getdata();
            if (dataPoints.isEmpty()) {
                System.out.println("No data loaded.");
                return;
            }

            // 分析profit数据
            ProfitStatistics stats = analyzeProfit(dataPoints);

            // 打印结果
            System.out.println(stats);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static ProfitStatistics analyzeProfit(List<FinancialDataPoint> dataPoints) {
        ProfitStatistics result = new ProfitStatistics();

        // 提取所有profit值
        List<Double> profits = dataPoints.stream()
                .map(FinancialDataPoint::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 按订单号分组
        Map<String, List<FinancialDataPoint>> orderGroups = dataPoints.stream()
                .filter(dp -> dp.getId() != null)
                .collect(Collectors.groupingBy(FinancialDataPoint::getId));

        // 计算整体统计指标
        result.setCount(profits.size());
        result.setMin(profits.stream().mapToDouble(Double::doubleValue).min().orElse(0));
        result.setMax(profits.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        result.setAvg(profits.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        result.setMedian(calculateMedian(profits));
        result.setStdDev(calculateStdDev(profits, result.getAvg()));

        // 计算正负百分比
        long positiveCount = profits.stream().filter(p -> p > 0).count();
        long negativeCount = profits.stream().filter(p -> p < 0).count();
        result.setPositivePercentage((positiveCount * 100.0) / profits.size());
        result.setNegativePercentage((negativeCount * 100.0) / profits.size());

        // 计算每个订单组的统计指标
        Map<String, ProfitStatistics> orderStatsMap = new HashMap<>();
        for (Map.Entry<String, List<FinancialDataPoint>> entry : orderGroups.entrySet()) {
            String orderId = entry.getKey();
            List<FinancialDataPoint> groupData = entry.getValue();

            ProfitStatistics orderStats = new ProfitStatistics();
            List<Double> groupProfits = groupData.stream()
                    .map(FinancialDataPoint::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            orderStats.setCount(groupProfits.size());
            orderStats.setMin(groupProfits.stream().mapToDouble(Double::doubleValue).min().orElse(0));
            orderStats.setMax(groupProfits.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            orderStats.setAvg(groupProfits.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            orderStats.setMedian(calculateMedian(groupProfits));
            orderStats.setStdDev(calculateStdDev(groupProfits, orderStats.getAvg()));

            long groupPositiveCount = groupProfits.stream().filter(p -> p > 0).count();
            long groupNegativeCount = groupProfits.stream().filter(p -> p < 0).count();
            orderStats.setPositivePercentage((groupPositiveCount * 100.0) / groupProfits.size());
            orderStats.setNegativePercentage((groupNegativeCount * 100.0) / groupProfits.size());

            orderStatsMap.put(orderId, orderStats);
        }

        result.setOrderGroupStats(orderStatsMap);
        return result;
    }

    private static double calculateMedian(List<Double> values) {
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }

    private static double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    public static List<FinancialDataPoint> getdata() throws IOException {
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<FinancialDataPoint> result = new ArrayList<>();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/测试777.csv");
        for(OrderTimeSeries orderTimeSeries : allSeries){
            if(orderTimeSeries.getValueTime().length>70){

                double[] values = orderTimeSeries.getValues();
                for (int i = 0; i <values.length ; i++) {
                    FinancialDataPoint financialDataPoint = new FinancialDataPoint();
                    financialDataPoint.setId(orderTimeSeries.getOrderId());
                    financialDataPoint.setValue(values[i]);
                    financialDataPoint.setTimestamp(orderTimeSeries.getValueTime()[i]);
                    result.add(financialDataPoint);
                }
            }
        }
        return  result;
    }
}