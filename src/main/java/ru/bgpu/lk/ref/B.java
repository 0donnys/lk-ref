package ru.bgpu.lk.ref;

import java.util.List;

public class B {

    @AppConfig
    private static int maxUsers;

    @AppConfig
    private static Integer minUsers;

    @AppConfig
    private static double temperature;

    @AppConfig
    private static boolean enabled;

    @AppConfig
    private static Boolean debugMode;

    @AppConfig
    private static String[] ports;

    @AppConfig
    private static int[] intArray;

    @AppConfig
    private static double[] coefficients;

    @AppConfig
    private static List<String> stringList;

    @AppConfig
    private static List<Integer> integerList;

    @Override
    public String toString() {
        return "B{" +
                "maxUsers=" + maxUsers +
                ", minUsers=" + minUsers +
                ", temperature=" + temperature +
                ", enabled=" + enabled +
                ", debugMode=" + debugMode +
                ", ports=" + java.util.Arrays.toString(ports) +
                ", intArray=" + java.util.Arrays.toString(intArray) +
                ", coefficients=" + java.util.Arrays.toString(coefficients) +
                ", stringList=" + stringList +
                ", integerList=" + integerList +
                '}';
    }
}
