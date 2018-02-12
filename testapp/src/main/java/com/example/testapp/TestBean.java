package com.example.testapp;

/**
 * Created by tlh on 2018/2/8.
 */

public class TestBean implements ITestB {
    public String name;

    public TestBean(String name) {
        this.name = name;
    }

    @Override
    public void a() {

    }

    @Override
    public void b() {

    }

    @Override
    public String toString() {
        return "TestBean{" +
                "name='" + name + '\'' +
                '}';
    }
}
