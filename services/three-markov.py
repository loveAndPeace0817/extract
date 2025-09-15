import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime
from scipy import stats
from scipy.signal import find_peaks

# 输入数据（保持不变）
data = """-37.06,2025.06.12 13:20:00
-8.16,2025.06.12 13:25:00
-47.94,2025.06.12 13:30:00
-47.6,2025.06.12 13:35:00
-32.98,2025.06.12 13:40:00
-17.68,2025.06.12 13:45:00
-54.06,2025.06.12 13:50:00
-67.32,2025.06.12 13:55:00
-56.1,2025.06.12 14:00:00
-101.32,2025.06.12 14:05:00
-95.54,2025.06.12 14:10:00
-62.22,2025.06.12 14:15:00
-70.04,2025.06.12 14:20:00
-79.22,2025.06.12 14:25:00
-58.48,2025.06.12 14:30:00
-70.72,2025.06.12 14:35:00
-43.86,2025.06.12 14:40:00
-32.98,2025.06.12 14:45:00
-35.7,2025.06.12 14:50:00
-47.94,2025.06.12 14:55:00
-45.9,2025.06.12 15:00:00
-57.46,2025.06.12 15:05:00
-59.84,2025.06.12 15:10:00
-69.02,2025.06.12 15:15:00
-89.42,2025.06.12 15:20:00
-84.66,2025.06.12 15:25:00
-75.48,2025.06.12 15:30:00
-150.28,2025.06.12 15:35:00
-171.36,2025.06.12 15:40:00
-175.44,2025.06.12 15:45:00
-133.96,2025.06.12 15:50:00
-121.38,2025.06.12 15:55:00
-113.22,2025.06.12 16:00:00
-127.16,2025.06.12 16:05:00
-112.54,2025.06.12 16:10:00
-84.32,2025.06.12 16:15:00
-73.44,2025.06.12 16:20:00
-45.56,2025.06.12 16:25:00
-48.96,2025.06.12 16:30:00
-62.56,2025.06.12 16:35:00
-75.14,2025.06.12 16:40:00
-91.8,2025.06.12 16:45:00
-40.12,2025.06.12 16:50:00
-29.58,2025.06.12 16:55:00
-30.26,2025.06.12 17:00:00
-26.52,2025.06.12 17:05:00
-42.5,2025.06.12 17:10:00
-35.36,2025.06.12 17:15:00
-27.88,2025.06.12 17:20:00
-31.62,2025.06.12 17:25:00
-19.04,2025.06.12 17:30:00
-12.92,2025.06.12 17:35:00
-18.36,2025.06.12 17:40:00
-17,2025.06.12 17:45:00
-28.22,2025.06.12 17:50:00
-33.66,2025.06.12 17:55:00
-20.06,2025.06.12 18:00:00
3.06,2025.06.12 18:05:00
-5.78,2025.06.12 18:10:00
5.44,2025.06.12 18:15:00
10.2,2025.06.12 18:20:00
11.9,2025.06.12 18:25:00
11.56,2025.06.12 18:30:00
-5.78,2025.06.12 18:35:00
6.46,2025.06.12 18:40:00
13.26,2025.06.12 18:45:00
-0.34,2025.06.12 18:50:00
-8.16,2025.06.12 18:55:00
-3.06,2025.06.12 19:00:00
1.36,2025.06.12 19:05:00
2.04,2025.06.12 19:10:00
3.74,2025.06.12 19:15:00
-5.44,2025.06.12 19:20:00"""  # 您提供的完整数据（此处省略）

# 数据解析（保持不变）
lines = [line.split(',') for line in data.strip().split('\n')]
values = np.array([float(line[0]) for line in lines])
timestamps = [datetime.strptime(line[1].strip(), '%Y.%m.%d %H:%M:%S') for line in lines]


# 修复1：安全的斜率计算函数
def safe_slope_calc(x, y):
    """处理SVD不收敛问题的稳健斜率计算"""
    try:
        slope = np.polyfit(range(len(x)), x, 1)[0]
    except np.linalg.LinAlgError:
        # 使用最小二乘法替代
        A = np.vstack([range(len(x)), np.ones(len(x))]).T
        slope = np.linalg.lstsq(A, x, rcond=None)[0][0]
    return slope


# 修复2：增强的趋势类型判断
def determine_trend_type(segment_values):
    if len(segment_values) < 3:  # 数据点不足时返回默认值
        return ("数据不足", 0.5)

    # 使用安全斜率计算
    slope = safe_slope_calc(segment_values, range(len(segment_values)))
    volatility = np.std(segment_values)

    # 判断逻辑
    if abs(slope) > 0.5:  # 显著斜率
        return ("趋势上涨", 0.65) if slope > 0 else ("趋势下跌", 0.65)
    else:
        return ("宽幅震荡", 0.5) if volatility > 20 else ("窄幅震荡", 0.5)


# 修复3：稳健的Hurst指数计算
def hurst_exponent(ts):
    if len(ts) < 5:  # 数据点不足时返回默认值
        return 0.5

    try:
        lags = range(2, min(20, len(ts) // 2))
        tau = [np.std(np.subtract(ts[lag:], ts[:-lag])) for lag in lags]
        return np.polyfit(np.log(lags), np.log(tau), 1)[0] if len(lags) > 1 else 0.5
    except:
        return 0.5


# 修复4：改进的自动分段函数
def auto_segment(values, timestamps):
    """基于视觉显著点的分段逻辑"""
    # 识别关键极值点
    peaks, _ = find_peaks(values, prominence=20)  # 突出至少20bps的峰值
    valleys, _ = find_peaks(-values, prominence=20)  # 突出至少20bps的谷底

    # 合并并排序所有关键点
    turning_points = sorted(set(np.concatenate([peaks, valleys])))

    # 强制包含首尾点
    turning_points = [0] + [p for p in turning_points if 0 < p < len(values) - 1] + [len(values) - 1]

    # 筛选最具代表性的转折点（按价格变化幅度）
    significant_points = []
    for i in range(1, len(turning_points)):
        prev = turning_points[i - 1]
        curr = turning_points[i]
        price_change = abs(values[curr] - values[prev])
        if price_change > 30:  # 至少30bps的变化才视为有效转折
            significant_points.append(curr)

    # 合并相邻过近的转折点（最小30分钟间隔）
    merged_points = []
    min_interval = 6  # 5分钟*6=30分钟
    for point in significant_points:
        if not merged_points or point - merged_points[-1] >= min_interval:
            merged_points.append(point)

    # 确保分段在3-5段之间
    if len(merged_points) > 4:
        # 取价格变化最大的4个点
        merged_points = sorted([
            merged_points[np.argmax([abs(values[p] - values[merged_points[i - 1]])
                                     for i, p in enumerate(merged_points[1:], 1)])],
            merged_points[np.argmax([abs(values[p] - values[0]) for p in merged_points])],
            merged_points[np.argmax([abs(values[-1] - values[p]) for p in merged_points])]
        ])

    # 构建最终分段
    segments = []
    last_point = 0
    for point in merged_points[:4]:  # 最多取4个分割点形成5段
        segments.append((last_point, point))
        last_point = point
    segments.append((last_point, len(values) - 1))

    return segments[:5]  # 保证最多5段


# 执行分段
segments = auto_segment(values, timestamps)


# 阶段分析（使用修复后的函数）
def analyze_segment(values, start_idx, end_idx):
    segment_values = values[start_idx:end_idx + 1]

    # 基础指标
    duration = (timestamps[end_idx] - timestamps[start_idx]).total_seconds() / 60
    mean_return = np.mean(segment_values)
    max_dd = np.min(segment_values)
    volatility = np.std(segment_values)

    # 使用修复后的函数
    trend_type, hurst = determine_trend_type(segment_values)

    return {
        "时间范围": f"{timestamps[start_idx].strftime('%H:%M')}-{timestamps[end_idx].strftime('%H:%M')}",
        "持续时间(min)": round(duration),
        "趋势类型": trend_type,
        "平均收益(bps)": round(mean_return, 1),
        "最大回撤(bps)": round(max_dd, 1),
        "波动率": round(volatility, 1),
        "Hurst指数": round(hurst, 2),
        "趋势强度": "强" if abs(mean_return) > 30 else "中" if abs(mean_return) > 15 else "弱"
    }


# 整体评估（使用修复后的函数）
def evaluate_overall(values):
    try:
        slope = safe_slope_calc(values, range(len(values)))
        hurst = hurst_exponent(values)
        return {
            "整体趋势": "趋势上涨" if slope > 0.3 else "趋势下跌" if slope < -0.3 else "震荡行情",
            "累计收益(bps)": round(values[-1] - values[0], 1),
            "整体Hurst指数": round(hurst, 2),
            "趋势持续性": "强" if hurst > 0.65 else "中" if hurst > 0.55 else "弱"
        }
    except:
        return {
            "整体趋势": "数据不足",
            "累计收益(bps)": 0,
            "整体Hurst指数": 0.5,
            "趋势持续性": "未知"
        }


# 生成报告
seg_reports = [analyze_segment(values, start, end) for start, end in segments]
overall_report = evaluate_overall(values)

# 输出结果
print("=== 三段式趋势分析 ===")
print(pd.DataFrame(seg_reports).to_string(index=False))

print("\n=== 整体趋势评估 ===")
for k, v in overall_report.items():
    print(f"{k}: {v}")

# 可视化
plt.figure(figsize=(12, 5))
plt.plot(timestamps, values, color='navy', linewidth=1.5)

colors = {'趋势上涨': 'green', '趋势下跌': 'red',
          '宽幅震荡': 'orange', '窄幅震荡': 'blue',
          '数据不足': 'gray'}
for (start, end), report in zip(segments, seg_reports):
    plt.axvspan(timestamps[start], timestamps[end],
                color=colors[report['趋势类型']], alpha=0.2)

plt.title("市场趋势分析（修复版）")
plt.ylabel("收益 (bps)")
plt.xticks(rotation=45)
plt.grid(alpha=0.3)
plt.tight_layout()
plt.show()