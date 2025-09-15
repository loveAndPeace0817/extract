package com.demo.extract.shortTimeMar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 持仓轨迹类，记录从开仓到平仓的完整轨迹
 */
public class PositionTrajectory {
    private String positionId; // 持仓ID
    private LocalDateTime openTime; // 开仓时间
    private double openPrice; // 开仓价格
    private String symbol; // 交易品种
    private String direction; // 方向：多头或空头
    private List<ReturnPoint> returnPoints; // 收益率序列
    private double finalReturnRate; // 最终收益率
    private LocalDateTime closeTime; // 平仓时间
    
    /**
     * 收益率点类
     */
    public static class ReturnPoint {
        private int timeStep; // 时间步长
        private LocalDateTime timestamp; // 时间戳
        private double returnRate; // 收益率百分比
        private TradingState state; // 对应的交易状态
        
        public ReturnPoint(int timeStep, LocalDateTime timestamp, double returnRate) {
            this.timeStep = timeStep;
            this.timestamp = timestamp;
            this.returnRate = returnRate;
        }
        
        // Getters and setters
        public int getTimeStep() {
            return timeStep;
        }
        
        public void setTimeStep(int timeStep) {
            this.timeStep = timeStep;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public double getReturnRate() {
            return returnRate;
        }
        
        public void setReturnRate(double returnRate) {
            this.returnRate = returnRate;
        }
        
        public TradingState getState() {
            return state;
        }
        
        public void setState(TradingState state) {
            this.state = state;
        }
    }
    
    /**
     * 构造函数
     */
    public PositionTrajectory(String positionId, LocalDateTime openTime, double openPrice, String symbol, String direction) {
        this.positionId = positionId;
        this.openTime = openTime;
        this.openPrice = openPrice;
        this.symbol = symbol;
        this.direction = direction;
        this.returnPoints = new ArrayList<>();
    }
    
    // 添加收益率点
    public void addReturnPoint(int timeStep, LocalDateTime timestamp, double returnRate) {
        ReturnPoint point = new ReturnPoint(timeStep, timestamp, returnRate);
        this.returnPoints.add(point);
    }
    
    // 获取轨迹中指定时间步长的收益率点
    public ReturnPoint getReturnPoint(int timeStep) {
        for (ReturnPoint point : returnPoints) {
            if (point.getTimeStep() == timeStep) {
                return point;
            }
        }
        return null;
    }
    
    // Getters and setters
    public String getPositionId() {
        return positionId;
    }
    
    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }
    
    public LocalDateTime getOpenTime() {
        return openTime;
    }
    
    public void setOpenTime(LocalDateTime openTime) {
        this.openTime = openTime;
    }
    
    public double getOpenPrice() {
        return openPrice;
    }
    
    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public List<ReturnPoint> getReturnPoints() {
        return returnPoints;
    }
    
    public void setReturnPoints(List<ReturnPoint> returnPoints) {
        this.returnPoints = returnPoints;
    }
    
    public double getFinalReturnRate() {
        return finalReturnRate;
    }
    
    public void setFinalReturnRate(double finalReturnRate) {
        this.finalReturnRate = finalReturnRate;
    }
    
    public LocalDateTime getCloseTime() {
        return closeTime;
    }
    
    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }
    
    @Override
    public String toString() {
        return "PositionTrajectory{" +
                "positionId='" + positionId + '\'' +
                ", openTime=" + openTime +
                ", openPrice=" + openPrice +
                ", symbol='" + symbol + '\'' +
                ", direction='" + direction + '\'' +
                ", returnPointsCount=" + returnPoints.size() +
                ", finalReturnRate=" + finalReturnRate +
                ", closeTime=" + closeTime +
                '}';
    }
}