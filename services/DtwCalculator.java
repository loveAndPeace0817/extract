package com.demo.extract.services;


import com.demo.extract.DTO.OrderTimeSeries;

import java.util.Arrays;


public class DtwCalculator {
    public static double compute(OrderTimeSeries s1, OrderTimeSeries s2) {
        return compute(s1.getValues(), s2.getValues());
    }

    public static double computePartial(OrderTimeSeries s1, OrderTimeSeries s2, double ratio,Integer type) {
        int splitPointS1, splitPointS2;
        double compute;
        switch (type){
            case 1:
                splitPointS1 = (int)(s1.getValues().length * ratio);
                splitPointS2 = (int)(s2.getValues().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getValues(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getValues(), 0, splitPointS2) );
                break;
            case 2:
                splitPointS1 = (int)(s1.getClose().length * ratio);
                splitPointS2 = (int)(s2.getClose().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getClose(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getClose(), 0, splitPointS2) );
                break;
            case 3:
                splitPointS1 = (int)(s1.getOpen().length * ratio);
                splitPointS2 = (int)(s2.getOpen().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getOpen(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getOpen(), 0, splitPointS2) );
                break;
            case 4:
                splitPointS1 = (int)(s1.getAtr().length * ratio);
                splitPointS2 = (int)(s2.getAtr().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getAtr(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getAtr(), 0, splitPointS2) );
                break;
            case 5:
                splitPointS1 = (int)(s1.getTH().length * ratio);
                splitPointS2 = (int)(s2.getTH().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getTH(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getTH(), 0, splitPointS2) );
                break;
            case 6:
                splitPointS1 = (int)(s1.getTL().length * ratio);
                splitPointS2 = (int)(s2.getTL().length * ratio);
                compute = compute(
                        Arrays.copyOfRange(s1.getTL(), 0, splitPointS1),
                        Arrays.copyOfRange(s2.getTL(), 0, splitPointS2) );
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        return compute;
    }

    public static double computePartialClose(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        int splitPointS1 = (int)(s1.getClose().length * ratio);
        int splitPointS2 = (int)(s2.getClose().length * ratio);
        double compute = compute(
                Arrays.copyOfRange(s1.getClose(), 0, splitPointS1),
                Arrays.copyOfRange(s2.getClose(), 0, splitPointS2)
        );
        return compute;
    }

    public static double computePartialOpen(OrderTimeSeries s1, OrderTimeSeries s2, double ratio) {
        int splitPointS1 = (int)(s1.getOpen().length * ratio);
        int splitPointS2 = (int)(s2.getOpen().length * ratio);
        double compute = compute(
                Arrays.copyOfRange(s1.getOpen(), 0, splitPointS1),
                Arrays.copyOfRange(s2.getOpen(), 0, splitPointS2)
        );
        return compute;
    }

    private static double compute(double[] s1, double[] s2) {
        int n = s1.length, m = s2.length;
        double[][] dtw = new double[n+1][m+1];

        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                if (i == 0 && j == 0) {
                    dtw[i][j] = 0;
                } else if (i == 0 || j == 0) {
                    dtw[i][j] = Double.POSITIVE_INFINITY;
                } else {
                    double cost = Math.pow(s1[i-1] - s2[j-1], 2);
                    dtw[i][j] = cost + min(
                            dtw[i-1][j],
                            dtw[i][j-1],
                            dtw[i-1][j-1]
                    );
                }
            }
        }
        return Math.sqrt(dtw[n][m]);
    }

    private static double min(double a, double b, double c) {
        return Math.min(a, Math.min(b, c));
    }
}