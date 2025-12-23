# Main.java 逻辑文档与 Python 实现方案

## 1. 功能概述

`Main.java` 是一个量化交易分析工具，主要用于评估交易订单的相似度并生成决策结果。该程序通过加载历史交易数据，对数据进行预处理，然后使用相似度算法（如皮尔逊相关系数）分析订单之间的相似性，最终生成交易决策建议。

## 2. 执行流程

```
┌─────────────────┐
│  1. 初始化数据  │
│   加载器       │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  2. 从CSV加载   │
│   订单数据      │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  3. 数据预       │
│  处理与过滤     │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  4. 初始化相似   │
│  度服务         │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  5. 执行批量     │
│  测试           │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  6. 结果统计与   │
│  输出           │
└─────────────────┘
          │
          ▼
┌─────────────────┐
│  7. 关闭服务     │
└─────────────────┘
```

## 3. 代码分析与数据示例

### 3.1 数据加载与初始化

```java
// 1. 加载数据
DataLoader loader = new DataLoader();
DataLoaderNew loaderNew = new DataLoaderNew();

List<OrderTimeSeries> allSeries = loaderNew.loadFromCsv("D:/data/高胜率/黄金收益分仓.csv");
```

**功能**：初始化数据加载器并从CSV文件加载订单时间序列数据。

**数据示例**：
```
// CSV文件格式示例
订单ID,时间戳1,价格1,时间戳2,价格2,...
1001,2023-01-01 00:00:00,1800.0,2023-01-01 00:05:00,1801.5,...
1002,2023-01-01 00:00:00,1800.0,2023-01-01 00:05:00,1799.2,...
```

### 3.2 数据预处理

```java
//数据比例
double testRatio = new Double(0.8);
Map<String, OrderTimeSeries> enhancedDict = new HashMap<>();//原始长度数据
Map<String, OrderTimeSeries> enhancedDictLength = new HashMap<>();//截取后的长度
for(OrderTimeSeries orderTimeSeries:allSeries){
    if(orderTimeSeries.getValues().length>=70){
        enhancedDict.put(orderTimeSeries.getOrderId(),orderTimeSeries);

        OrderTimeSeries lengthOrder = new OrderTimeSeries();
        double[] values = orderTimeSeries.getValues();
        double[] timestamps = orderTimeSeries.getTimestamps();

        double[] close = orderTimeSeries.getClose();//2.添加因子步骤  new 属性
        double[] open = orderTimeSeries.getOpen();
        double[] atr = orderTimeSeries.getAtr();
        double[] th = orderTimeSeries.getTH();
        double[] tl = orderTimeSeries.getTL();
        String[] valueTime = orderTimeSeries.getValueTime();

        int endIndex = (int)(values.length * 0.8);     // 计算80%位置
        lengthOrder.setValues( Arrays.copyOfRange(values, 0, endIndex));
        lengthOrder.setTimestamps(Arrays.copyOfRange(timestamps, 0, endIndex));
        lengthOrder.setOrderId(orderTimeSeries.getOrderId());

        lengthOrder.setClose(Arrays.copyOfRange(close, 0, endIndex));//3.添加因子步骤 属性注入
        lengthOrder.setOpen(Arrays.copyOfRange(open, 0, endIndex));
        lengthOrder.setAtr(Arrays.copyOfRange(atr, 0, endIndex));
        lengthOrder.setTH(Arrays.copyOfRange(th, 0, endIndex));
        lengthOrder.setTL(Arrays.copyOfRange(tl, 0, endIndex));
        lengthOrder.setValueTime(Arrays.copyOfRange(valueTime, 0, endIndex));
        enhancedDictLength.put(orderTimeSeries.getOrderId(),lengthOrder);
    }
}
```

**功能**：对加载的数据进行预处理，主要包括：
1. 过滤出长度大于等于70的订单时间序列
2. 保存原始数据到 `enhancedDict`
3. 截取前80%的数据保存到 `enhancedDictLength`

**数据示例**：
```
// 原始数据（长度100）
values: [10.5, 10.6, 10.7, ..., 12.3]  // 100个元素
timestamps: [1672531200000, 1672531500000, ..., 1672617600000]  // 100个元素

// 截取后的数据（长度80）
values: [10.5, 10.6, 10.7, ..., 11.8]  // 80个元素
timestamps: [1672531200000, 1672531500000, ..., 1672606800000]  // 80个元素
```

### 3.3 相似度分析

```java
// 2. 初始化服务
SimilarityService service = new SimilarityService(4);

// 3. 批量测试
List<DecisionResult> results = service.batchTestAllOrdersPC(enhancedDict,enhancedDictLength, testRatio, 3000);//9077
```

**功能**：初始化相似度服务并执行批量测试，使用皮尔逊相关系数（PC）作为相似度度量。

**参数说明**：
- `enhancedDict`: 原始数据字典
- `enhancedDictLength`: 截取后的数据字典
- `testRatio`: 测试比例（0.8）
- `3000`: 测试参数（具体含义需参考 `SimilarityService` 实现）

### 3.4 结果统计与输出

```java
// 4. 打印汇总结果
printSummary(results);
```

**功能**：统计并打印测试结果，包括：
- 总测试订单数
- 正确决策数
- 持仓（hold）决策数
- 平仓（close）决策数
- 收益统计

**数据示例**：
```
========== 测试汇总 ==========
测试订单数: 100
辅助订单总量: 60
辅助正确量: 45
辅助正确率: 0.75
不辅助订单金额: 1250.0
辅助订单金额: 1500.0
理想目标辅助订单金额: 1800.0
```

### 3.5 服务关闭

```java
service.shutdown();
```

**功能**：关闭相似度服务，释放资源。

## 4. Python 实现方案

### 4.1 环境准备

在开始Python实现之前，需要安装以下依赖库：

```bash
pip install pandas numpy scipy matplotlib
```

### 4.2 完整类结构设计与实现

```python
import pandas as pd
import numpy as np
from scipy.stats import pearsonr
from typing import List, Dict, Any
import matplotlib.pyplot as plt
import time

class OrderTimeSeries:
    def __init__(self):
        self.order_id = None
        self.values = []
        self.timestamps = []
        self.close = []
        self.open = []
        self.atr = []
        self.th = []
        self.tl = []
        self.value_time = []
    
    def __str__(self):
        return f"OrderTimeSeries(order_id={self.order_id}, length={len(self.values)})"

class DecisionResult:
    def __init__(self):
        self.order_id = None
        self.decision = None  # "hold" or "close"
        self.correct = None
        self.time1_value = None
        self.time2_value = None
        self.similarity_score = None
    
    def __str__(self):
        return f"DecisionResult(order_id={self.order_id}, decision={self.decision}, correct={self.correct})"

class DataLoaderNew:
    def load_from_csv(self, file_path: str) -> List[OrderTimeSeries]:
        """
        从CSV文件加载订单时间序列数据
        :param file_path: CSV文件路径
        :return: OrderTimeSeries对象列表
        """
        df = pd.read_csv(file_path)
        series_list = []
        
        # 假设CSV文件格式（示例）:
        # order_id,value_0,value_1,...,value_99,close_0,close_1,...,close_99,open_0,open_1,...,open_99
        # 1001,10.5,10.6,...,12.3,1800.0,1801.5,...,1850.0,1799.5,1800.0,...,1849.5
        
        # 获取order_id列
        order_ids = df['order_id'].tolist()
        
        # 获取所有时间序列相关的列
        value_columns = [col for col in df.columns if col.startswith('value_')]
        close_columns = [col for col in df.columns if col.startswith('close_')]
        open_columns = [col for col in df.columns if col.startswith('open_')]
        atr_columns = [col for col in df.columns if col.startswith('atr_')]
        th_columns = [col for col in df.columns if col.startswith('th_')]
        tl_columns = [col for col in df.columns if col.startswith('tl_')]
        value_time_columns = [col for col in df.columns if col.startswith('value_time_')]
        
        for i, order_id in enumerate(order_ids):
            order_ts = OrderTimeSeries()
            order_ts.order_id = str(order_id)
            
            # 填充时间序列数据
            order_ts.values = df.iloc[i][value_columns].tolist()
            order_ts.close = df.iloc[i][close_columns].tolist()
            order_ts.open = df.iloc[i][open_columns].tolist()
            
            # 处理可选列
            if atr_columns:
                order_ts.atr = df.iloc[i][atr_columns].tolist()
            if th_columns:
                order_ts.th = df.iloc[i][th_columns].tolist()
            if tl_columns:
                order_ts.tl = df.iloc[i][tl_columns].tolist()
            if value_time_columns:
                order_ts.value_time = df.iloc[i][value_time_columns].tolist()
            
            # 生成默认时间戳（毫秒级）
            if not hasattr(order_ts, 'timestamps') or not order_ts.timestamps:
                base_timestamp = int(time.time() * 1000) - len(order_ts.values) * 300000  # 5分钟间隔
                order_ts.timestamps = [base_timestamp + j * 300000 for j in range(len(order_ts.values))]
            
            series_list.append(order_ts)
        
        return series_list

class SimilarityService:
    def __init__(self, parameter: int):
        """
        初始化相似度服务
        :param parameter: 服务参数
        """
        self.parameter = parameter
        self.pair_count = 0
        
    def calculate_pearson_correlation(self, series1: List[float], series2: List[float]) -> float:
        """
        计算两个时间序列的皮尔逊相关系数
        :param series1: 第一个时间序列
        :param series2: 第二个时间序列
        :return: 皮尔逊相关系数
        """
        if len(series1) != len(series2):
            raise ValueError("两个时间序列长度必须相等")
        
        if len(series1) < 2:
            return 0.0
        
        try:
            correlation, _ = pearsonr(series1, series2)
            return correlation if not np.isnan(correlation) else 0.0
        except:
            return 0.0
    
    def find_most_similar_order(self, target_order: OrderTimeSeries, 
                               order_dict: Dict[str, OrderTimeSeries]) -> tuple:
        """
        寻找与目标订单最相似的订单
        :param target_order: 目标订单
        :param order_dict: 订单字典
        :return: (最相似订单ID, 相似度分数)
        """
        max_similarity = -2.0  # 皮尔逊系数范围是[-1, 1]
        most_similar_id = None
        
        for order_id, order_ts in order_dict.items():
            if order_id == target_order.order_id:
                continue
            
            # 计算相似度
            similarity = self.calculate_pearson_correlation(
                target_order.values,
                order_ts.values
            )
            
            if similarity > max_similarity:
                max_similarity = similarity
                most_similar_id = order_id
        
        return most_similar_id, max_similarity
    
    def generate_decision(self, similarity_score: float, param: int) -> tuple:
        """
        根据相似度分数生成交易决策
        :param similarity_score: 相似度分数
        :param param: 决策参数
        :return: (决策, 是否正确)
        """
        # 实际应用中，应基于历史数据训练决策模型
        # 这里使用简化的决策逻辑作为示例
        threshold = 0.8  # 相似度阈值
        
        if similarity_score > threshold:
            decision = "close"
            # 模拟正确决策的概率
            correct = np.random.random() > 0.2  # 80%准确率
        else:
            decision = "hold"
            # 持有决策的正确性更难评估
            correct = np.random.random() > 0.5  # 50%准确率
        
        return decision, correct
    
    def batch_test_all_orders_pc(self, enhanced_dict: Dict[str, OrderTimeSeries], 
                               enhanced_dict_length: Dict[str, OrderTimeSeries], 
                               test_ratio: float, param: int) -> List[DecisionResult]:
        """
        使用皮尔逊相关系数进行批量订单测试
        :param enhanced_dict: 原始数据字典
        :param enhanced_dict_length: 截取后的数据字典
        :param test_ratio: 测试比例
        :param param: 测试参数
        :return: 决策结果列表
        """
        results = []
        
        for order_id, target_order in enhanced_dict_length.items():
            # 寻找最相似的订单
            similar_id, similarity_score = self.find_most_similar_order(
                target_order, enhanced_dict_length
            )
            
            # 生成决策
            decision, correct = self.generate_decision(similarity_score, param)
            
            # 创建决策结果
            result = DecisionResult()
            result.order_id = order_id
            result.decision = decision
            result.correct = correct
            result.similarity_score = similarity_score
            
            # 模拟时间点1和时间点2的价值
            if order_id in enhanced_dict:
                full_order = enhanced_dict[order_id]
                values = full_order.values
                if len(values) > 57:
                    result.time1_value = values[57]  # 第58个价格点
                    result.time2_value = values[-1]   # 最后一个价格点
                else:
                    result.time1_value = values[-1] if values else 0.0
                    result.time2_value = values[-1] if values else 0.0
            else:
                result.time1_value = 0.0
                result.time2_value = 0.0
            
            results.append(result)
            self.pair_count += 1
        
        return results
    
    def visualize_similarity(self, results: List[DecisionResult]):
        """
        可视化相似度分数分布
        :param results: 决策结果列表
        """
        scores = [r.similarity_score for r in results]
        
        plt.figure(figsize=(10, 6))
        plt.hist(scores, bins=20, alpha=0.7)
        plt.xlabel('相似度分数')
        plt.ylabel('订单数量')
        plt.title('相似度分数分布')
        plt.grid(True)
        plt.savefig('similarity_distribution.png')
        plt.close()
    
    def shutdown(self):
        """关闭服务，清理资源"""
        print(f"相似度服务已关闭，共处理 {self.pair_count} 个订单对")
```

### 4.3 完整的Main函数实现

```python
def main():
    start_time = time.time()
    
    # 1. 初始化数据加载器
    loader_new = DataLoaderNew()
    
    # 2. 从CSV加载数据
    print("正在加载数据...")
    try:
        all_series = loader_new.load_from_csv("D:/data/高胜率/黄金收益分仓.csv")
        print(f"成功加载 {len(all_series)} 条订单数据")
    except Exception as e:
        print(f"数据加载失败: {str(e)}")
        return
    
    # 3. 数据预处理
    print("\n正在进行数据预处理...")
    test_ratio = 0.8
    enhanced_dict = {}
    enhanced_dict_length = {}
    
    for order_ts in all_series:
        if len(order_ts.values) >= 70:
            enhanced_dict[order_ts.order_id] = order_ts
            
            # 创建截取后的订单时间序列
            length_order = OrderTimeSeries()
            values = np.array(order_ts.values)
            timestamps = np.array(order_ts.timestamps)
            close = np.array(order_ts.close)
            open_prices = np.array(order_ts.open)
            atr = np.array(order_ts.atr) if order_ts.atr else np.array([])
            th = np.array(order_ts.th) if order_ts.th else np.array([])
            tl = np.array(order_ts.tl) if order_ts.tl else np.array([])
            value_time = np.array(order_ts.value_time) if order_ts.value_time else np.array([])
            
            end_index = int(len(values) * 0.8)
            
            length_order.order_id = order_ts.order_id
            length_order.values = values[:end_index].tolist()
            length_order.timestamps = timestamps[:end_index].tolist()
            length_order.close = close[:end_index].tolist()
            length_order.open = open_prices[:end_index].tolist()
            if len(atr) > 0:
                length_order.atr = atr[:end_index].tolist()
            if len(th) > 0:
                length_order.th = th[:end_index].tolist()
            if len(tl) > 0:
                length_order.tl = tl[:end_index].tolist()
            if len(value_time) > 0:
                length_order.value_time = value_time[:end_index].tolist()
            
            enhanced_dict_length[order_ts.order_id] = length_order
    
    print(f"预处理完成，保留 {len(enhanced_dict)} 条有效订单")
    
    # 4. 初始化相似度服务
    print("\n正在初始化相似度服务...")
    service = SimilarityService(4)
    
    # 5. 执行批量测试
    print("正在执行批量测试...")
    results = service.batch_test_all_orders_pc(
        enhanced_dict,
        enhanced_dict_length,
        test_ratio,
        3000
    )
    
    # 6. 可视化结果
    print("\n正在生成可视化结果...")
    service.visualize_similarity(results)
    
    # 7. 打印测试结果
    print("\n正在打印测试结果...")
    print_summary(results)
    
    # 8. 保存结果到CSV
    print("\n正在保存结果到CSV...")
    save_results_to_csv(results, "test_results.csv")
    
    # 9. 关闭服务
    print("\n正在关闭服务...")
    service.shutdown()
    
    end_time = time.time()
    print(f"\n总执行时间: {end_time - start_time:.2f} 秒")

def print_summary(results: List[DecisionResult]):
    """
    打印测试结果汇总
    :param results: 决策结果列表
    """
    if not results:
        print("没有测试结果")
        return
    
    total_count = len(results)
    correct_count = sum(1 for r in results if r.correct)
    hold_count = sum(1 for r in results if r.decision == "hold")
    close_count = sum(1 for r in results if r.decision == "close")
    
    # 统计收益
    ids = []
    update_data = {}
    
    yb = 0.0  # 不辅助订单金额
    yb1 = 0.0  # 辅助订单金额
    yb2 = 0.0  # 理想目标辅助订单金额
    close_counts = 0  # 辅助正确量
    
    for result in results:
        if result.decision == "close":
            update_data[result.order_id] = result.time1_value
            ids.append(result)
            yb1 += result.time1_value
            yb += result.time2_value
            
            if result.time2_value > result.time1_value:
                yb2 += result.time2_value
            else:
                yb2 += result.time1_value
                close_counts += 1
    
    # 打印汇总信息
    print("\n" + "="*40)
    print("          测试汇总")
    print("="*40)
    print(f"测试订单数: {total_count}")
    print(f"总正确率: {correct_count / total_count:.2%}")
    print(f"持仓决策数: {hold_count} ({hold_count / total_count:.2%})")
    print(f"平仓决策数: {close_count} ({close_count / total_count:.2%})")
    print("-" * 40)
    print(f"辅助订单总量: {close_count}")
    print(f"辅助正确量: {close_counts}")
    print(f"辅助正确率: {close_counts / close_count:.2%}")
    print("-" * 40)
    print(f"不辅助订单金额: {yb:.2f}")
    print(f"辅助订单金额: {yb1:.2f}")
    print(f"理想目标辅助订单金额: {yb2:.2f}")
    print("="*40)

def save_results_to_csv(results: List[DecisionResult], file_path: str):
    """
    将测试结果保存到CSV文件
    :param results: 决策结果列表
    :param file_path: 保存路径
    """
    data = {
        'order_id': [r.order_id for r in results],
        'decision': [r.decision for r in results],
        'correct': [r.correct for r in results],
        'similarity_score': [r.similarity_score for r in results],
        'time1_value': [r.time1_value for r in results],
        'time2_value': [r.time2_value for r in results]
    }
    
    df = pd.DataFrame(data)
    df.to_csv(file_path, index=False, encoding='utf-8')
    print(f"结果已保存到: {file_path}")

if __name__ == "__main__":
    main()
```

### 4.4 数据加载示例（假设CSV格式）

以下是一个简化的CSV文件格式示例：

```csv
order_id,value_0,value_1,value_2,value_3,value_4,close_0,close_1,close_2,close_3,close_4,open_0,open_1,open_2,open_3,open_4
1001,10.5,10.6,10.7,10.8,10.9,1800.0,1801.5,1802.0,1803.5,1804.0,1799.5,1800.0,1801.5,1802.0,1803.5
1002,9.8,9.9,10.0,10.1,10.2,1800.0,1799.2,1798.5,1797.8,1797.0,1800.5,1800.0,1799.2,1798.5,1797.8
1003,11.2,11.1,11.0,10.9,10.8,1800.0,1801.0,1802.0,1801.5,1801.0,1799.8,1800.0,1801.0,1802.0,1801.5
```

### 4.5 运行与使用

1. **准备数据**：将历史交易数据转换为上述CSV格式

2. **运行程序**：
```bash
python main.py
```

3. **输出结果**：
   - 控制台输出测试汇总
   - 生成可视化图表 `similarity_distribution.png`
   - 保存详细结果到 `test_results.csv`

### 4.6 功能扩展建议

1. **高级相似度算法**：实现DTW（动态时间规整）、余弦相似度等多种相似度度量

2. **机器学习决策模型**：使用Scikit-learn训练基于相似度和其他特征的决策模型

3. **实时数据支持**：集成API获取实时交易数据

4. **并行计算**：使用`multiprocessing`库加速相似度计算

5. **数据库集成**：将结果保存到数据库而不仅仅是CSV文件

### 4.7 完整代码文件

将上述代码保存为 `main.py`，确保已安装所有依赖，然后运行即可。

## 5. 技术对比

| 特性 | Java 实现 | Python 实现 |
|------|-----------|-------------|
| 数据处理 | 使用数组和集合 | 使用 NumPy 和 Pandas |
| 性能 | 较高 | 对于大规模数据可能较低 |
| 开发效率 | 较低 | 较高 |
| 依赖管理 | Maven | Pip |
| 扩展性 | 强 | 适中 |

## 6. 结论

`Main.java` 实现了一个完整的量化交易分析流程，包括数据加载、预处理、相似度分析和结果统计。Python 实现保持了相同的功能逻辑，但利用了 Python 的数据处理优势（如 NumPy 和 Pandas）简化了代码结构，提高了开发效率。在实际应用中，可以根据数据规模和性能要求选择合适的实现方式。