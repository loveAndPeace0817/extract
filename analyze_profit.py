import csv
import statistics

def analyze_profit(csv_file):
    profits = []
    order_profits = {}

    with open(csv_file, 'r', encoding='utf-8') as file:
        reader = csv.reader(file)
        next(reader)  # 跳过标题行
        for row in reader:
            if len(row) < 1:
                continue
            try:
                profit = float(row[0])
                profits.append(profit)
                # 获取订单号（假设第二列是订单号）
                if len(row) >= 2:
                    order_id = row[1]
                    if order_id not in order_profits:
                        order_profits[order_id] = []
                    order_profits[order_id].append(profit)
            except ValueError:
                continue

    # 计算整体统计量
    overall_stats = {
        'count': len(profits),
        'min': min(profits) if profits else 0,
        'max': max(profits) if profits else 0,
        'mean': statistics.mean(profits) if profits else 0,
        'median': statistics.median(profits) if profits else 0,
        'std_dev': statistics.stdev(profits) if len(profits) > 1 else 0
    }

    # 计算每个订单的统计量
    order_stats = {}
    for order_id, order_profit_list in order_profits.items():
        order_stats[order_id] = {
            'count': len(order_profit_list),
            'min': min(order_profit_list),
            'max': max(order_profit_list),
            'mean': statistics.mean(order_profit_list),
            'std_dev': statistics.stdev(order_profit_list) if len(order_profit_list) > 1 else 0
        }

    return overall_stats, order_stats

if __name__ == '__main__':
    csv_file = '测试777.csv'
    overall_stats, order_stats = analyze_profit(csv_file)

    print("=== 整体利润统计 ===")
    print(f"数据量: {overall_stats['count']}")
    print(f"最小值: {overall_stats['min']}")
    print(f"最大值: {overall_stats['max']}")
    print(f"平均值: {overall_stats['mean']:.2f}")
    print(f"中位数: {overall_stats['median']:.2f}")
    print(f"标准差: {overall_stats['std_dev']:.2f}")

    print("\n=== 订单利润统计 (前5个订单) ===")
    sorted_orders = sorted(order_stats.keys())[:5]
    for order_id in sorted_orders:
        stats = order_stats[order_id]
        print(f"订单 {order_id}:")
        print(f"  数据量: {stats['count']}")
        print(f"  最小值: {stats['min']:.2f}")
        print(f"  最大值: {stats['max']:.2f}")
        print(f"  平均值: {stats['mean']:.2f}")
        print(f"  标准差: {stats['std_dev']:.2f}")