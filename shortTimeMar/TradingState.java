package com.demo.extract.shortTimeMar;

/**
 * 表示交易状态的类
 * 状态由持仓时间和当前收益率组成
 */
public class TradingState {
    private int timeInterval; // 持仓时间区间，0表示初始状态
    private double returnRate; // 当前收益率百分比
    private String timeIntervalLabel; // 时间区间标签
    private String returnRateLabel; // 收益率区间标签
    
    /**
     * 构造函数
     * @param timeInterval 持仓时间区间
     * @param returnRate 当前收益率百分比
     */
    public TradingState(int timeInterval, double returnRate) {
        this.timeInterval = timeInterval;
        this.returnRate = returnRate;
    }
    
    /**
     * 构造函数（包含区间标签）
     * @param timeInterval 持仓时间区间
     * @param returnRate 当前收益率百分比
     * @param timeIntervalLabel 时间区间标签
     * @param returnRateLabel 收益率区间标签
     */
    public TradingState(int timeInterval, double returnRate, String timeIntervalLabel, String returnRateLabel) {
        this.timeInterval = timeInterval;
        this.returnRate = returnRate;
        this.timeIntervalLabel = timeIntervalLabel;
        this.returnRateLabel = returnRateLabel;
    }
    
    // Getters and setters
    public int getTimeInterval() {
        return timeInterval;
    }
    
    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }
    
    public double getReturnRate() {
        return returnRate;
    }
    
    public void setReturnRate(double returnRate) {
        this.returnRate = returnRate;
    }
    
    public String getTimeIntervalLabel() {
        return timeIntervalLabel;
    }
    
    public void setTimeIntervalLabel(String timeIntervalLabel) {
        this.timeIntervalLabel = timeIntervalLabel;
    }
    
    public String getReturnRateLabel() {
        return returnRateLabel;
    }
    
    public void setReturnRateLabel(String returnRateLabel) {
        this.returnRateLabel = returnRateLabel;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TradingState that = (TradingState) o;
        return timeInterval == that.timeInterval && Double.compare(that.returnRate, returnRate) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + timeInterval;
        result = 31 * result + Double.hashCode(returnRate);
        return result;
    }
    
    @Override
    public String toString() {
        return "TradingState{" +
                "timeInterval=" + timeInterval +
                ", returnRate=" + returnRate +
                ", timeIntervalLabel='" + timeIntervalLabel + '\'' +
                ", returnRateLabel='" + returnRateLabel + '\'' +
                '}';
    }
}