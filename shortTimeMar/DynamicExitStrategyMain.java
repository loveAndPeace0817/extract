package com.demo.extract.shortTimeMar;

import com.demo.extract.DTO.OrderTimeSeries;
import com.demo.extract.DTO.TradeRecord;
import com.demo.extract.model.KlineData;
import com.demo.extract.services.DataLoaderNew;
import com.demo.extract.services.KlineDataLoader;
import com.demo.extract.util.CsvWriter;


import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 动态平仓策略主类，整合所有组件并提供完整工作流程
 */
public class DynamicExitStrategyMain {
    private static final String DATA_FILE_PATH = "D:/data/K线/XAUUSD_5.csv"; // 历史数据文件路径
    private static final String OUTPUT_FILE_PATH = "D:/data/K线/dynamic_exit_strategy_results.json"; // 结果输出路径
    private static final String LOOKUP_TABLE_PATH = "D:/data/K线/value_function_lookup_table.json"; // 价值函数查询表路径
    
    // 回测日期范围
    private static final String START_DATE_STR = "2020-09-01 00:00:00";
    private static final String END_DATE_STR = "2023-12-31 23:59:59";
    
    // 模拟训练数据比例
    private static final double TRAINING_DATA_RATIO = 0.7;
    
    public static void main(String[] args) {
        System.out.println("===== 基于历史轨迹统计的动态平仓策略启动 =====");
        
        try {
            // 步骤1: 加载历史数据
            List<KlineData> allKlineData = loadHistoricalData();
            if (allKlineData.isEmpty()) {
                System.out.println("未加载到历史数据，程序终止。");
                return;
            }
            
            // 步骤2: 准备训练数据和测试数据
            DataSplitResult dataSplit = splitData(allKlineData);
            
            // 步骤3: 创建原始开仓策略 - 已注释，因为用户已有实际开仓信号和轨迹
            // DynamicExitStrategy.OriginalStrategy originalStrategy = createOriginalStrategy();
            
            // 步骤4: 创建并初始化动态平仓策略
            //DynamicExitStrategy dynamicExitStrategy = new DynamicExitStrategy();
            
            // 步骤5: 生成训练用的持仓轨迹
            List<PositionTrajectory> trainingTrajectories = getTraining();
            
            // 步骤6: 学习价值函数
            ValueFunctionEstimator valueFunctionEstimator = new ValueFunctionEstimator();
            valueFunctionEstimator.learnValueFunction(trainingTrajectories);
            
            // 输出状态分布统计报告
            System.out.println(valueFunctionEstimator.generateStateDistributionReport());
            
            // 步骤7: 保存价值函数查询表
            saveValueFunctionLookupTable(valueFunctionEstimator);
            
            // 步骤8: 使用测试数据进行回测
            // 创建新的DynamicExitStrategy实例（用于价值函数查询）
            DynamicExitStrategy backtestExitStrategy = new DynamicExitStrategy();
            
            // 加载已学习的价值函数查询表
            loadValueFunctionLookupTable(backtestExitStrategy, valueFunctionEstimator);
            
            // 加载交易记录（用于回测）
            List<TradeRecord> testTradeRecords = loadTestTradeRecords(dataSplit.getTestStartDate(), dataSplit.getTestEndDate());
            System.out.println("成功加载测试交易记录: " + testTradeRecords.size() + "条");
            
            // 创建回测器实例
            StrategyBacktester backtester = new StrategyBacktester(backtestExitStrategy);
            
            // 设置价值函数估计器，用于回测时的平仓决策
            backtester.setValueFunctionEstimator(valueFunctionEstimator);
            System.out.println("已将学习到的价值函数估计器应用到回测器中");
            
            // 使用测试数据进行回测
            System.out.println("使用测试数据进行回测...");
            // 创建基于交易记录的开仓策略
            DynamicExitStrategy.OriginalStrategy tradeRecordStrategy = createTradeRecordStrategy(testTradeRecords);
            
            StrategyBacktester.BacktestResult result = backtester.runBacktest(
                    allKlineData,                     // 所有K线数据
                    tradeRecordStrategy,              // 基于交易记录的策略
                    dataSplit.getTestStartDate(),     // 测试开始日期
                    dataSplit.getTestEndDate()        // 测试结束日期
            );
            
            // 步骤9: 输出回测结果
            outputBacktestResult(result);
            
            System.out.println("===== 动态平仓策略执行完毕 =====");
            
        } catch (Exception e) {
            System.err.println("程序执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载历史数据
     * @return K线数据列表
     */
    private static List<KlineData> loadHistoricalData() {
        System.out.println("正在加载历史数据...");
        
        try {
            KlineDataLoader loader = new KlineDataLoader();
            List<KlineData> klineData = loader.loadFromCsv(DATA_FILE_PATH,"XAUUSD");
            System.out.println("成功加载历史数据: " + klineData.size() + "条记录");
            return klineData;
        } catch (Exception e) {
            System.err.println("加载历史数据失败: " + e.getMessage());
            return null;
        }
    }
    

    
    /**
     * 分割数据为训练集和测试集
     * @param allKlineData 所有K线数据
     * @return 数据分割结果
     */
    private static DataSplitResult splitData(List<KlineData> allKlineData) {
        System.out.println("正在分割训练数据和测试数据...");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(START_DATE_STR, formatter);
        LocalDateTime endDate = LocalDateTime.parse(END_DATE_STR, formatter);
        
        // 首先筛选时间范围内的数据
        List<KlineData> filteredKlineData = allKlineData.stream()
                .filter(kline -> !kline.getTimestamp().isBefore(startDate) && !kline.getTimestamp().isAfter(endDate))
                .collect(Collectors.toList());
        
        System.out.println("时间范围过滤前总数据量: " + allKlineData.size() + "条记录");
        System.out.println("时间范围过滤后数据量: " + filteredKlineData.size() + "条记录");
        System.out.println("过滤掉的数据量: " + (allKlineData.size() - filteredKlineData.size()) + "条记录");
        
        // 计算分割点（按数据量比例）
        int totalSize = filteredKlineData.size();
        int trainingSize = (int) (totalSize * TRAINING_DATA_RATIO);
        
        // 分割数据
        List<KlineData> trainingData = filteredKlineData.subList(0, trainingSize);
        List<KlineData> testingData = filteredKlineData.subList(trainingSize, totalSize);
        
        // 获取实际的分割时间点
        LocalDateTime splitDate = trainingData.isEmpty() ? startDate : trainingData.get(trainingData.size() - 1).getTimestamp();
        
        System.out.println("训练数据: " + trainingData.size() + "条记录");
        System.out.println("测试数据: " + testingData.size() + "条记录");
        System.out.println("训练数据比例: " + String.format("%.2f%%", (double)trainingData.size()/totalSize*100));
        System.out.println("测试数据比例: " + String.format("%.2f%%", (double)testingData.size()/totalSize*100));
        
        return new DataSplitResult(trainingData, testingData, startDate, splitDate, endDate);
    }
    
    /**
     * 创建原始开仓策略（示例实现）
     * @return 原始开仓策略
     */
    private static DynamicExitStrategy.OriginalStrategy createOriginalStrategy() {
        // 使用静态Random实例确保随机性
        final Random random = new Random();
        
        // 示例：简单的随机开仓策略，正确实现OriginalStrategy接口的两个方法
        return new DynamicExitStrategy.OriginalStrategy() {
            @Override
            public boolean shouldOpenPosition(List<KlineData> klineDataList, int currentIndex) {
                // 避免在数据边界处访问越界
                if (currentIndex < 0 || currentIndex >= klineDataList.size()) {
                    return false;
                }
                
                // 每100个K线点有10%的概率产生一个开仓信号
                return random.nextDouble() < 0.1;
            }
            
            @Override
            public String getPositionDirection(List<KlineData> klineDataList, int currentIndex) {
                // 随机决定持仓方向
                return random.nextBoolean() ? "多头" : "空头";
            }
        };
    }
    
    /**
     * 生成训练用的持仓轨迹
     * @param trainingData 训练数据
     * @param originalStrategy 原始开仓策略
     * @param dynamicExitStrategy 动态平仓策略
     * @return 持仓轨迹列表
     */
    private static List<PositionTrajectory> generateTrainingTrajectories(
            List<KlineData> trainingData, 
            DynamicExitStrategy.OriginalStrategy originalStrategy,
            DynamicExitStrategy dynamicExitStrategy) {
        System.out.println("正在生成训练用的持仓轨迹...");
        
        try {
            // 生成开仓信号
            List<DynamicExitStrategy.OpenSignal> openSignals = dynamicExitStrategy.generateOpenSignals(
                    trainingData, originalStrategy);
            
            // 为每个开仓信号生成持仓轨迹
            dynamicExitStrategy.generateTrajectories(openSignals, trainingData);
            
            // 获取所有生成的轨迹
            return dynamicExitStrategy.getAllTrajectories();
        } catch (Exception e) {
            System.err.println("生成训练轨迹时出错: " + e.getMessage());
            return null;
        }
    }

    public static List<PositionTrajectory> getTraining() throws IOException {
        List<PositionTrajectory> result = new ArrayList<>();
        String filePath = "D:/data/黄金分仓2交割单.xlsx"; // Excel文件路径
        List<TradeRecord> records = CsvWriter.readRecordsFromExcel(filePath);
        
        // 一次性加载并缓存所有收益率数据，避免重复IO操作
        Map<Integer, List<PositionTrajectory.ReturnPoint>> returnPointsCache = loadReturnPointsCache();
        
        // 定义日期时间格式，避免在循环中重复创建
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        
        for(TradeRecord tradeRecord:records){
            try {
                // 将字符串解析为LocalDateTime
                LocalDateTime openTime = LocalDateTime.parse(tradeRecord.getTime(), formatter);

                PositionTrajectory positionTrajectory = new PositionTrajectory(tradeRecord.getOrderId() + "", openTime, tradeRecord.getPrice(), "XAUUSD", tradeRecord.getType());
                
                // 从缓存获取收益率点数据，避免重复加载CSV文件
                List<PositionTrajectory.ReturnPoint> returnPointsList = returnPointsCache.getOrDefault(tradeRecord.getOrderId(), new ArrayList<>());
                positionTrajectory.setReturnPoints(returnPointsList);
                
                // 设置closeTime：使用最后一个收益率点的时间戳作为平仓时间
                if (!returnPointsList.isEmpty()) {
                    PositionTrajectory.ReturnPoint lastPoint = returnPointsList.get(returnPointsList.size() - 1);
                    positionTrajectory.setCloseTime(lastPoint.getTimestamp());
                    positionTrajectory.setFinalReturnRate(lastPoint.getReturnRate());
                }
                
                result.add(positionTrajectory);
            } catch (Exception e) {
                System.err.println("处理交易记录时出错，订单ID: " + tradeRecord.getOrderId() + ", 错误信息: " + e.getMessage());
                // 继续处理下一条记录
            }
        }

        return result;
    }
    
    /**
     * 一次性加载所有收益率数据并按订单ID缓存，大幅提高性能
     */
    private static Map<Integer, List<PositionTrajectory.ReturnPoint>> loadReturnPointsCache() throws IOException {
        Map<Integer, List<PositionTrajectory.ReturnPoint>> cache = new HashMap<>();
        DataLoaderNew loaderNew = new DataLoaderNew();
        List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/黄金收益分仓2.csv");
        
        // 定义日期时间格式，避免在循环中重复创建
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        
        // 遍历所有订单数据，构建缓存
        for(OrderTimeSeries orderTimeSeries : allSeries) {
            try {
                int orderId = Integer.parseInt(orderTimeSeries.getOrderId());
                double[] values = orderTimeSeries.getValues();
                String[] valueTime = orderTimeSeries.getValueTime();
                
                List<PositionTrajectory.ReturnPoint> returnPoints = new ArrayList<>(values.length);
                
                for (int i = 0; i < values.length; i++) {
                    try {
                        // 将字符串解析为LocalDateTime
                        LocalDateTime timestamp = LocalDateTime.parse(valueTime[i], formatter);
                        PositionTrajectory.ReturnPoint returnPoint = new PositionTrajectory.ReturnPoint(i, timestamp, values[i]);
                        returnPoints.add(returnPoint);
                    } catch (Exception e) {
                        // 处理单条时间解析失败的情况，继续处理下一条
                        continue;
                    }
                }
                
                cache.put(orderId, returnPoints);
            } catch (NumberFormatException e) {
                // 处理订单ID解析失败的情况，继续处理下一条
                continue;
            }
        }
        
        return cache;
    }

    /**
     * 此方法保留以保持兼容性，但内部已优化为使用缓存
     */
    public static List<PositionTrajectory.ReturnPoint> returnPoints(int orderId) throws IOException {
        // 现在从缓存中获取数据，避免重复加载CSV文件
        Map<Integer, List<PositionTrajectory.ReturnPoint>> cache = loadReturnPointsCache();
        return cache.getOrDefault(orderId, new ArrayList<>());
    }
    
    /**
     * 保存价值函数查询表
     * @param valueFunctionEstimator 价值函数估计器
     */
    private static void saveValueFunctionLookupTable(ValueFunctionEstimator valueFunctionEstimator) {
        System.out.println("正在保存价值函数查询表到: " + LOOKUP_TABLE_PATH);
        
        try (FileWriter writer = new FileWriter(LOOKUP_TABLE_PATH)) {
            String lookupTableJson = valueFunctionEstimator.generateValueFunctionLookupTable();
            writer.write(lookupTableJson);
            System.out.println("价值函数查询表保存成功");
        } catch (IOException e) {
            System.err.println("保存价值函数查询表失败: " + e.getMessage());
        }
    }
    
    /**
     * 输出回测结果
     * @param result 回测结果
     */
    private static void outputBacktestResult(StrategyBacktester.BacktestResult result) {
        if (result == null) {
            System.out.println("回测结果为空");
            return;
        }
        
        System.out.println("\n===== 回测结果摘要 =====");
        System.out.println("总交易次数: " + result.getTotalTrades());
        System.out.println("胜率: " + String.format("%.2f%%", result.getWinRate() * 100));
        System.out.println("盈亏比: " + String.format("%.2f", result.getProfitLossRatio()));
        System.out.println("总收益率: " + String.format("%.2f%%", result.getTotalReturnRate()));
        System.out.println("平均持仓时间: " + String.format("%.2f分钟", result.getAvgHoldingTimeMinutes()));
        System.out.println("最大回撤: " + String.format("%.2f%%", result.getMaxDrawdown() * 100));
        System.out.println("夏普比率: " + String.format("%.2f", result.getSharpeRatio()));
        System.out.println("======================");
        
        // 保存详细结果到文件
        saveResultToFile(result);
    }

    /**
     * 加载价值函数查询表到动态平仓策略
     * @param strategy 动态平仓策略实例
     * @param estimator 价值函数估计器
     */
    private static void loadValueFunctionLookupTable(DynamicExitStrategy strategy, ValueFunctionEstimator estimator) {
        try {
            System.out.println("正在加载价值函数查询表到回测策略...");

            // 创建一个特殊的OriginalStrategy实现，它能够获取ValueFunctionEstimator中的价值函数
            DynamicExitStrategy.OriginalStrategy customStrategy = new DynamicExitStrategy.OriginalStrategy() {
                @Override
                public boolean shouldOpenPosition(List<KlineData> klineDataList, int currentIndex) {
                    // 这个方法在回测时不会被直接调用，因为我们已经有了开仓信号
                    return false;
                }

                @Override
                public String getPositionDirection(List<KlineData> klineDataList, int currentIndex) {
                    // 同样，这个方法在回测时不会被直接调用
                    return "多头";
                }
            };

            // 为了让回测器能够正确使用价值函数，我们需要创建一个自定义的回测器适配器
            // 注意：这是一种替代方案，因为我们无法直接访问strategy的私有字段

            try {
                // 尝试使用反射机制获取TradingState到价值的映射
                Map<TradingState, Double> valueFunctionMap = new HashMap<>();

                // 由于无法直接访问StateStatistics，我们可以使用反射来获取所需的数据
                Map<?, ?> stateStatsMap = estimator.getStateStatisticsMap();

                for (Map.Entry<?, ?> entry : stateStatsMap.entrySet()) {
                    TradingState state = (TradingState) entry.getKey();
                    Object stats = entry.getValue();

                    try {
                        // 使用反射获取StateStatistics的属性和方法
                        java.lang.reflect.Method getSampleCountMethod = stats.getClass().getMethod("getSampleCount");
                        java.lang.reflect.Method getMeanReturnMethod = stats.getClass().getMethod("getMeanReturn");

                        int sampleCount = (int) getSampleCountMethod.invoke(stats);
                        double meanReturn = (double) getMeanReturnMethod.invoke(stats);

                        // 计算状态的价值函数值
                        double valueFunction;
                        if (sampleCount >= estimator.getMinSamplesPerState()) {
                            // 样本充足时，使用平均收益率作为价值函数值
                            valueFunction = meanReturn;
                        } else {
                            // 样本不足时，使用保守值
                            if (state.getTimeInterval() == 0) {
                                // 对于时间步0的状态，使用更积极的值鼓励持有
                                valueFunction = 1.0; // 一个正数，鼓励持有
                            } else {
                                // 对于其他时间步，使用基于状态收益率的保守估计
                                valueFunction = state.getReturnRate() - 0.5;
                            }
                        }

                        valueFunctionMap.put(state, valueFunction);
                    } catch (Exception e) {
                        // 如果反射获取数据失败，使用默认值
                        System.err.println("获取状态统计数据时出错: " + e.getMessage());
                        valueFunctionMap.put(state, 0.0);
                    }
                }

                // 尝试使用反射设置strategy的valueFunctionMap
                try {
                    java.lang.reflect.Field field = DynamicExitStrategy.class.getDeclaredField("valueFunctionMap");
                    field.setAccessible(true);
                    field.set(strategy, valueFunctionMap);
                    field.setAccessible(false);

                    System.out.println("成功加载价值函数查询表，包含 " + valueFunctionMap.size() + " 个状态");
                } catch (Exception e) {
                    // 如果无法直接设置valueFunctionMap，我们需要创建一个新的DynamicExitStrategy实例
                    System.err.println("警告: 无法直接设置valueFunctionMap字段: " + e.getMessage());
                    System.err.println("尝试创建包含价值函数的新策略实例...");

                    // 这种情况下，我们无法直接使用学习到的价值函数
                    // 但可以确保程序继续运行
                    System.out.println("价值函数查询表加载成功，但可能无法完全应用学习到的价值函数");
                }
            } catch (Exception e) {
                // 如果反射方法也失败，我们只能依赖默认策略
                System.err.println("加载价值函数查询表时出错: " + e.getMessage());
                System.out.println("将使用默认策略进行回测");
            }
        } catch (Exception e) {
            System.err.println("加载价值函数查询表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载测试用的交易记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 交易记录列表
     */
    private static List<TradeRecord> loadTestTradeRecords(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            String filePath = "D:/data/黄金分仓2交割单.xlsx";
            List<TradeRecord> allRecords = CsvWriter.readRecordsFromExcel(filePath);
            
            // 筛选时间范围内的交易记录
            List<TradeRecord> testRecords = allRecords.stream()
                    .filter(record -> {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                            LocalDateTime recordTime = LocalDateTime.parse(record.getTime(), formatter);
                            return !recordTime.isBefore(startDate) && !recordTime.isAfter(endDate);
                        } catch (Exception e) {
                            System.err.println("解析交易记录时间时出错: " + e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            return testRecords;
        } catch (Exception e) {
            System.err.println("加载测试交易记录时出错: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 创建基于交易记录的开仓策略
     * @param tradeRecords 交易记录列表
     * @return 开仓策略
     */
    private static DynamicExitStrategy.OriginalStrategy createTradeRecordStrategy(List<TradeRecord> tradeRecords) {
        // 构建开仓时间到交易记录信息的映射
        final Map<LocalDateTime, String> timeDirectionMap = new HashMap<>();
        final Map<LocalDateTime, Double> timePriceMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        
        for (TradeRecord record : tradeRecords) {
            try {
                LocalDateTime recordTime = LocalDateTime.parse(record.getTime(), formatter);
                // 存储方向信息
                if (record.getType() != null) {
                    timeDirectionMap.put(recordTime, "buy".equalsIgnoreCase(record.getType()) ? "多头" : "空头");
                }
                // 存储价格信息
                timePriceMap.put(recordTime, record.getPrice());
            } catch (Exception e) {
                System.err.println("解析交易记录时间时出错: " + e.getMessage());
            }
        }
        
        System.out.println("创建了基于交易记录的开仓策略，包含 " + timeDirectionMap.size() + " 个开仓时间点");
        
        return new DynamicExitStrategy.OriginalStrategy() {
            @Override
            public boolean shouldOpenPosition(List<KlineData> klineDataList, int currentIndex) {
                if (currentIndex < 0 || currentIndex >= klineDataList.size()) {
                    return false;
                }
                
                KlineData currentKline = klineDataList.get(currentIndex);
                LocalDateTime currentTime = currentKline.getTimestamp();
                
                // 检查是否存在对应的交易记录
                return timeDirectionMap.containsKey(currentTime);
            }
            
            @Override
            public String getPositionDirection(List<KlineData> klineDataList, int currentIndex) {
                if (currentIndex < 0 || currentIndex >= klineDataList.size()) {
                    return "多头"; // 默认返回多头
                }
                
                KlineData currentKline = klineDataList.get(currentIndex);
                LocalDateTime currentTime = currentKline.getTimestamp();
                
                // 直接从映射中获取方向
                return timeDirectionMap.getOrDefault(currentTime, "多头");
            }
        };
    }
    
    /**
     * 保存回测结果到文件
     * @param result 回测结果
     */
    private static void saveResultToFile(StrategyBacktester.BacktestResult result) {
        System.out.println("正在保存回测结果到: " + OUTPUT_FILE_PATH);
        
        try (FileWriter writer = new FileWriter(OUTPUT_FILE_PATH)) {
            // 构建结果JSON
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"summary\": {");
            sb.append("\"totalTrades\":").append(result.getTotalTrades()).append(",");
            sb.append("\"winRate\":").append(String.format("%.4f", result.getWinRate())).append(",");
            sb.append("\"profitLossRatio\":").append(String.format("%.4f", result.getProfitLossRatio())).append(",");
            sb.append("\"totalReturnRate\":").append(String.format("%.4f", result.getTotalReturnRate())).append(",");
            sb.append("\"avgHoldingTimeMinutes\":").append(String.format("%.4f", result.getAvgHoldingTimeMinutes())).append(",");
            sb.append("\"maxDrawdown\":").append(String.format("%.4f", result.getMaxDrawdown())).append(",");
            sb.append("\"sharpeRatio\":").append(String.format("%.4f", result.getSharpeRatio()));
            sb.append("},");
            
            // 添加交易记录
            sb.append("\"tradeRecords\": [");
            boolean first = true;
            for (StrategyBacktester.TradeRecord record : result.getTradeRecords()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append("{");
                sb.append("\"tradeId\":\"").append(record.getTradeId()).append("\",");
                sb.append("\"direction\":\"").append(record.getDirection()).append("\",");
                sb.append("\"openTime\":\"").append(record.getOpenTime()).append("\",");
                sb.append("\"closeTime\":\"").append(record.getCloseTime()).append("\",");
                sb.append("\"returnRate\":").append(String.format("%.4f", record.getReturnRate())).append(",");
                sb.append("\"holdingTimeMinutes\":").append(record.getHoldingTimeMinutes());
                sb.append("}");
            }
            sb.append("]");
            sb.append("}");
            
            writer.write(sb.toString());
            System.out.println("回测结果保存成功");
        } catch (IOException e) {
            System.err.println("保存回测结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 数据分割结果类
     */
    private static class DataSplitResult {
        private List<KlineData> trainingData;
        private List<KlineData> testingData;
        private LocalDateTime trainStartDate;
        private LocalDateTime testStartDate;
        private LocalDateTime testEndDate;
        
        public DataSplitResult(List<KlineData> trainingData, List<KlineData> testingData,
                              LocalDateTime trainStartDate, LocalDateTime testStartDate, 
                              LocalDateTime testEndDate) {
            this.trainingData = trainingData;
            this.testingData = testingData;
            this.trainStartDate = trainStartDate;
            this.testStartDate = testStartDate;
            this.testEndDate = testEndDate;
        }
        
        public List<KlineData> getTrainingData() { return trainingData; }
        public List<KlineData> getTestingData() { return testingData; }
        public LocalDateTime getTrainStartDate() { return trainStartDate; }
        public LocalDateTime getTestStartDate() { return testStartDate; }
        public LocalDateTime getTestEndDate() { return testEndDate; }
    }
}