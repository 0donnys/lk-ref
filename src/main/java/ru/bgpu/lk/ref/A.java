package ru.bgpu.lk.ref;

public class A {

    @AppConfig(value = "hi!")
    private static String name;

    @AppConfig
    static String host;

    @Override
    public String toString() {
        return super.toString() + " {name = " + name + ", host = " + host + "}";
    }
}
