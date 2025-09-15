package com.demo.extract.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

/**
 * K线数据类，用于存储OHLCV数据
 * OHLCV: Open, High, Low, Close, Volume
 */
public class KlineData {
    private final String symbol;  // 交易对或股票代码
    private final LocalDateTime timestamp;  // 时间戳
    private final double open;  // 开盘价
    private final double high;  // 最高价
    private final double low;   // 最低价
    private final double close; // 收盘价
    private final long volume;  // 成交量

    public KlineData(String symbol, LocalDateTime timestamp, double open, double high, double low, double close, long volume) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // 从字符串日期时间创建KlineData的静态工厂方法
    public static KlineData fromDateTimeString(String symbol, String dateTimeStr, double open, double high, double low, double close, long volume) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime timestamp = LocalDateTime.parse(dateTimeStr, formatter);
        return new KlineData(symbol, timestamp, open, high, low, close, volume);
    }

    // 获取K线数据的主要统计信息
    public double getPriceChange() {
        return close - open;
    }

    public double getPriceChangePercent() {
        return open == 0 ? 0 : ((close - open) / open) * 100;
    }

    public double getRange() {
        return high - low;
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KlineData klineData = (KlineData) o;
        return Double.compare(open, klineData.open) == 0 &&
                Double.compare(high, klineData.high) == 0 &&
                Double.compare(low, klineData.low) == 0 &&
                Double.compare(close, klineData.close) == 0 &&
                volume == klineData.volume &&
                Objects.equals(symbol, klineData.symbol) &&
                Objects.equals(timestamp, klineData.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, timestamp, open, high, low, close, volume);
    }

    @Override
    public String toString() {
        return "KlineData{" +
                "symbol='" + symbol + '\'' +
                ", timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}