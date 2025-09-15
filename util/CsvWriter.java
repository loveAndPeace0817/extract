package com.demo.extract.util;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.TradeRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;


@Service
public class CsvWriter {

    /**
     * 写入CSV文件
     * @param filePath 文件路径（例如："./trades.csv"）
     * @param currencyPairs 货币对列表（如：["EUR/USD", "USD/JPY"]）
     * @param operations 操作列表（如：["BUY", "SELL"]）
     * @param ids ID列表（如：[1001, 1002]）
     * @throws IOException 如果文件写入失败
     *
     *
     */

    private static final Map<String, Map<String,Double> > dataMap = new ConcurrentHashMap<>();

    public static void writeToCsv(
            String filePath,
            List<String> currencyPairs,
            List<String> operations,
            List<String> ids
    ) throws IOException {
        // 检查参数数量是否一致
        if (currencyPairs.size() != operations.size() || operations.size() != ids.size()) {
            throw new IllegalArgumentException("参数列表长度不一致");
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            // 1. 写入标题行
            writer.append("时间,操作,ID\n");

            // 2. 写入数据行
            for (int i = 0; i < currencyPairs.size(); i++) {
                writer.append(currencyPairs.get(i))
                        .append(",")
                        .append(operations.get(i))
                        .append(",")
                        .append(ids.get(i))
                        .append("\n");
            }
        }
    }


    public static void WrierToMap(String targetId, Map<String,Double> similarOrderMap){
        dataMap.put(targetId,similarOrderMap);
    }

    public static void MNNWrier( String filePath,Map<String, OrderTimeSeries> dtMap)throws IOException{



        try (FileWriter writer = new FileWriter(filePath)) {
            // 1. 写入标题行
            writer.append("目标订单id,对比订单id,距离,目标订单t1,目标订单t2,对比订单t1,对比订单t2\n");
            for (String key:dataMap.keySet()){
                Map<String, Double> stringDoubleMap = dataMap.get(key);
                for (String key1:stringDoubleMap.keySet()){
                    String targetOrderT1 ="";
                    String targetOrderT2 ="";
                    String simOrderT1 ="";
                    String simOrderT2 ="";
                    double[] targrtValues = dtMap.get(key).getValues();
                    double[] simtValues = dtMap.get(key1).getValues();
                    if(targrtValues.length>57){
                        targetOrderT1 = targrtValues[56]+"";
                    }else {
                        targetOrderT1 = "0";
                    }
                    if(targrtValues.length>70){
                        targetOrderT2 = targrtValues[targrtValues.length-1]+"";
                    }else {
                        targetOrderT2 = "0";
                    }
                    if(simtValues.length>57){
                        simOrderT1 = simtValues[56]+"";
                    }else {
                        simOrderT1 = "0";
                    }
                    if(simtValues.length>70){
                        simOrderT2 = simtValues[simtValues.length-1]+"";
                    }else {
                        simOrderT2 = "0";
                    }
                    if(!targetOrderT1.equals("0")){
                        writer.append(key).append(",")
                                .append(key1).append(",")
                                .append(stringDoubleMap.get(key1).toString()).append(",")
                                .append(targetOrderT1).append(",")
                                .append(targetOrderT2).append(",")
                                .append(simOrderT1).append(",")
                                .append(simOrderT2).append(",")
                                .append("\n");
                    }

                }


            }

        }
    }


    // 从Excel文件读取交易记录
    public static List<TradeRecord> readRecordsFromExcel(String filePath) throws IOException {
        List<TradeRecord> records = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表

            // 从第二行开始读取(跳过表头)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                if (row.getCell(6) != null && row.getCell(6).getCellType() != CellType.BLANK) {
                    int id = (int) row.getCell(0).getNumericCellValue();
                    String time = row.getCell(1).getStringCellValue();
                    String type = row.getCell(2).getStringCellValue();
                    int orderId = (int) row.getCell(3).getNumericCellValue();
                    double lots = row.getCell(4).getNumericCellValue();
                    double price = row.getCell(5).getNumericCellValue();
                    double profit = row.getCell(6).getNumericCellValue();
                    records.add(new TradeRecord(id, time, type, orderId, lots, price, profit));
                }



            }
        }
        return records;
    }

    // 将记录写入Excel文件
    public static void writeRecordsToExcel(List<TradeRecord> records, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("交易记录");

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "时间", "类型", "订单", "手数", "价格", "获利"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 写入数据行
            for (int i = 0; i < records.size(); i++) {
                Row row = sheet.createRow(i + 1);
                TradeRecord record = records.get(i);

                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(record.getTime());
                row.createCell(2).setCellValue(record.getType());
                row.createCell(3).setCellValue(record.getOrderId());
                row.createCell(4).setCellValue(record.getLots());
                row.createCell(5).setCellValue(record.getPrice());
                row.createCell(6).setCellValue(record.getProfit());
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }
}
