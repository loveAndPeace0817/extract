package com.demo.extract.services;


import com.demo.extract.DTO.OrderTimeSeries;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 健壮的CSV数据加载器（修复BOM和编码问题）
 */
public class DataLoader {
    private static final String[] EXPECTED_HEADERS = {"收益", "订单号", "持仓时间","日期", "close", "atr","open","DonchianHigh","DonchianLow"};
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * 加载CSV文件（自动处理BOM和编码）
     */
    public List<OrderTimeSeries> loadFromCsv(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        // 1. 读取文件并移除BOM
        byte[] fileBytes = Files.readAllBytes(path);
        if (startsWithBom(fileBytes)) {
            fileBytes = Arrays.copyOfRange(fileBytes, UTF8_BOM.length, fileBytes.length);
        }

        // 2. 使用StringReader避免二次编码问题
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, buildCsvFormat())) {

            // 3. 严格验证表头
            validateHeaders(parser);

            // 4. 按订单号分组并排序
            Map<String, List<CSVRecord>> recordsByOrder = groupAndSortRecords(parser);

            // 5. 转换为时间序列对象
            return convertToTimeSeries(recordsByOrder);
        }
    }




    // -----------------------------------
    // 私有工具方法
    // -----------------------------------

    private CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()  // 忽略大小写
                .withTrim()              // 自动去除前后空格
                .withIgnoreEmptyLines();
    }

    private boolean startsWithBom(byte[] bytes) {
        if (bytes.length < UTF8_BOM.length) {
            return false;
        }
        return bytes[0] == UTF8_BOM[0] &&
                bytes[1] == UTF8_BOM[1] &&
                bytes[2] == UTF8_BOM[2];
    }

    private void validateHeaders(CSVParser parser) {
        Map<String, Integer> headers = parser.getHeaderMap();

        // 调试输出原始表头信息
        System.out.println("[DEBUG] 实际表头(Hex):");
        headers.keySet().forEach(k ->
                System.out.printf("  '%s' → %s%n", k, toHexString(k)));

        // 标准化比较（移除BOM/空格/大小写）
        Set<String> normalizedHeaders = headers.keySet().stream()
                .map(this::normalizeHeader)
                .collect(Collectors.toSet());

        for (String expected : EXPECTED_HEADERS) {
            String normalizedExpected = normalizeHeader(expected);
            if (!normalizedHeaders.contains(normalizedExpected)) {
                throw new IllegalArgumentException(
                        String.format("CSV缺少必要列！期望: '%s' (实际: %s)",
                                expected, headers.keySet()));
            }
        }
    }

    private String normalizeHeader(String header) {
        // 1. 移除BOM和不可见字符
        String cleaned = header.replace("\uFEFF", "")
                .replaceAll("[\\p{Cf}\\p{Cn}]", "");
        // 2. 标准化Unicode字符
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFKC);
        // 3. 统一为小写并去除空格
        return cleaned.trim().toLowerCase();
    }

    private Map<String, List<CSVRecord>> groupAndSortRecords(CSVParser parser) {
        Map<String, List<CSVRecord>> recordsByOrder = new LinkedHashMap<>();

        for (CSVRecord record : parser) {
            String orderId = record.get("订单号");
            recordsByOrder.computeIfAbsent(orderId, k -> new ArrayList<>())
                    .add(record);

        }

        // 按持仓时间排序
        recordsByOrder.values().forEach(list ->
                list.sort(Comparator.comparingDouble(
                        r -> Double.parseDouble(r.get("持仓时间")))));

        return recordsByOrder;
    }

    private List<OrderTimeSeries> convertToTimeSeries(
            Map<String, List<CSVRecord>> recordsByOrder) {

        List<OrderTimeSeries> result = new ArrayList<>();
        for (Map.Entry<String, List<CSVRecord>> entry : recordsByOrder.entrySet()) {
            String orderId = entry.getKey();
            if(orderId.equals("1311")){
                System.out.println(888);
            }
            List<CSVRecord> records = entry.getValue();

            double[] timestamps = new double[records.size()];
            double[] values = new double[records.size()];
            double[] close = new double[records.size()];
            double[] open = new double[records.size()];
            double[] atr = new double[records.size()];
            double[] TH = new double[records.size()];
            double[] TL = new double[records.size()];
            String[] orderTime = new String[records.size()];
            for (int i = 0; i < records.size(); i++) {
                CSVRecord record = records.get(i);

                if (!StringUtils.isEmpty(record.get("持仓时间")) && !StringUtils.isEmpty(record.get("atr"))   && !StringUtils.isEmpty(record.get("DonchianHigh"))) {
                    timestamps[i] = Double.parseDouble(record.get("持仓时间"));
                    values[i] = Double.parseDouble(record.get("收益"));
                    close[i] = Double.parseDouble(record.get("close"));
                    open[i] = Double.parseDouble(record.get("open"));
                    atr[i] = Double.parseDouble(record.get("atr"));
                    TH[i] = Double.parseDouble(record.get("DonchianHigh"));
                    TL[i] = Double.parseDouble(record.get("DonchianLow"));
                    orderTime[i] = record.get("日期");
                }

            }

            result.add(new OrderTimeSeries(orderId, timestamps, values,close,open,atr,TH,TL,orderTime));
        }
        return result;
    }

    private String toHexString(String s) {
        return s.chars()
                .mapToObj(c -> String.format("%04X", c))
                .collect(Collectors.joining(" "));
    }
}