package com.demo.extract.zzq;

import com.demo.extract.zzq.dto.zzqdto;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用于解析特定格式CSV文件并转换为zzqdto对象的加载器
 */
public class ZZQDataLoader {
    // 期望的CSV表头
    private static final String[] EXPECTED_HEADERS = {
            "日期", "品种", "预测下一个值", "预测涨跌", "实际下一个值", "实际涨跌"
    };

    /**
     * 从CSV文件加载数据
     * @param filePath CSV文件路径
     * @return zzqdto对象列表
     * @throws IOException 如果文件不存在或读取失败
     */
    public List<zzqdto> loadFromCsv(String filePath) throws IOException {
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

    /**
     * 使用指定编码尝试解析CSV文件
     */
    private List<zzqdto> tryParseWithEncoding(Path path, Charset charset) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, charset);
             CSVParser parser = new CSVParser(reader, buildCsvFormat())) {

            validateHeaders(parser);
            return convertToZZQDtoList(parser);
        }
    }

    /**
     * 构建CSV解析格式
     */
    private CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .withIgnoreEmptyLines();
    }

    /**
     * 验证CSV表头是否符合预期
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
                    "CSV缺少必要列！缺失: " + missingHeaders + " (实际表头: " + actualHeaders + ")");
        }
    }

    /**
     * 将CSV记录转换为zzqdto对象列表
     */
    private List<zzqdto> convertToZZQDtoList(CSVParser parser) {
        List<zzqdto> result = new ArrayList<>();

        for (CSVRecord record : parser) {
            zzqdto dto = new zzqdto();

            // 日期
            dto.setDate(record.get("日期"));
            
            // 品种
            dto.setType(record.get("品种"));
            
            // 预测下一个值
            if (!StringUtils.isEmpty(record.get("预测下一个值"))) {
                try {
                    dto.setValue(Double.parseDouble(record.get("预测下一个值")));
                } catch (NumberFormatException e) {
                    // 如果解析失败，保留为null
                }
            }
            
            // 预测涨跌
            dto.setFluctuation(record.get("预测涨跌"));
            
            // 实际下一个值
            if (!StringUtils.isEmpty(record.get("实际下一个值"))) {
                try {
                    dto.setTrueValue(Double.parseDouble(record.get("实际下一个值")));
                } catch (NumberFormatException e) {
                    // 如果解析失败，保留为null
                }
            }
            
            // 实际涨跌
            if (!StringUtils.isEmpty(record.get("实际涨跌"))) {
                try {
                    dto.setTruefluctuation(record.get("实际涨跌"));
                } catch (NumberFormatException e) {
                    // 如果解析失败，保留为null
                }
            }

            result.add(dto);
        }

        return result;
    }
}