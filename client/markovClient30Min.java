package com.demo.extract.client;

import java.util.*;
import java.util.stream.Collectors;

public class markovClient30Min {
    private int numStates;                   // 状态数量
    private double[] initialProbabilities;   // 初始概率
    private double[][] transitionMatrix;     // 转移矩阵
    private double[][] emissionMeans;        // 发射分布均值
    private double[][] emissionVars;         // 发射分布方差
    private static final double MIN_PROB = 1e-10; // 最小概率值，防止参数收敛到0

    /**
     * 构造函数
     * @param numStates 状态数量
     */
    public markovClient30Min(int numStates) {
        this.numStates = numStates;
        this.initialProbabilities = new double[numStates];
        this.transitionMatrix = new double[numStates][numStates];
        this.emissionMeans = new double[numStates][1];
        this.emissionVars = new double[numStates][1];

        // 初始化参数（可以根据30分钟数据分布特征调整）
        initializeParams();
    }

    /**
     * 初始化参数（适用于30分钟数据的默认初始化）
     */
    private void initializeParams() {
        // 均匀初始化初始概率
        Arrays.fill(initialProbabilities, 1.0 / numStates);

        // 初始化转移矩阵（轻微偏向自转移，适应30分钟数据的持续性特征）
        for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < numStates; j++) {
                if (i == j) {
                    transitionMatrix[i][j] = 0.7; // 自转移概率较高
                } else {
                    transitionMatrix[i][j] = 0.3 / (numStates - 1);
                }
            }
        }

        // 初始化发射分布（假设30分钟收益数据的均值范围更大）
        Random random = new Random(42); // 设置随机种子以保证可重复性
        for (int i = 0; i < numStates; i++) {
            // 30分钟收益的均值范围可能比5分钟更大，这里扩大范围
            emissionMeans[i][0] = (random.nextDouble() - 0.5) * 0.05; // 范围 -0.025 到 0.025
            emissionVars[i][0] = 0.0001 + random.nextDouble() * 0.001; // 方差稍大
        }
    }

    /**
     * 训练模型
     * @param observations 观测序列（30分钟收益数据）
     * @param maxIterations 最大迭代次数
     */
    public void train(double[] observations, int maxIterations) {
        baumWelch(observations, maxIterations);
    }

    /**
     * 识别状态
     * @param observations 观测序列
     * @return 状态序列
     */
    public int[] identifyStates(double[] observations) {
        return viterbi(observations);
    }

    /**
     * 获取Gamma矩阵
     * @param observations 观测序列
     * @return Gamma矩阵
     */
    public double[][] getGamma(double[] observations) {
        int T = observations.length;
        double[][] alpha = new double[T][numStates];
        double[][] beta = new double[T][numStates];
        double[][] gamma = new double[T][numStates];
        final double LOG_ZERO = Math.log(1e-300);

        // 前向算法（使用对数概率）
        for (int i = 0; i < numStates; i++) {
            double emissionProb = emissionProbability(observations[0], i);
            alpha[0][i] = Math.log(initialProbabilities[i]) + (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO);
        }

        for (int t = 1; t < T; t++) {
            for (int j = 0; j < numStates; j++) {
                double maxVal = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < numStates; i++) {
                    double transProb = transitionMatrix[i][j];
                    double temp = alpha[t-1][i] + (transProb > 0 ? Math.log(transProb) : LOG_ZERO);
                    if (temp > maxVal) {
                        maxVal = temp;
                    }
                }

                double sum = 0;
                for (int i = 0; i < numStates; i++) {
                    double transProb = transitionMatrix[i][j];
                    sum += Math.exp(alpha[t-1][i] + (transProb > 0 ? Math.log(transProb) : LOG_ZERO) - maxVal);
                }

                double emissionProb = emissionProbability(observations[t], j);
                alpha[t][j] = maxVal + Math.log(sum) + (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO);
            }
        }

        // 后向算法（使用对数概率）
        for (int i = 0; i < numStates; i++) {
            beta[T-1][i] = 0.0; // log(1.0) = 0
        }

        for (int t = T-2; t >= 0; t--) {
            for (int i = 0; i < numStates; i++) {
                double maxVal = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < numStates; j++) {
                    double transProb = transitionMatrix[i][j];
                    double emissionProb = emissionProbability(observations[t+1], j);
                    double temp = (transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                 (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                 beta[t+1][j];
                    if (temp > maxVal) {
                        maxVal = temp;
                    }
                }

                double sum = 0;
                for (int j = 0; j < numStates; j++) {
                    double transProb = transitionMatrix[i][j];
                    double emissionProb = emissionProbability(observations[t+1], j);
                    sum += Math.exp((transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                   (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                   beta[t+1][j] - maxVal);
                }

                beta[t][i] = maxVal + Math.log(sum);
            }
        }

        // 计算log似然度
        double logLikelihood = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numStates; i++) {
            logLikelihood = Math.max(logLikelihood, alpha[T-1][i]);
        }
        double sumLikelihood = 0;
        for (int i = 0; i < numStates; i++) {
            sumLikelihood += Math.exp(alpha[T-1][i] - logLikelihood);
        }
        logLikelihood += Math.log(sumLikelihood);

        // 计算gamma
        for (int t = 0; t < T; t++) {
            double maxGamma = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < numStates; i++) {
                double gammaVal = alpha[t][i] + beta[t][i] - logLikelihood;
                if (gammaVal > maxGamma) {
                    maxGamma = gammaVal;
                }
            }
            double sumGamma = 0;
            for (int i = 0; i < numStates; i++) {
                gamma[t][i] = Math.exp(alpha[t][i] + beta[t][i] - logLikelihood - maxGamma);
                sumGamma += gamma[t][i];
            }
            // 归一化gamma
            for (int i = 0; i < numStates; i++) {
                gamma[t][i] /= sumGamma;
                gamma[t][i] = Math.max(gamma[t][i], MIN_PROB);
            }
        }

        return gamma;
    }

    /**
     * 分析状态持续性
     * @param states 状态序列
     * @return 包含各状态持续时间和转移概率的Map
     */
    public Map<String, Double> analyzeStatePersistence(int[] states) {
        Map<String, Double> result = new HashMap<>();
        if (states == null || states.length < 2) {
            return result;
        }

        // 计算各状态持续时间
        Map<Integer, List<Integer>> stateDurations = new HashMap<>();
        int currentState = states[0];
        int currentDuration = 1;

        for (int i = 1; i < states.length; i++) {
            if (states[i] == currentState) {
                currentDuration++;
            } else {
                stateDurations.computeIfAbsent(currentState, k -> new ArrayList<>()).add(currentDuration);
                currentState = states[i];
                currentDuration = 1;
            }
        }
        // 添加最后一个状态的持续时间
        stateDurations.computeIfAbsent(currentState, k -> new ArrayList<>()).add(currentDuration);

        // 计算平均持续时间
        for (Map.Entry<Integer, List<Integer>> entry : stateDurations.entrySet()) {
            int state = entry.getKey();
            List<Integer> durations = entry.getValue();
            double avgDuration = durations.stream().mapToInt(Integer::intValue).average().orElse(0);
            result.put("state_" + state + "_avg_duration", avgDuration);
        }

        // 计算状态转移概率
        int[][] transitionCounts = new int[numStates][numStates];
        int[] stateCounts = new int[numStates];

        for (int i = 0; i < states.length - 1; i++) {
            int fromState = states[i];
            int toState = states[i+1];
            transitionCounts[fromState][toState]++;
            stateCounts[fromState]++;
        }

        for (int i = 0; i < numStates; i++) {
            if (stateCounts[i] > 0) {
                for (int j = 0; j < numStates; j++) {
                    result.put("transition_" + i + "_to_" + j,
                            (double) transitionCounts[i][j] / stateCounts[i]);
                }
            }
        }

        return result;
    }

    /**
     * 验证持续性分析的准确性
     * @param actualStates 实际状态序列
     * @param predictedStates 预测状态序列
     * @return 准确率
     */
    public double validatePersistence(int[] actualStates, int[] predictedStates) {
        if (actualStates == null || predictedStates == null ||
                actualStates.length != predictedStates.length) {
            return 0.0;
        }

        int correct = 0;
        for (int i = 0; i < actualStates.length; i++) {
            if (actualStates[i] == predictedStates[i]) {
                correct++;
            }
        }

        return (double) correct / actualStates.length;
    }

    // Baum-Welch算法实现（改进版，使用对数概率避免数值下溢）
    private void baumWelch(double[] observations, int maxIterations) {
        int T = observations.length;
        double[][] alpha = new double[T][numStates];
        double[][] beta = new double[T][numStates];
        double[][][] xi = new double[T-1][numStates][numStates]; // 状态转移概率
        double[][] gamma = new double[T][numStates]; // 状态概率
        final double LOG_ZERO = Math.log(1e-300);
        final double MIN_PROB = 1e-10; // 最小概率值，防止参数收敛到0
        final double LEARNING_RATE = 0.03; // 学习率，30分钟数据可能需要更小的学习率以保证稳定性

        for (int iter = 0; iter < maxIterations; iter++) {
            // 前向算法（使用对数概率）
            for (int i = 0; i < numStates; i++) {
                double emissionProb = emissionProbability(observations[0], i);
                alpha[0][i] = Math.log(initialProbabilities[i]) + (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO);
            }

            for (int t = 1; t < T; t++) {
                for (int j = 0; j < numStates; j++) {
                    double maxVal = Double.NEGATIVE_INFINITY;
                    for (int i = 0; i < numStates; i++) {
                        double transProb = transitionMatrix[i][j];
                        double temp = alpha[t-1][i] + (transProb > 0 ? Math.log(transProb) : LOG_ZERO);
                        if (temp > maxVal) {
                            maxVal = temp;
                        }
                    }

                    double sum = 0;
                    for (int i = 0; i < numStates; i++) {
                        double transProb = transitionMatrix[i][j];
                        sum += Math.exp(alpha[t-1][i] + (transProb > 0 ? Math.log(transProb) : LOG_ZERO) - maxVal);
                    }

                    double emissionProb = emissionProbability(observations[t], j);
                    alpha[t][j] = maxVal + Math.log(sum) + (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO);
                }
            }

            // 后向算法（使用对数概率）
            for (int i = 0; i < numStates; i++) {
                beta[T-1][i] = 0.0; // log(1.0) = 0
            }

            for (int t = T-2; t >= 0; t--) {
                for (int i = 0; i < numStates; i++) {
                    double maxVal = Double.NEGATIVE_INFINITY;
                    for (int j = 0; j < numStates; j++) {
                        double transProb = transitionMatrix[i][j];
                        double emissionProb = emissionProbability(observations[t+1], j);
                        double temp = (transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                     (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                     beta[t+1][j];
                        if (temp > maxVal) {
                            maxVal = temp;
                        }
                    }

                    double sum = 0;
                    for (int j = 0; j < numStates; j++) {
                        double transProb = transitionMatrix[i][j];
                        double emissionProb = emissionProbability(observations[t+1], j);
                        sum += Math.exp((transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                       (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                       beta[t+1][j] - maxVal);
                    }

                    beta[t][i] = maxVal + Math.log(sum);
                }
            }

            // 计算gamma和xi
            // 首先计算全局归一化因子
            double logLikelihood = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < numStates; i++) {
                logLikelihood = Math.max(logLikelihood, alpha[T-1][i]);
            }
            double sumLikelihood = 0;
            for (int i = 0; i < numStates; i++) {
                sumLikelihood += Math.exp(alpha[T-1][i] - logLikelihood);
            }
            logLikelihood += Math.log(sumLikelihood);

            for (int t = 0; t < T; t++) {
                double maxGamma = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < numStates; i++) {
                    double gammaVal = alpha[t][i] + beta[t][i] - logLikelihood;
                    if (gammaVal > maxGamma) {
                        maxGamma = gammaVal;
                    }
                }
                double sumGamma = 0;
                for (int i = 0; i < numStates; i++) {
                    gamma[t][i] = Math.exp(alpha[t][i] + beta[t][i] - logLikelihood - maxGamma);
                    sumGamma += gamma[t][i];
                }
                // 归一化gamma
                for (int i = 0; i < numStates; i++) {
                    gamma[t][i] /= sumGamma;
                }
            }

            // 计算xi
            for (int t = 0; t < T-1; t++) {
                double maxXi = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < numStates; i++) {
                    for (int j = 0; j < numStates; j++) {
                        double transProb = transitionMatrix[i][j];
                        double emissionProb = emissionProbability(observations[t+1], j);
                        double xiVal = alpha[t][i] + 
                                      (transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                      (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                      beta[t+1][j] - logLikelihood;
                        if (xiVal > maxXi) {
                            maxXi = xiVal;
                        }
                    }
                }

                double sumXi = 0;
                for (int i = 0; i < numStates; i++) {
                    for (int j = 0; j < numStates; j++) {
                        double transProb = transitionMatrix[i][j];
                        double emissionProb = emissionProbability(observations[t+1], j);
                        xi[t][i][j] = Math.exp(alpha[t][i] + 
                                              (transProb > 0 ? Math.log(transProb) : LOG_ZERO) + 
                                              (emissionProb > 0 ? Math.log(emissionProb) : LOG_ZERO) + 
                                              beta[t+1][j] - logLikelihood - maxXi);
                        sumXi += xi[t][i][j];
                    }
                }

                // 归一化xi
                for (int i = 0; i < numStates; i++) {
                    for (int j = 0; j < numStates; j++) {
                        xi[t][i][j] /= sumXi;
                    }
                }
            }

            // 更新参数
            // 1. 初始概率（使用学习率）
            double[] newInitialProbabilities = new double[numStates];
            for (int i = 0; i < numStates; i++) {
                newInitialProbabilities[i] = Math.max(gamma[0][i], MIN_PROB);
            }
            // 归一化新的初始概率
            double sumInitProb = 0;
            for (int i = 0; i < numStates; i++) {
                sumInitProb += newInitialProbabilities[i];
            }
            for (int i = 0; i < numStates; i++) {
                newInitialProbabilities[i] /= sumInitProb;
            }
            // 使用学习率更新初始概率
            for (int i = 0; i < numStates; i++) {
                initialProbabilities[i] = (1 - LEARNING_RATE) * initialProbabilities[i] + LEARNING_RATE * newInitialProbabilities[i];
                initialProbabilities[i] = Math.max(initialProbabilities[i], MIN_PROB);
            }
            // 再次归一化
            sumInitProb = 0;
            for (int i = 0; i < numStates; i++) {
                sumInitProb += initialProbabilities[i];
            }
            for (int i = 0; i < numStates; i++) {
                initialProbabilities[i] /= sumInitProb;
            }

            // 2. 转移矩阵（使用学习率）
            double[][] newTransitionMatrix = new double[numStates][numStates];
            for (int i = 0; i < numStates; i++) {
                double sumGammaI = 0;
                for (int t = 0; t < T-1; t++) {
                    sumGammaI += gamma[t][i];
                }
                for (int j = 0; j < numStates; j++) {
                    double sumXiIJ = 0;
                    for (int t = 0; t < T-1; t++) {
                        sumXiIJ += xi[t][i][j];
                    }
                    newTransitionMatrix[i][j] = Math.max(sumXiIJ / sumGammaI, MIN_PROB);
                }

                // 归一化新的转移矩阵的行
                double sumTransRow = 0;
                for (int j = 0; j < numStates; j++) {
                    sumTransRow += newTransitionMatrix[i][j];
                }
                for (int j = 0; j < numStates; j++) {
                    newTransitionMatrix[i][j] /= sumTransRow;
                }
            }
            // 使用学习率更新转移矩阵
            for (int i = 0; i < numStates; i++) {
                for (int j = 0; j < numStates; j++) {
                    transitionMatrix[i][j] = (1 - LEARNING_RATE) * transitionMatrix[i][j] + LEARNING_RATE * newTransitionMatrix[i][j];
                    transitionMatrix[i][j] = Math.max(transitionMatrix[i][j], MIN_PROB);
                }
                // 再次归一化行
                double sumTransRow = 0;
                for (int j = 0; j < numStates; j++) {
                    sumTransRow += transitionMatrix[i][j];
                }
                for (int j = 0; j < numStates; j++) {
                    transitionMatrix[i][j] /= sumTransRow;
                }
            }

            // 3. 发射分布参数（高斯分布，使用学习率）
            double[][] newEmissionMeans = new double[numStates][1];
            double[][] newEmissionVars = new double[numStates][1];
            for (int j = 0; j < numStates; j++) {
                double sumGammaJ = 0;
                double sumObs = 0;
                double sumObsSq = 0;

                for (int t = 0; t < T; t++) {
                    sumGammaJ += gamma[t][j];
                    sumObs += gamma[t][j] * observations[t];
                    sumObsSq += gamma[t][j] * observations[t] * observations[t];
                }

                newEmissionMeans[j][0] = sumObs / sumGammaJ;
                newEmissionVars[j][0] = Math.max((sumObsSq / sumGammaJ) - Math.pow(newEmissionMeans[j][0], 2), 0.001);
            }
            // 使用学习率更新发射分布参数
            for (int j = 0; j < numStates; j++) {
                emissionMeans[j][0] = (1 - LEARNING_RATE) * emissionMeans[j][0] + LEARNING_RATE * newEmissionMeans[j][0];
                emissionVars[j][0] = (1 - LEARNING_RATE) * emissionVars[j][0] + LEARNING_RATE * newEmissionVars[j][0];
                emissionVars[j][0] = Math.max(emissionVars[j][0], 0.001);
            }

            // 添加调试信息 - 每100次迭代打印一次参数
            if (iter % 100 == 0) {
                System.out.println("迭代次数: " + iter);
                System.out.println("初始概率: " + Arrays.toString(initialProbabilities));
                System.out.println("转移矩阵:");
                for (int i = 0; i < numStates; i++) {
                    System.out.println(Arrays.toString(transitionMatrix[i]));
                }
                System.out.println("发射分布均值:");
                for (int i = 0; i < numStates; i++) {
                    System.out.println("状态 " + i + ": " + emissionMeans[i][0]);
                }
                System.out.println("发射分布方差:");
                for (int i = 0; i < numStates; i++) {
                    System.out.println("状态 " + i + ": " + emissionVars[i][0]);
                }
                // 打印gamma的前5个时间步
                System.out.println("Gamma前5个时间步:");
                for (int t = 0; t < Math.min(5, T); t++) {
                    System.out.println("时间步 " + t + ": " + Arrays.toString(gamma[t]));
                }
                System.out.println("----------------------------------------");
            }
        }
    }

    // Viterbi算法实现（改进版，使用对数概率避免数值下溢，直接支持一维数组）
    private int[] viterbi(double[] observations) {
        int T = observations.length;
        int[] states = new int[T];
        double[][] delta = new double[T][numStates];
        int[][] psi = new int[T][numStates];

        // 初始化（使用对数概率）
        for (int i = 0; i < numStates; i++) {
            double emissionProb = emissionProbability(observations[0], i);
            // 避免log(0)问题
            if (emissionProb == 0) emissionProb = 1e-300;
            if (initialProbabilities[i] == 0) initialProbabilities[i] = 1e-300;
            
            delta[0][i] = Math.log(initialProbabilities[i]) + Math.log(emissionProb);
            psi[0][i] = -1; // 使用-1表示初始状态
        }

        // 递推
        for (int t = 1; t < T; t++) {
            for (int j = 0; j < numStates; j++) {
                double maxDelta = Double.NEGATIVE_INFINITY;
                int maxPsi = 0;

                for (int i = 0; i < numStates; i++) {
                    // 避免log(0)问题
                    if (transitionMatrix[i][j] == 0) transitionMatrix[i][j] = 1e-300;
                    
                    double currentDelta = delta[t-1][i] + Math.log(transitionMatrix[i][j]);
                    if (currentDelta > maxDelta) {
                        maxDelta = currentDelta;
                        maxPsi = i;
                    }
                }

                double emissionProb = emissionProbability(observations[t], j);
                // 避免log(0)问题
                if (emissionProb == 0) emissionProb = 1e-300;
                
                delta[t][j] = maxDelta + Math.log(emissionProb);
                psi[t][j] = maxPsi;
            }
        }

        // 终止
        double maxDelta = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numStates; i++) {
            if (delta[T-1][i] > maxDelta) {
                maxDelta = delta[T-1][i];
                states[T-1] = i;
            }
        }

        // 回溯
        for (int t = T-2; t >= 0; t--) {
            states[t] = psi[t+1][states[t+1]];
        }

        return states;
    }

    // 计算发射概率（高斯分布）
    private double emissionProbability(double observation, int state) {
        double mean = emissionMeans[state][0];
        double var = emissionVars[state][0];

        return (1.0 / Math.sqrt(2 * Math.PI * var)) *
               Math.exp(-Math.pow(observation - mean, 2) / (2 * var));
    }

    // 获取初始概率
    public double[] getInitialProbabilities() {
        return initialProbabilities;
    }

    // 获取转移矩阵
    public double[][] getTransitionMatrix() {
        return transitionMatrix;
    }

    // 获取发射分布均值
    public double[][] getEmissionMeans() {
        return emissionMeans;
    }

    // 获取发射分布方差
    public double[][] getEmissionVars() {
        return emissionVars;
    }

    // 获取状态数量
    public int getNumStates() {
        return numStates;
    }
}