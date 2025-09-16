package com.demo.extract.services;



import com.demo.extract.DTO.OrderTimeSeries;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DataLoaderNew {
    private static final String[] EXPECTED_HEADERS = {
            "profit", "订单号", "holdtime", "日期",
            "close","进场价格", "atr", "open", "DonchianHigh", "DonchianLow"
    };

    public List<OrderTimeSeries> loadFromCsv(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 自动处理BOM和编码（UTF-8优先，失败时尝试GBK）
        try {
            return tryParseWithEncoding(path, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            try {
                return tryParseWithEncoding(path, Charset.forName("GBK"));
            } catch (IOException e2) {
                throw new IOException("无法用UTF-8或GBK编码解析文件", e2);
            }
        }
    }

    private List<OrderTimeSeries> tryParseWithEncoding(Path path, Charset charset) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, charset);
             CSVParser parser = new CSVParser(reader, buildCsvFormat())) {

            validateHeaders(parser);
            Map<String, List<CSVRecord>> recordsByOrder = groupAndSortRecords(parser);
            return convertToTimeSeries(recordsByOrder);
        }
    }

    private CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .withIgnoreEmptyLines();
    }

    private void validateHeaders(CSVParser parser) {
        Set<String> actualHeaders = parser.getHeaderMap().keySet().stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> missingHeaders = Arrays.stream(EXPECTED_HEADERS)
                .filter(h -> !actualHeaders.contains(h.trim()))
                .collect(Collectors.toSet());

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV缺少必要列！缺失: " + missingHeaders + " (实际表头: " + actualHeaders + ")");
        }
    }

    private Map<String, List<CSVRecord>> groupAndSortRecords(CSVParser parser) {
        Map<String, List<CSVRecord>> recordsByOrder = new LinkedHashMap<>();

        for (CSVRecord record : parser) {
            String orderId = record.get("订单号");
            if (StringUtils.isEmpty(orderId)) {
                continue;
            }

            recordsByOrder.computeIfAbsent(orderId, k -> new ArrayList<>())
                    .add(record);
        }

        // 按持仓时间排序
        recordsByOrder.values().forEach(list ->
                list.sort(Comparator.comparingDouble(
                        r -> Double.parseDouble(r.get("holdtime")))));

        return recordsByOrder;
    }

    private List<OrderTimeSeries> convertToTimeSeries(
            Map<String, List<CSVRecord>> recordsByOrder) {

        List<OrderTimeSeries> result = new ArrayList<>();
        for (Map.Entry<String, List<CSVRecord>> entry : recordsByOrder.entrySet()) {
            String orderId = entry.getKey();
            List<CSVRecord> records = entry.getValue();

            double[] timestamps = new double[records.size()];
            double[] values = new double[records.size()];
            double[] close = new double[records.size()];
            double[] open = new double[records.size()];
            double[] atr = new double[records.size()];
            double[] TH = new double[records.size()];
            double[] TL = new double[records.size()];
            double[] inPrice = new double[records.size()];
            String[] orderTime = new String[records.size()];
            for (int i = 0; i < records.size(); i++) {
                CSVRecord record = records.get(i);

                if (!StringUtils.isEmpty(record.get("holdtime")) && !StringUtils.isEmpty(record.get("atr"))   && !StringUtils.isEmpty(record.get("DonchianHigh"))) {
                    timestamps[i] = Double.parseDouble(record.get("holdtime"));
                    values[i] = Double.parseDouble(record.get("profit"));
                    close[i] = Double.parseDouble(record.get("close"));
                    open[i] = Double.parseDouble(record.get("open"));
                    atr[i] = Double.parseDouble(record.get("atr"));
                    TH[i] = Double.parseDouble(record.get("DonchianHigh"));
                    TL[i] = Double.parseDouble(record.get("DonchianLow"));
                    inPrice[i] = Double.parseDouble(record.get("进场价格"));
                    orderTime[i] = record.get("日期");
                }

            }

            result.add(new OrderTimeSeries(orderId, timestamps, values,close,open,atr,TH,TL,orderTime,inPrice));
        }
        return result;
    }
}
