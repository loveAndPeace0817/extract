package com.demo.extract.shortTimeMar;

import com.demo.extract.model.KlineData;
import com.demo.extract.shortTimeMar.PositionTrajectory;
import com.demo.extract.shortTimeMar.ValueFunctionEstimator;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
     * 策略回测和评估类
     */
    public class StrategyBacktester {
        private ValueFunctionEstimator valueFunctionEstimator; // 价值函数估计器（用于回测）
    // 与DynamicExitStrategy相同的配置参数
    private static final int MAX_HOLDING_TIME_HOURS = 6; // 最大持仓时间（小时）
    private static final int TIME_INTERVAL_MINUTES = 10; // 时间间隔（分钟）
    private static final double RETURN_MIN_PERCENT = -62.0; // 收益率最小值
    private static final double RETURN_MAX_PERCENT = 34.0; // 收益率最大值
    
    // 可配置的保护期参数
    private int protectionPeriodMinutes = 120; // 默认保护期为90分钟
    private double protectionReturnThreshold = 50; // 默认保护期内收益率阈值
    private DynamicExitStrategy dynamicExitStrategy;
    private List<TradeRecord> tradeRecords; // 交易记录
    private List<PositionTrajectory> backtestTrajectories; // 回测的持仓轨迹
    
    /**
     * 构造函数
     */
    public StrategyBacktester() {
        this.dynamicExitStrategy = new DynamicExitStrategy();
        this.tradeRecords = new ArrayList<>();
        this.backtestTrajectories = new ArrayList<>();
    }
    
    /**
     * 构造函数（使用自定义的动态平仓策略）
     */
    public StrategyBacktester(DynamicExitStrategy strategy) {
        this.dynamicExitStrategy = strategy;
        this.tradeRecords = new ArrayList<>();
        this.backtestTrajectories = new ArrayList<>();
        this.valueFunctionEstimator = null;
    }
    
    /**
     * 设置价值函数估计器（用于回测）
     */
    public void setValueFunctionEstimator(ValueFunctionEstimator estimator) {
        this.valueFunctionEstimator = estimator;
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
     * 运行回测
     * @param allKlineData 所有K线数据
     * @param originalStrategy 原始开仓策略
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 回测结果
     */
    public BacktestResult runBacktest(List<KlineData> allKlineData, 
                                      DynamicExitStrategy.OriginalStrategy originalStrategy, 
                                      LocalDateTime startDate, 
                                      LocalDateTime endDate) {
        System.out.println("开始回测: " + startDate + " 到 " + endDate);
        
        // 重置回测数据
        tradeRecords.clear();
        backtestTrajectories.clear();
        
        try {
            // 筛选指定时间范围内的K线数据
            List<KlineData> filteredKlineData = allKlineData.stream()
                    .filter(kline -> !kline.getTimestamp().isBefore(startDate) && !kline.getTimestamp().isAfter(endDate))
                    .collect(Collectors.toList());
            
            System.out.println("筛选出K线数据: " + filteredKlineData.size() + "条");
            
            // 生成开仓信号
            List<DynamicExitStrategy.OpenSignal> openSignals = dynamicExitStrategy.generateOpenSignals(filteredKlineData, originalStrategy);
            System.out.println("生成开仓信号: " + openSignals.size() + "个");
            
            // 模拟交易过程
            for (DynamicExitStrategy.OpenSignal signal : openSignals) {
                simulateTrade(signal, allKlineData);
            }
            
            // 计算回测结果
            BacktestResult result = calculateBacktestResult();
            return result;
            
        } catch (Exception e) {
            System.err.println("回测过程中出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 模拟单个交易
     * @param signal 开仓信号
     * @param allKlineData 所有K线数据
     */
    private void simulateTrade(DynamicExitStrategy.OpenSignal signal, List<KlineData> allKlineData) {
        try {
            double lots = 30000.00 / signal.getOpenPrice() / 100;
            DecimalFormat df = new DecimalFormat("#.##");
            lots = Double.parseDouble(df.format(lots));

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
                return; // 没有找到对应的K线数据
            }
            
            // 模拟持仓过程
            LocalDateTime openTime = signal.getTimestamp();
            LocalDateTime maxHoldTime = openTime.plusHours(MAX_HOLDING_TIME_HOURS);
            double openPrice = signal.getOpenPrice();
            String symbol = signal.getSymbol();
            String direction = signal.getDirection();
            
            boolean isExited = false;
            LocalDateTime exitTime = null;
            double exitPrice = 0;
            int currentTimeStep = 0;
            
            // 创建回测用的持仓轨迹
            PositionTrajectory trajectory = new PositionTrajectory(
                    "backtest_" + signal.getSignalId(),
                    openTime,
                    openPrice,
                    symbol,
                    direction
            );
            
            // 计算每个时间点的收益率并决定是否平仓
            for (int i = startIndex; i < allKlineData.size(); i++) {
                KlineData currentKline = allKlineData.get(i);
                
                // 检查是否超过最大持仓时间
                if (currentKline.getTimestamp().isAfter(maxHoldTime)) {
                    // 强制平仓
                    exitTime = currentKline.getTimestamp();
                    exitPrice = currentKline.getClose();
                    isExited = true;
                    System.out.println("交易 " + signal.getSignalId() + " 达到最大持仓时间，强制平仓");
                    break;
                }
                
                // 计算时间步长
                Duration duration = Duration.between(openTime, currentKline.getTimestamp());
                int minutesSinceOpen = (int) duration.toMinutes();
                
                // 只在指定的时间间隔点检查是否平仓
                if (minutesSinceOpen % TIME_INTERVAL_MINUTES == 0 || i == startIndex) {
                    double returnRate;

                    if ("多头".equals(direction)) {
                        returnRate = (currentKline.getClose() - openPrice) * lots *100;
                    } else {
                        returnRate = (openPrice - currentKline.getClose() ) * lots *100;
                    }
                    
                    // 限制收益率在设定范围内
                    returnRate = Math.max(RETURN_MIN_PERCENT, 
                                         Math.min(RETURN_MAX_PERCENT, returnRate));
                    
                    // 添加到轨迹
                    trajectory.addReturnPoint(currentTimeStep, currentKline.getTimestamp(), returnRate);
                    
                    // 判断是否应该平仓
                    boolean shouldExit;
                    if (valueFunctionEstimator != null) {
                        // 开仓初期保护机制：持仓时间在保护期内，采取更保守的策略
                        // 不轻易平仓，除非有明显的盈利或亏损
                        if (minutesSinceOpen <= protectionPeriodMinutes) {
                            // 在保护期内，只有当收益率绝对值大于设定阈值时才考虑平仓
                            if (Math.abs(returnRate) > protectionReturnThreshold) {
                                // 即使收益率绝对值大于0.1，仍要比较是否大于价值函数，避免过早平仓
                                double valueFunction = valueFunctionEstimator.getValueFunction(currentTimeStep, returnRate);
                                shouldExit = returnRate > valueFunction;
                            } else {
                                // 收益率绝对值小于等于设定阈值时，在保护期内不平仓
                                shouldExit = false;
                                System.out.println("交易 " + signal.getSignalId() + " 在持仓时间 " + minutesSinceOpen + 
                                                  " 分钟(开仓保护期内)，继续持有，当前收益率(" + returnRate + ")");
                            }
                        } else {
                            // 使用学习到的价值函数进行平仓决策
                            double valueFunction = valueFunctionEstimator.getValueFunction(currentTimeStep, returnRate);
                            shouldExit = returnRate > valueFunction;
                             
                            if (shouldExit) {
                                System.out.println("交易 " + signal.getSignalId() + " 在时间步 " + currentTimeStep + 
                                                  " 平仓，当前收益率(" + returnRate + ") > 继续持有期望收益(" + valueFunction + ")");
                            } else {
                                System.out.println("交易 " + signal.getSignalId() + " 在时间步 " + currentTimeStep + 
                                                  " 继续持有，当前收益率(" + returnRate + ") <= 继续持有期望收益(" + valueFunction + ")");
                            }
                        }
                    } else {
                        // 使用默认的平仓策略
                        shouldExit = dynamicExitStrategy.shouldExitPosition(currentTimeStep, returnRate);
                    }
                    
                    if (shouldExit) {
                        exitTime = currentKline.getTimestamp();
                        exitPrice = currentKline.getClose();
                        isExited = true;
                        break;
                    }
                    
                    currentTimeStep++;
                }
            }
            
            // 如果尚未平仓，使用最后一根K线平仓
            if (!isExited && startIndex < allKlineData.size()) {
                KlineData lastKline = allKlineData.get(allKlineData.size() - 1);
                exitTime = lastKline.getTimestamp();
                exitPrice = lastKline.getClose();
            }
            
            // 计算最终收益率
            double finalReturnRate;
            if ("多头".equals(direction)) {
                finalReturnRate = (exitPrice - openPrice - 1) * lots *100;
            } else {
                finalReturnRate = (openPrice - exitPrice - 1) * lots *100;
            }
            
            // 设置轨迹的最终信息
            trajectory.setFinalReturnRate(finalReturnRate);
            trajectory.setCloseTime(exitTime);
            
            // 创建交易记录
            TradeRecord tradeRecord = new TradeRecord();
            // 设置所有必要的字段
            tradeRecord.setTradeId(signal.getSignalId().replace("signal_", ""));
            tradeRecord.setSymbol(signal.getSymbol());
            tradeRecord.setDirection(direction);
            tradeRecord.setOpenTime(signal.getTimestamp());
            tradeRecord.setOpenPrice(signal.getOpenPrice());
            tradeRecord.setCloseTime(exitTime);
            tradeRecord.setClosePrice(exitPrice);
            // 计算持仓时间（分钟）
            long holdingTimeMinutes = Duration.between(signal.getTimestamp(), exitTime).toMinutes();
            tradeRecord.setHoldingTimeMinutes(holdingTimeMinutes);
            // 假设profit是基于收益率和一些默认手数计算的
            //double defaultLots = 0.1; // 默认手数
            //double profit = finalReturnRate * defaultLots * 1000; // 简单计算获利
            tradeRecord.setReturnRate(finalReturnRate);
            tradeRecord.setLots(lots);
            
            tradeRecords.add(tradeRecord);
            backtestTrajectories.add(trajectory);
            
        } catch (Exception e) {
            System.err.println("模拟交易时出错: " + e.getMessage());
        }
    }
    
    /**
     * 计算回测结果
     * @return 回测结果
     */
    private BacktestResult calculateBacktestResult() {
        if (tradeRecords.isEmpty()) {
            return new BacktestResult();
        }
        
        // 计算各项指标
        int totalTrades = tradeRecords.size();
        int winningTrades = (int) tradeRecords.stream().filter(record -> record.getReturnRate() > 0).count();
        double winRate = (double) winningTrades / totalTrades;
        
        // 计算盈亏比
        double totalProfit = tradeRecords.stream()
                .filter(record -> record.getReturnRate() > 0)
                .mapToDouble(TradeRecord::getReturnRate)
                .sum();
        
        double totalLoss = Math.abs(tradeRecords.stream()
                .filter(record -> record.getReturnRate() < 0)
                .mapToDouble(TradeRecord::getReturnRate)
                .sum());
        
        double profitLossRatio = totalLoss == 0 ? 0 : totalProfit / totalLoss;
        
        // 计算总收益率
        double totalReturnRate = tradeRecords.stream().mapToDouble(TradeRecord::getReturnRate).sum() / totalTrades;
        
        // 计算平均持仓时间
        double avgHoldingTime = tradeRecords.stream().mapToDouble(TradeRecord::getHoldingTimeMinutes).average().orElse(0);
        
        // 计算最大回撤（简化版）
        double maxDrawdown = 0;
        double peakEquity = 0;
        double currentEquity = 0;
        
        for (TradeRecord record : tradeRecords) {
            currentEquity += record.getReturnRate();
            peakEquity = Math.max(peakEquity, currentEquity);
            maxDrawdown = Math.max(maxDrawdown, (peakEquity - currentEquity) / (peakEquity + 1e-9));
        }
        
        // 计算夏普比率（简化版，使用无风险收益率为0）
        double returnsMean = totalReturnRate;
        double returnsStdDev = calculateStandardDeviation(
                tradeRecords.stream().mapToDouble(TradeRecord::getReturnRate).toArray());
        double sharpeRatio = returnsStdDev == 0 ? 0 : returnsMean / returnsStdDev;
        
        return new BacktestResult(
                totalTrades,
                winRate,
                profitLossRatio,
                totalReturnRate,
                avgHoldingTime,
                maxDrawdown,
                sharpeRatio,
                tradeRecords
        );
    }
    
    /**
     * 计算标准差
     * @param values 数值数组
     * @return 标准差
     */
    private double calculateStandardDeviation(double[] values) {
        if (values.length <= 1) {
            return 0;
        }
        
        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 回测结果类
     */
    public static class BacktestResult {
        private int totalTrades;
        private double winRate;
        private double profitLossRatio;
        private double totalReturnRate;
        private double avgHoldingTimeMinutes;
        private double maxDrawdown;
        private double sharpeRatio;
        private List<TradeRecord> tradeRecords;
        
        public BacktestResult() {
            this(0, 0, 0, 0, 0, 0, 0, new ArrayList<>());
        }
        
        public BacktestResult(int totalTrades, double winRate, double profitLossRatio, 
                             double totalReturnRate, double avgHoldingTimeMinutes, 
                             double maxDrawdown, double sharpeRatio, 
                             List<TradeRecord> tradeRecords) {
            this.totalTrades = totalTrades;
            this.winRate = winRate;
            this.profitLossRatio = profitLossRatio;
            this.totalReturnRate = totalReturnRate;
            this.avgHoldingTimeMinutes = avgHoldingTimeMinutes;
            this.maxDrawdown = maxDrawdown;
            this.sharpeRatio = sharpeRatio;
            this.tradeRecords = tradeRecords;
        }
        
        // Getters
        public int getTotalTrades() { return totalTrades; }
        public double getWinRate() { return winRate; }
        public double getProfitLossRatio() { return profitLossRatio; }
        public double getTotalReturnRate() { return totalReturnRate; }
        public double getAvgHoldingTimeMinutes() { return avgHoldingTimeMinutes; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public double getSharpeRatio() { return sharpeRatio; }
        public List<TradeRecord> getTradeRecords() { return new ArrayList<>(tradeRecords); }
        
        @Override
        public String toString() {
            return "BacktestResult{" +
                    "totalTrades=" + totalTrades +
                    ", winRate=" + String.format("%.2f%%", winRate * 100) +
                    ", profitLossRatio=" + String.format("%.2f", profitLossRatio) +
                    ", totalReturnRate=" + String.format("%.2f%%", totalReturnRate) +
                    ", avgHoldingTimeMinutes=" + String.format("%.2f", avgHoldingTimeMinutes) +
                    ", maxDrawdown=" + String.format("%.2f%%", maxDrawdown * 100) +
                    ", sharpeRatio=" + String.format("%.2f", sharpeRatio) +
                    '}';
        }
    }
    
    /**
     * 交易记录类
     */
    @NoArgsConstructor
    @Setter
    public static class TradeRecord {
        private String tradeId;
        private String symbol;
        private String direction;
        private LocalDateTime openTime;
        private double openPrice;
        private LocalDateTime closeTime;
        private double closePrice;
        private double returnRate;
        private long holdingTimeMinutes;
        private double lots;
        
        public TradeRecord(String tradeId, String symbol, String direction, 
                          LocalDateTime openTime, double openPrice, 
                          LocalDateTime closeTime, double closePrice, 
                          double returnRate, long holdingTimeMinutes,double lots) {
            this.tradeId = tradeId;
            this.symbol = symbol;
            this.direction = direction;
            this.openTime = openTime;
            this.openPrice = openPrice;
            this.closeTime = closeTime;
            this.closePrice = closePrice;
            this.returnRate = returnRate;
            this.holdingTimeMinutes = holdingTimeMinutes;
            this.lots = lots;
        }
        
        // Getters
        public String getTradeId() { return tradeId; }
        public String getSymbol() { return symbol; }
        public String getDirection() { return direction; }
        public LocalDateTime getOpenTime() { return openTime; }
        public double getOpenPrice() { return openPrice; }
        public LocalDateTime getCloseTime() { return closeTime; }
        public double getClosePrice() { return closePrice; }
        public double getReturnRate() { return returnRate; }
        public long getHoldingTimeMinutes() { return holdingTimeMinutes; }
        public double getLots() { return lots; }
        
        @Override
        public String toString() {
            return "TradeRecord{" +
                    "tradeId='" + tradeId + '\'' +
                    ", direction='" + direction + '\'' +
                    ", returnRate=" + String.format("%.2f%%", returnRate) +
                    ", holdingTime=" + holdingTimeMinutes + "分钟" +
                    '}';
        }
    }
    
    // Getters
    public List<TradeRecord> getTradeRecords() {
        return new ArrayList<>(tradeRecords);
    }
    
    public List<PositionTrajectory> getBacktestTrajectories() {
        return new ArrayList<>(backtestTrajectories);
    }
}