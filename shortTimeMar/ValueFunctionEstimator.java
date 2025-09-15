package com.demo.extract.shortTimeMar;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 价值函数估计器类，用于计算经验价值函数V_hold(s)
 */
public class ValueFunctionEstimator {
    private static final int DEFAULT_MIN_SAMPLES_PER_STATE = 30; // 每个状态的最小样本数
    private static final double DEFAULT_CONSERVATIVE_ADJUSTMENT = 0.5; // 保守调整值（百分比）
    
    // 与DynamicExitStrategy相同的配置参数
    private static final int MAX_HOLDING_TIME_HOURS = 6; // 最大持仓时间（小时）
    private static final int TIME_INTERVAL_MINUTES = 10; // 时间间隔（分钟）
    private static final int TOTAL_TIME_INTERVALS = (MAX_HOLDING_TIME_HOURS * 60) / TIME_INTERVAL_MINUTES; // 总时间间隔数
    private static final double RETURN_MIN_PERCENT = -62.0; // 收益率最小值（百分比）
    private static final double RETURN_MAX_PERCENT = 34.0; // 收益率最大值（百分比）
    private static final double RETURN_INTERVAL_PERCENT = 0.5; // 收益率间隔（百分比）
    
    // 计算分箱数
    private static final int TIME_BINS = TOTAL_TIME_INTERVALS; // 时间维度的分箱数
    private static final int RETURN_BINS = (int)Math.ceil((RETURN_MAX_PERCENT - RETURN_MIN_PERCENT) / RETURN_INTERVAL_PERCENT); // 收益率维度的分箱数
    
    private int timeBins; // 时间维度的分箱数
    private int returnBins; // 收益率维度的分箱数
    private double returnMin; // 收益率最小值（百分比）
    private double returnMax; // 收益率最大值（百分比）
    private int maxHoldingTimeMinutes; // 最大持仓时间（分钟）
    private int timeIntervalMinutes; // 时间间隔（分钟）
    private int minSamplesPerState; // 每个状态的最小样本数
    
    // 存储每个状态的价值和样本数
    private Map<TradingState, StateStatistics> stateStatisticsMap;
    
    /**
     * 构造函数（使用默认参数）
     */
    public ValueFunctionEstimator() {
        this.timeBins = TIME_BINS;
        this.returnBins = RETURN_BINS;
        this.returnMin = RETURN_MIN_PERCENT;
        this.returnMax = RETURN_MAX_PERCENT;
        this.maxHoldingTimeMinutes = MAX_HOLDING_TIME_HOURS * 60;
        this.timeIntervalMinutes = TIME_INTERVAL_MINUTES;
        this.minSamplesPerState = DEFAULT_MIN_SAMPLES_PER_STATE;
        this.stateStatisticsMap = new HashMap<>();
    }
    
    /**
     * 构造函数（使用自定义参数）
     */
    public ValueFunctionEstimator(int timeBins, int returnBins, 
                                double returnMin, double returnMax, 
                                int maxHoldingTimeMinutes, int timeIntervalMinutes,
                                int minSamplesPerState) {
        this.timeBins = timeBins;
        this.returnBins = returnBins;
        this.returnMin = returnMin;
        this.returnMax = returnMax;
        this.maxHoldingTimeMinutes = maxHoldingTimeMinutes;
        this.timeIntervalMinutes = timeIntervalMinutes;
        this.minSamplesPerState = minSamplesPerState;
        this.stateStatisticsMap = new HashMap<>();
    }
    
    /**
     * 从持仓轨迹数据中学习价值函数
     * @param trajectories 持仓轨迹列表
     */
    public void learnValueFunction(List<PositionTrajectory> trajectories) {
        System.out.println("开始学习价值函数，轨迹数量: " + trajectories.size());
        
        // 清空现有数据
        stateStatisticsMap.clear();
        
        // 处理每条轨迹
        for (PositionTrajectory trajectory : trajectories) {
            processTrajectory(trajectory);
        }
        
        System.out.println("价值函数学习完成，状态数量: " + stateStatisticsMap.size());
    }
    
    /**
     * 处理单条持仓轨迹
     * @param trajectory 持仓轨迹
     */
    private void processTrajectory(PositionTrajectory trajectory) {
        try {
            List<PositionTrajectory.ReturnPoint> returnPoints = trajectory.getReturnPoints();
            double finalReturnRate = trajectory.getFinalReturnRate();
            
            if (returnPoints.isEmpty()) {
                return;
            }
            
            // 为轨迹中的每个点计算价值
            for (PositionTrajectory.ReturnPoint point : returnPoints) {
                // 根据时间步长确定时间区间
                int timeStep = point.getTimeStep();
                int timeBin = getTimeBin(timeStep);
                
                // 根据收益率确定收益区间
                double returnRate = point.getReturnRate();
                // 限制收益率在设定范围内
                returnRate = Math.max(returnMin, Math.min(returnMax, returnRate));
                int returnBin = getReturnBin(returnRate);
                
                // 创建交易状态
                TradingState state = new TradingState(timeBin, returnBin);
                
                // 更新状态统计信息
                stateStatisticsMap.computeIfAbsent(state, k -> new StateStatistics())
                    .addSample(finalReturnRate);
            }
        } catch (Exception e) {
            System.err.println("处理轨迹时出错: " + e.getMessage());
        }
    }
    
    /**
     * 获取时间区间
     * @param timeStep 时间步长
     * @return 时间区间索引
     */
    private int getTimeBin(int timeStep) {
        // 计算当前时间区间
        int timeBin = Math.min(timeBins - 1, timeStep);
        return timeBin;
    }
    
    /**
     * 获取收益区间
     * @param returnRate 收益率
     * @return 收益区间索引
     */
    private int getReturnBin(double returnRate) {
        double returnRange = returnMax - returnMin;
        double binSize = returnRange / returnBins;
        
        // 计算收益区间
        int returnBin = (int) Math.max(0, Math.min(returnBins - 1, (returnRate - returnMin) / binSize));
        return returnBin;
    }
    
    /**
     * 获取指定状态的价值函数值V_hold(s)
     * @param timeStep 时间步长
     * @param returnRate 收益率
     * @return 价值函数值
     */
    public double getValueFunction(int timeStep, double returnRate) {
        // 转换为状态
        int timeBin = getTimeBin(timeStep);
        double clampedReturnRate = Math.max(returnMin, Math.min(returnMax, returnRate));
        int returnBin = getReturnBin(clampedReturnRate);
        TradingState state = new TradingState(timeBin, returnBin);
        
        // 获取状态统计信息
        StateStatistics statistics = stateStatisticsMap.get(state);
        
        // 如果状态存在且样本数足够
        if (statistics != null && statistics.getSampleCount() >= minSamplesPerState) {
            return statistics.getMeanReturn();
        }
        
        // 如果样本数不足，使用平滑处理或默认值
        return getSmoothedValue(state, clampedReturnRate);
    }
    
    /**
     * 获取平滑后的价值函数值
     * @param state 交易状态
     * @param returnRate 收益率
     * @return 平滑后的价值函数值
     */
    private double getSmoothedValue(TradingState state, double returnRate) {
        // 收集相邻状态的价值
        List<Double> neighborValues = new ArrayList<>();
        
        // 检查相邻的时间区间
        for (int t = Math.max(0, state.getTimeInterval() - 1); t <= Math.min(timeBins - 1, state.getTimeInterval() + 1); t++) {
            // 检查相邻的收益区间
            for (int r = Math.max(0, (int)state.getReturnRate() - 1); r <= Math.min(returnBins - 1, (int)state.getReturnRate() + 1); r++) {
                TradingState neighborState = new TradingState(t, r);
                StateStatistics neighborStats = stateStatisticsMap.get(neighborState);
                
                if (neighborStats != null && neighborStats.getSampleCount() >= minSamplesPerState / 2) {
                    neighborValues.add(neighborStats.getMeanReturn());
                }
            }
        }
        
        // 如果有相邻状态的数据，返回平均值，但确保结果不为负数
        if (!neighborValues.isEmpty()) {
            double avg = neighborValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            // 对于开仓初期，确保价值函数不为负
            if (state.getTimeInterval() < TOTAL_TIME_INTERVALS * 0.1) {
                return Math.max(0, avg);
            }
            return avg;
        }
        
        // 否则返回更合理的值：在初始阶段（时间步小）鼓励持有，在后期鼓励止盈
        double adjustedReturnRate;
        int timeInterval = state.getTimeInterval(); // 从状态中获取时间区间
        if (timeInterval < TOTAL_TIME_INTERVALS * 0.3) { // 前30%的时间，鼓励持有
            // 增加更大的保守调整值，确保初期价值函数为正
            adjustedReturnRate = returnRate + DEFAULT_CONSERVATIVE_ADJUSTMENT * 2;
        } else if (timeInterval < TOTAL_TIME_INTERVALS * 0.7) { // 中间40%的时间，保持中性
            adjustedReturnRate = returnRate;
        } else { // 后30%的时间，鼓励止盈
            adjustedReturnRate = returnRate - DEFAULT_CONSERVATIVE_ADJUSTMENT;
        }
        return adjustedReturnRate;
    }
    
    /**
     * 状态统计信息类
     */
    private static class StateStatistics {
        private double sumReturns; // 总收益率
        private int sampleCount; // 样本数
        private double sumSquaredReturns; // 平方和（用于计算方差）
        
        public StateStatistics() {
            this.sumReturns = 0;
            this.sampleCount = 0;
            this.sumSquaredReturns = 0;
        }
        
        /**
         * 添加一个样本
         * @param returnRate 收益率
         */
        public void addSample(double returnRate) {
            sumReturns += returnRate;
            sumSquaredReturns += returnRate * returnRate;
            sampleCount++;
        }
        
        /**
         * 获取平均收益率
         * @return 平均收益率
         */
        public double getMeanReturn() {
            return sampleCount > 0 ? sumReturns / sampleCount : 0;
        }
        
        /**
         * 获取收益率标准差
         * @return 标准差
         */
        public double getStandardDeviation() {
            if (sampleCount <= 1) {
                return 0;
            }
            double mean = getMeanReturn();
            double variance = (sumSquaredReturns - sampleCount * mean * mean) / (sampleCount - 1);
            return Math.sqrt(variance);
        }
        
        /**
         * 获取样本数
         * @return 样本数
         */
        public int getSampleCount() {
            return sampleCount;
        }
    }
    
    /**
     * 生成价值函数查询表
     * @return 价值函数查询表（JSON格式字符串）
     */
    public String generateValueFunctionLookupTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"valueFunction\": {");
        
        boolean firstState = true;
        
        // 按时间和收益排序的状态
        List<TradingState> sortedStates = stateStatisticsMap.keySet().stream()
                .sorted(Comparator.comparingInt(TradingState::getTimeInterval)
                        .thenComparingInt(s -> (int)s.getReturnRate()))
                .collect(Collectors.toList());
        
        for (TradingState state : sortedStates) {
            StateStatistics stats = stateStatisticsMap.get(state);
            
            if (firstState) {
                firstState = false;
            } else {
                sb.append(",");
            }
            
            sb.append("\"t");
            sb.append(state.getTimeInterval());
            sb.append("_r");
            sb.append((int)state.getReturnRate());
            sb.append("\": {");
            sb.append("\"meanReturn\":");
            sb.append(String.format("%.4f", stats.getMeanReturn()));
            sb.append(",");
            sb.append("\"sampleCount\":");
            sb.append(stats.getSampleCount());
            sb.append(",");
            sb.append("\"stdDev\":");
            sb.append(String.format("%.4f", stats.getStandardDeviation()));
            sb.append("}");
        }
        
        sb.append("}");
        sb.append(",");
        sb.append("\"metadata\": {");
        sb.append("\"timeBins\":");
        sb.append(timeBins);
        sb.append(",");
        sb.append("\"returnBins\":");
        sb.append(returnBins);
        sb.append(",");
        sb.append("\"returnMin\":");
        sb.append(returnMin);
        sb.append(",");
        sb.append("\"returnMax\":");
        sb.append(returnMax);
        sb.append(",");
        sb.append("\"minSamplesPerState\":");
        sb.append(minSamplesPerState);
        sb.append("}");
        sb.append("}");
        
        return sb.toString();
    }
    
    /**
     * 生成状态分布统计报告
     * @return 统计报告
     */
    public String generateStateDistributionReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 状态分布统计报告 =====\n");
        sb.append("总状态数量: ").append(stateStatisticsMap.size()).append("\n");
        sb.append("每个时间区间的状态数量:\n");
        
        // 计算每个时间区间的状态数量
        Map<Integer, Integer> timeBinCount = new HashMap<>();
        for (TradingState state : stateStatisticsMap.keySet()) {
            timeBinCount.put(state.getTimeInterval(), timeBinCount.getOrDefault(state.getTimeInterval(), 0) + 1);
        }
        
        // 按时间区间排序并打印
        for (int t = 0; t < timeBins; t++) {
            sb.append("时间区间 ").append(t).append(": ");
            sb.append(timeBinCount.getOrDefault(t, 0)).append("个状态\n");
        }
        
        sb.append("\n样本数统计:\n");
        
        // 计算样本数分布
        int sufficientSamplesCount = 0;
        int insufficientSamplesCount = 0;
        
        for (StateStatistics stats : stateStatisticsMap.values()) {
            if (stats.getSampleCount() >= minSamplesPerState) {
                sufficientSamplesCount++;
            } else {
                insufficientSamplesCount++;
            }
        }
        
        sb.append("样本数充足的状态 (≥").append(minSamplesPerState).append("): ");
        sb.append(sufficientSamplesCount).append("个 (").append(
                String.format("%.1f%%", (double) sufficientSamplesCount / stateStatisticsMap.size() * 100
        )).append(")\n");
        
        sb.append("样本数不足的状态 (<").append(minSamplesPerState).append("): ");
        sb.append(insufficientSamplesCount).append("个 (").append(
                String.format("%.1f%%", (double) insufficientSamplesCount / stateStatisticsMap.size() * 100
        )).append(")\n");
        
        sb.append("\n平均每个状态的样本数: ").append(
                String.format("%.1f", 
                        stateStatisticsMap.values().stream()
                                .mapToInt(StateStatistics::getSampleCount)
                                .average().orElse(0)
                )
        ).append("\n");
        
        sb.append("=========================");
        
        return sb.toString();
    }
    
    // Getters
    public Map<TradingState, StateStatistics> getStateStatisticsMap() {
        return new HashMap<>(stateStatisticsMap);
    }
    
    public int getTimeBins() { return timeBins; }
    public int getReturnBins() { return returnBins; }
    public double getReturnMin() { return returnMin; }
    public double getReturnMax() { return returnMax; }
    public int getMinSamplesPerState() { return minSamplesPerState; }
    
    // Setters
    public void setMinSamplesPerState(int minSamplesPerState) {
        this.minSamplesPerState = minSamplesPerState;
    }
}