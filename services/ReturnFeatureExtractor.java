package com.demo.extract.services;


public class ReturnFeatureExtractor {

    /**
     * 提取趋势特征
     * @param returns 收益数据数组
     * @return 包含趋势斜率的double数组
     */
    public double[] extractTrendFeatures(double[] returns) {
        if (returns == null || returns.length < 2) {
            return new double[]{0};
        }

        // 计算简单线性回归的斜率（趋势）
        int n = returns.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = returns[i];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return new double[]{slope};
    }

    /**
     * 提取波动率特征
     * @param returns 收益数据数组
     * @return 包含滚动波动率和已实现波动率的double数组
     */
    public double[] extractVolatilityFeatures(double[] returns) {
        if (returns == null || returns.length < 20) {
            return new double[]{0, 0};
        }

        // 计算滚动波动率（20期）
        double rollingVol = calculateRollingVolatility(returns, 20);

        // 计算已实现波动率
        double realizedVol = calculateRealizedVolatility(returns);

        return new double[]{rollingVol, realizedVol};
    }

    /**
     * 提取极值特征
     * @param returns 收益数据数组
     * @return 包含最大回撤、偏度和峰度的double数组
     */
    public double[] extractExtremeFeatures(double[] returns) {
        if (returns == null || returns.length < 2) {
            return new double[]{0, 0, 0};
        }

        // 计算最大回撤
        double maxDrawdown = calculateMaxDrawdown(returns);

        // 计算偏度
        double skewness = calculateSkewness(returns);

        // 计算峰度
        double kurtosis = calculateKurtosis(returns);

        return new double[]{maxDrawdown, skewness, kurtosis};
    }

    /**
     * 提取非线性特征
     * @param returns 收益数据数组
     * @return 包含Hurst指数的double数组
     */
    public double[] extractNonlinearFeatures(double[] returns) {
        if (returns == null || returns.length < 10) {
            return new double[]{0.5}; // 默认为随机游走
        }

        // 计算Hurst指数
        double hurst = calculateHurstExponent(returns);

        return new double[]{hurst};
    }

    // 计算滚动波动率
    private double calculateRollingVolatility(double[] returns, int window) {
        int n = returns.length;
        double sumSquared = 0;

        // 取最后window个数据计算波动率
        for (int i = n - window; i < n; i++) {
            sumSquared += returns[i] * returns[i];
        }

        return Math.sqrt(sumSquared / window);
    }

    // 计算已实现波动率
    private double calculateRealizedVolatility(double[] returns) {
        double sumSquared = 0;
        for (double ret : returns) {
            sumSquared += ret * ret;
        }
        return Math.sqrt(sumSquared / returns.length);
    }

    // 计算最大回撤
    private double calculateMaxDrawdown(double[] returns) {
        double cumulativeReturn = 1.0;
        double maxCumulativeReturn = 1.0;
        double maxDrawdown = 0.0;

        for (double ret : returns) {
            cumulativeReturn *= (1 + ret);
            maxCumulativeReturn = Math.max(maxCumulativeReturn, cumulativeReturn);
            double drawdown = (maxCumulativeReturn - cumulativeReturn) / maxCumulativeReturn;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }

        return maxDrawdown;
    }

    // 计算偏度
    private double calculateSkewness(double[] returns) {
        int n = returns.length;
        double mean = 0, variance = 0, skewness = 0;

        // 计算均值
        for (double ret : returns) {
            mean += ret;
        }
        mean /= n;

        // 计算方差
        for (double ret : returns) {
            variance += Math.pow(ret - mean, 2);
        }
        variance /= n;

        // 计算偏度
        for (double ret : returns) {
            skewness += Math.pow(ret - mean, 3);
        }
        skewness /= (n * Math.pow(variance, 1.5));

        return skewness;
    }

    // 计算峰度
    private double calculateKurtosis(double[] returns) {
        int n = returns.length;
        double mean = 0, variance = 0, kurtosis = 0;

        // 计算均值
        for (double ret : returns) {
            mean += ret;
        }
        mean /= n;

        // 计算方差
        for (double ret : returns) {
            variance += Math.pow(ret - mean, 2);
        }
        variance /= n;

        // 计算峰度
        for (double ret : returns) {
            kurtosis += Math.pow(ret - mean, 4);
        }
        kurtosis /= (n * Math.pow(variance, 2));

        return kurtosis; // 超额峰度可以减去3
    }

    // 计算Hurst指数 (简化版)
    private double calculateHurstExponent(double[] returns) {
        int n = returns.length;
        int maxWindow = n / 2;
        double[] windowSizes = new double[10];
        double[] rsValues = new double[10];

        // 生成10个不同的窗口大小
        for (int i = 0; i < 10; i++) {
            windowSizes[i] = 10 + i * (maxWindow - 10) / 9;
        }

        // 计算每个窗口大小的R/S值
        for (int i = 0; i < 10; i++) {
            int window = (int) windowSizes[i];
            int numWindows = n / window;
            double avgRS = 0;

            for (int j = 0; j < numWindows; j++) {
                // 截取窗口数据
                double[] windowData = new double[window];
                System.arraycopy(returns, j * window, windowData, 0, window);

                // 计算累积偏差
                double[] cumulativeDeviation = new double[window];
                double mean = 0;
                for (double data : windowData) mean += data;
                mean /= window;

                cumulativeDeviation[0] = windowData[0] - mean;
                for (int k = 1; k < window; k++) {
                    cumulativeDeviation[k] = cumulativeDeviation[k-1] + windowData[k] - mean;
                }

                // 计算极差
                double max = cumulativeDeviation[0];
                double min = cumulativeDeviation[0];
                for (double dev : cumulativeDeviation) {
                    if (dev > max) max = dev;
                    if (dev < min) min = dev;
                }
                double range = max - min;

                // 计算标准差
                double std = 0;
                for (double data : windowData) {
                    std += Math.pow(data - mean, 2);
                }
                std = Math.sqrt(std / window);

                // 计算R/S
                if (std > 0) avgRS += range / std;
            }

            avgRS /= numWindows;
            rsValues[i] = avgRS;
        }

        // 线性回归求Hurst指数
        double sumLogWindow = 0, sumLogRS = 0, sumLogWindowSquared = 0, sumLogWindowLogRS = 0;
        for (int i = 0; i < 10; i++) {
            double logWindow = Math.log10(windowSizes[i]);
            double logRS = Math.log10(rsValues[i]);
            sumLogWindow += logWindow;
            sumLogRS += logRS;
            sumLogWindowSquared += logWindow * logWindow;
            sumLogWindowLogRS += logWindow * logRS;
        }

        double hurst = (10 * sumLogWindowLogRS - sumLogWindow * sumLogRS) /
                (10 * sumLogWindowSquared - sumLogWindow * sumLogWindow);

        return Math.max(0, Math.min(1, hurst)); // 限制在0到1之间
    }
}
