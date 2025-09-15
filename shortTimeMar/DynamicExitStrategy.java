package com.demo.extract.shortTimeMar;

import com.demo.extract.model.KlineData;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于历史轨迹统计的动态平仓策略核心类
 */
public class DynamicExitStrategy {
    // 配置参数
    private static final int MAX_HOLDING_TIME_HOURS = 6; // 最大持仓时间（小时）
    private static final int TIME_INTERVAL_MINUTES = 10; // 时间间隔（分钟）
    private static final int TOTAL_TIME_INTERVALS = (MAX_HOLDING_TIME_HOURS * 60) / TIME_INTERVAL_MINUTES; // 总时间间隔数
    private static final double RETURN_MIN_PERCENT = -62.0; // 收益率最小值（百分比）
    private static final double RETURN_MAX_PERCENT = 34.0; // 收益率最大值（百分比）
    private static final double RETURN_INTERVAL_PERCENT = 0.5; // 收益率间隔（百分比）
    private static final int MIN_SAMPLE_SIZE = 30; // 最小样本数阈值
    private static final double CONSERVATIVE_ADJUSTMENT = 0.5; // 保守调整值（百分比）
    
    // 可配置的保护期参数
    private int protectionPeriodMinutes = 90; // 默认保护期为90分钟
    private double protectionReturnThreshold = 0.1; // 默认保护期内收益率阈值
    
    // 经验价值函数存储
    private Map<TradingState, Double> valueFunctionMap; // 存储每个状态的价值函数
    private List<PositionTrajectory> allTrajectories; // 所有持仓轨迹
    
    /**
     * 构造函数
     */
    public DynamicExitStrategy() {
        this.valueFunctionMap = new HashMap<>();
        this.allTrajectories = new ArrayList<>();
    }
    
    /**
     * 获取保护期分钟数
     */
    public int getProtectionPeriodMinutes() {
        return protectionPeriodMinutes;
    }
    
    /**
     * 设置保护期分钟数
     */
    public void setProtectionPeriodMinutes(int protectionPeriodMinutes) {
        this.protectionPeriodMinutes = protectionPeriodMinutes;
    }
    
    /**
     * 获取保护期内收益率阈值
     */
    public double getProtectionReturnThreshold() {
        return protectionReturnThreshold;
    }
    
    /**
     * 设置保护期内收益率阈值
     */
    public void setProtectionReturnThreshold(double protectionReturnThreshold) {
        this.protectionReturnThreshold = protectionReturnThreshold;
    }
    
    /**
     * 生成开仓信号
     * @param klineDataList K线数据列表
     * @param originalStrategy 原始策略实现
     * @return 开仓信号列表
     */
    public List<OpenSignal> generateOpenSignals(List<KlineData> klineDataList, OriginalStrategy originalStrategy) {
        List<OpenSignal> openSignals = new ArrayList<>();
        
        for (int i = 0; i < klineDataList.size(); i++) {
            KlineData currentKline = klineDataList.get(i);
            boolean shouldOpen = originalStrategy.shouldOpenPosition(klineDataList, i);
            
            if (shouldOpen) {
                String direction = originalStrategy.getPositionDirection(klineDataList, i);
                OpenSignal signal = new OpenSignal(
                        "signal_" + System.nanoTime(),
                        currentKline.getTimestamp(),
                        currentKline.getClose(),
                        currentKline.getSymbol(),
                        direction
                );
                openSignals.add(signal);
            }
        }
        
        return openSignals;
    }
    
    /**
     * 生成持仓轨迹
     * @param openSignals 开仓信号列表
     * @param allKlineData 所有K线数据
     */
    public void generateTrajectories(List<OpenSignal> openSignals, List<KlineData> allKlineData) {
        for (OpenSignal signal : openSignals) {
            PositionTrajectory trajectory = simulatePositionTrajectory(signal, allKlineData);
            if (trajectory != null) {
                allTrajectories.add(trajectory);
            }
        }
        System.out.println("生成了" + allTrajectories.size() + "条持仓轨迹");
    }
    
    /**
     * 模拟单个持仓轨迹
     * @param signal 开仓信号
     * @param allKlineData 所有K线数据
     * @return 持仓轨迹
     */
    private PositionTrajectory simulatePositionTrajectory(OpenSignal signal, List<KlineData> allKlineData) {
        try {
            // 找到开仓信号对应的K线索引
            int startIndex = -1;
            for (int i = 0; i < allKlineData.size(); i++) {
                if (allKlineData.get(i).getTimestamp().isEqual(signal.getTimestamp()) || 
                    allKlineData.get(i).getTimestamp().isAfter(signal.getTimestamp())) {
                    startIndex = i;
                    break;
                }
            }
            
            if (startIndex == -1) {
                return null; // 没有找到对应的K线数据
            }
            
            // 创建持仓轨迹
            PositionTrajectory trajectory = new PositionTrajectory(
                    signal.getSignalId(),
                    signal.getTimestamp(),
                    signal.getOpenPrice(),
                    signal.getSymbol(),
                    signal.getDirection()
            );
            
            // 模拟持仓过程，计算每个时间点的收益率
            LocalDateTime maxHoldTime = signal.getTimestamp().plusHours(MAX_HOLDING_TIME_HOURS);
            int currentTimeStep = 0;
            
            for (int i = startIndex; i < allKlineData.size(); i++) {
                KlineData currentKline = allKlineData.get(i);
                
                // 检查是否超过最大持仓时间
                if (currentKline.getTimestamp().isAfter(maxHoldTime)) {
                    break;
                }
                
                // 计算时间步长
                Duration duration = Duration.between(signal.getTimestamp(), currentKline.getTimestamp());
                int minutesSinceOpen = (int) duration.toMinutes();
                
                // 只在指定的时间间隔点记录收益率
                if (minutesSinceOpen % TIME_INTERVAL_MINUTES == 0 || i == startIndex) {
                    double returnRate;
                    double lots = 30000.00/currentKline.getClose()/100;
                    DecimalFormat df = new DecimalFormat("#.##");
                    lots = Double.parseDouble(df.format(lots));

                    if ("多头".equals(signal.getDirection())) {
                        returnRate = (currentKline.getClose() / signal.getOpenPrice() - 1) * lots;
                    } else {
                        returnRate = (signal.getOpenPrice() / currentKline.getClose() - 1) * lots;
                    }
                    
                    // 限制收益率在设定范围内
                    returnRate = Math.max(RETURN_MIN_PERCENT, Math.min(RETURN_MAX_PERCENT, returnRate));
                    
                    trajectory.addReturnPoint(currentTimeStep, currentKline.getTimestamp(), returnRate);
                    currentTimeStep++;
                }
            }
            
            // 设置最终收益率
            if (!trajectory.getReturnPoints().isEmpty()) {
                PositionTrajectory.ReturnPoint lastPoint = trajectory.getReturnPoints().get(trajectory.getReturnPoints().size() - 1);
                trajectory.setFinalReturnRate(lastPoint.getReturnRate());
                trajectory.setCloseTime(lastPoint.getTimestamp());
            }
            
            return trajectory;
        } catch (Exception e) {
            System.err.println("模拟持仓轨迹时出错: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 计算经验价值函数
     */
    public void computeValueFunction() {
        // 首先，将所有轨迹数据分配到状态箱子中
        Map<TradingState, List<Double>> stateFinalReturnsMap = new HashMap<>();
        
        for (PositionTrajectory trajectory : allTrajectories) {
            double finalReturnRate = trajectory.getFinalReturnRate();
            
            for (PositionTrajectory.ReturnPoint point : trajectory.getReturnPoints()) {
                // 离散化时间和收益率
                TradingState state = discretizeState(point.getTimeStep(), point.getReturnRate());
                
                // 将当前状态和最终收益率关联起来
                stateFinalReturnsMap.computeIfAbsent(state, k -> new ArrayList<>()).add(finalReturnRate);
            }
        }
        
        // 计算每个状态的价值函数
        for (Map.Entry<TradingState, List<Double>> entry : stateFinalReturnsMap.entrySet()) {
            TradingState state = entry.getKey();
            List<Double> finalReturns = entry.getValue();
            
            double averageReturn;
            if (finalReturns.size() >= MIN_SAMPLE_SIZE) {
                // 计算平均值
                averageReturn = finalReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            } else {
                // 样本数不足，使用保守值
                averageReturn = state.getReturnRate() - CONSERVATIVE_ADJUSTMENT;
                System.out.println("状态 " + state + " 的样本数不足，使用保守值: " + averageReturn);
            }
            
            valueFunctionMap.put(state, averageReturn);
        }
        
        System.out.println("计算完成，共包含 " + valueFunctionMap.size() + " 个状态的价值函数");
    }
    
    /**
     * 离散化状态
     * @param timeStep 时间步长
     * @param returnRate 收益率
     * @return 离散化后的交易状态
     */
    private TradingState discretizeState(int timeStep, double returnRate) {
        // 离散化时间
        int timeInterval = Math.min(timeStep, TOTAL_TIME_INTERVALS - 1);
        String timeIntervalLabel = timeInterval + "-" + (timeInterval + 1) + "区间";
        
        // 离散化收益率
        int returnIntervalCount = (int) ((returnRate - RETURN_MIN_PERCENT) / RETURN_INTERVAL_PERCENT);
        returnIntervalCount = Math.max(0, Math.min((int)((RETURN_MAX_PERCENT - RETURN_MIN_PERCENT) / RETURN_INTERVAL_PERCENT), returnIntervalCount));
        
        double returnIntervalStart = RETURN_MIN_PERCENT + returnIntervalCount * RETURN_INTERVAL_PERCENT;
        double returnIntervalEnd = returnIntervalStart + RETURN_INTERVAL_PERCENT;
        String returnRateLabel = "[" + String.format("%.1f", returnIntervalStart) + ", " + String.format("%.1f", returnIntervalEnd) + ")";
        
        // 使用区间中间值作为状态的收益率表示
        double stateReturnRate = returnIntervalStart + RETURN_INTERVAL_PERCENT / 2;
        
        return new TradingState(timeInterval, stateReturnRate, timeIntervalLabel, returnRateLabel);
    }
    
    /**
     * 判断是否应该平仓
     * @param currentTimeStep 当前时间步长
     * @param currentReturnRate 当前收益率
     * @return 是否应该平仓
     */
    public boolean shouldExitPosition(int currentTimeStep, double currentReturnRate) {
        // 开仓初期保护：持仓时间在保护期内，采取更保守的策略
        // 不轻易平仓，除非有明显的盈利或亏损
        int minutesSinceOpen = currentTimeStep * TIME_INTERVAL_MINUTES;
        if (minutesSinceOpen <= protectionPeriodMinutes) {
            // 在保护期内，只有当收益率绝对值大于设定阈值时才考虑平仓
            if (Math.abs(currentReturnRate) <= protectionReturnThreshold) {
                System.out.println("开仓初期保护（持仓" + minutesSinceOpen + "分钟），继续持有，当前收益率(" + currentReturnRate + ")");
                return false;
            }
        }
        
        // 如果已经达到最大持仓时间，强制平仓
        if (currentTimeStep >= TOTAL_TIME_INTERVALS) {
            System.out.println("达到最大持仓时间，强制平仓");
            return true;
        }
        
        // 离散化当前状态
        TradingState currentState = discretizeState(currentTimeStep, currentReturnRate);
        
        // 获取该状态的价值函数
        Double valueFunction = valueFunctionMap.get(currentState);
        
        if (valueFunction == null) {
            // 没有找到对应的价值函数，使用保守策略
            System.out.println("未找到状态的价值函数，使用保守策略");
            return currentReturnRate > 0; // 如果当前盈利，就平仓
        }
        
        // 比较当前收益率和继续持有的期望收益
        boolean shouldExit = currentReturnRate > valueFunction;
        
        if (shouldExit) {
            System.out.println("当前收益率(" + currentReturnRate + ")高于继续持有的期望收益(" + valueFunction + ")，建议平仓");
        } else {
            System.out.println("当前收益率(" + currentReturnRate + ")低于继续持有的期望收益(" + valueFunction + ")，建议继续持有");
        }
        
        return shouldExit;
    }
    
    /**
     * 开仓信号类
     */
    public static class OpenSignal {
        private String signalId;
        private LocalDateTime timestamp;
        private double openPrice;
        private String symbol;
        private String direction;
        
        public OpenSignal(String signalId, LocalDateTime timestamp, double openPrice, String symbol, String direction) {
            this.signalId = signalId;
            this.timestamp = timestamp;
            this.openPrice = openPrice;
            this.symbol = symbol;
            this.direction = direction;
        }
        
        // Getters
        public String getSignalId() { return signalId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getOpenPrice() { return openPrice; }
        public String getSymbol() { return symbol; }
        public String getDirection() { return direction; }
    }
    
    /**
     * 原始策略接口
     */
    public interface OriginalStrategy {
        boolean shouldOpenPosition(List<KlineData> klineDataList, int currentIndex);
        String getPositionDirection(List<KlineData> klineDataList, int currentIndex);
    }
    
    // Getters
    public Map<TradingState, Double> getValueFunctionMap() {
        return new HashMap<>(valueFunctionMap);
    }
    
    public List<PositionTrajectory> getAllTrajectories() {
        return new ArrayList<>(allTrajectories);
    }
}