package com.demo.extract.services;

import com.demo.extract.model.KlineData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * K线数据加载器类，用于从CSV文件加载OHLCV格式的K线数据
 * 仿照DataLoaderNew的设计模式实现
 */
public class KlineDataLoader {
    private static final String[] EXPECTED_HEADERS = {
            "datetime", "open", "high", "low", "close", "volume"
    };

    /**
     * 从CSV文件加载K线数据
     * @param filePath CSV文件路径
     * @param symbol 交易对或股票代码
     * @return K线数据列表
     * @throws IOException 如果文件不存在或读取失败
     */
    public List<KlineData> loadFromCsv(String filePath, String symbol) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 自动处理BOM和编码（UTF-8优先，失败时尝试GBK）
        try {
            return tryParseWithEncoding(path, StandardCharsets.UTF_8, symbol);
        } catch (IOException e1) {
            try {
                return tryParseWithEncoding(path, Charset.forName("GBK"), symbol);
            } catch (IOException e2) {
                throw new IOException("无法用UTF-8或GBK编码解析文件", e2);
            }
        }
    }

    /**
     * 尝试用指定编码解析CSV文件
     * @param path 文件路径
     * @param charset 字符编码
     * @param symbol 交易对或股票代码
     * @return K线数据列表
     * @throws IOException 如果解析失败
     */
    private List<KlineData> tryParseWithEncoding(Path path, Charset charset, String symbol) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, charset);
             CSVParser parser = new CSVParser(reader, buildCsvFormat())) {

            validateHeaders(parser);
            return convertToKlineData(parser, symbol);
        }
    }

    /**
     * 构建CSV解析格式
     * @return CSVFormat对象
     */
    private CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .withIgnoreEmptyLines();
    }

    /**
     * 验证CSV文件的表头是否包含所有必要的列
     * @param parser CSV解析器
     * @throws IllegalArgumentException 如果缺少必要的列
     */
    private void validateHeaders(CSVParser parser) {
        Set<String> actualHeaders = parser.getHeaderMap().keySet().stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> missingHeaders = Arrays.stream(EXPECTED_HEADERS)
                .filter(h -> !actualHeaders.contains(h.trim()))
                .collect(Collectors.toSet());

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException(
                    "K线CSV缺少必要列！缺失: " + missingHeaders + " (实际表头: " + actualHeaders + ")");
        }
    }

    /**
     * 将CSV记录转换为KlineData对象列表
     * @param parser CSV解析器
     * @param symbol 交易对或股票代码
     * @return KlineData对象列表
     */
    private List<KlineData> convertToKlineData(CSVParser parser, String symbol) {
        List<KlineData> result = new ArrayList<>();
        int lineNumber = 1; // 从1开始计数（表头行）

        for (CSVRecord record : parser) {
            lineNumber++;
            try {
                // 检查必要字段是否为空
                if (StringUtils.isEmpty(record.get("datetime")) ||
                        StringUtils.isEmpty(record.get("open")) ||
                        StringUtils.isEmpty(record.get("high")) ||
                        StringUtils.isEmpty(record.get("low")) ||
                        StringUtils.isEmpty(record.get("close")) ||
                        StringUtils.isEmpty(record.get("volume"))) {
                    System.err.println("警告: 第" + lineNumber + "行缺少必要字段，跳过该行");
                    continue;
                }

                // 解析字段值
                String dateTimeStr = record.get("datetime");
                double open = Double.parseDouble(record.get("open"));
                double high = Double.parseDouble(record.get("high"));
                double low = Double.parseDouble(record.get("low"));
                double close = Double.parseDouble(record.get("close"));
                long volume = Long.parseLong(record.get("volume"));

                // 创建KlineData对象
                KlineData klineData = KlineData.fromDateTimeString(
                        symbol, dateTimeStr, open, high, low, close, volume);
                result.add(klineData);

            } catch (NumberFormatException e) {
                System.err.println("警告: 第" + lineNumber + "行数值格式错误: " + e.getMessage() + "，跳过该行");
            } catch (DateTimeParseException e) {
                System.err.println("警告: 第" + lineNumber + "行日期时间格式错误: " + e.getMessage() + "，跳过该行");
            }
        }

        return result;
    }

    /**
     * 获取K线数据的简单统计信息
     * @param klines K线数据列表
     * @return 包含统计信息的字符串
     */
    public String getKlineStatistics(List<KlineData> klines) {
        if (klines == null || klines.isEmpty()) {
            return "没有可用的K线数据";
        }

        double totalChange = 0;
        double totalVolume = 0;
        double maxHigh = Double.MIN_VALUE;
        double minLow = Double.MAX_VALUE;

        for (KlineData kline : klines) {
            totalChange += kline.getPriceChangePercent();
            totalVolume += kline.getVolume();
            maxHigh = Math.max(maxHigh, kline.getHigh());
            minLow = Math.min(minLow, kline.getLow());
        }

        double avgChange = totalChange / klines.size();
        double avgVolume = totalVolume / klines.size();

        StringBuilder stats = new StringBuilder();
        stats.append("K线数据统计信息:\n");
        stats.append("- 数据总数: ").append(klines.size()).append("条\n");
        stats.append("- 平均涨跌幅: ").append(String.format("%.2f", avgChange)).append("%\n");
        stats.append("- 平均成交量: ").append(String.format("%.0f", avgVolume)).append("\n");
        stats.append("- 最高价格: ").append(String.format("%.2f", maxHigh)).append("\n");
        stats.append("- 最低价格: ").append(String.format("%.2f", minLow)).append("\n");
        stats.append("- 价格范围: ").append(String.format("%.2f", maxHigh - minLow)).append("\n");
        stats.append("- 起始时间: ").append(klines.get(0).getTimestamp()).append("\n");
        stats.append("- 结束时间: ").append(klines.get(klines.size() - 1).getTimestamp()).append("\n");

        return stats.toString();
    }
}