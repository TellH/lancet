package com.example.testapp;

import android.os.Bundle;
import android.util.Log;

import me.ele.lancet.base.Origin;
import me.ele.lancet.base.Scope;
import me.ele.lancet.base.This;
import me.ele.lancet.base.annotations.Insert;
import me.ele.lancet.base.annotations.Proxy;
import me.ele.lancet.base.annotations.TargetClass;

/**
 * Created by tlh on 2017/12/22.
 */

public class AopTest {
    public static final String TAG = AopTest.class.getSimpleName();

    @Insert(value = "onCreate", mayCreateSuper = true)
    @TargetClass(value = "android.support.v7.app.AppCompatActivity", scope = Scope.LEAF)
    protected void hookOnCreate(Bundle savedInstanceState) {
        Log.d(TAG, "hookOnCreate: ");
        System.out.println("hookOnCreate: ");
        Log.d(TAG, This.getField("name").toString());
        This.putField("haha", "name");
        Log.d(TAG, This.getField("name").toString());
        Origin.callVoid();
    }

    @TargetClass(value = "android.support.v7.app.AppCompatActivity", scope = Scope.LEAF)
    @Insert(value = "onStop", mayCreateSuper = true)
    protected void hookOnStop() {
        Log.d(TAG, "hookOnStop: ");
        System.out.println("hookOnStop: ");
        Origin.callVoid();
    }

    @TargetClass(value = "com.example.testapp.MainActivity")
    @Proxy("sayHello")
    private void sayHaha() {
        Log.d(TAG, "Hook Hello");
        System.out.println("Hook Hello");
    }

    @TargetClass(value = "com.example.testapp.proxy.ProxyBean")
    @Proxy("test1")
    public static void test1() {
        Log.d(TAG, "lancet test1");
        Origin.callVoid();
    }

    @TargetClass(value = "com.example.testapp.proxy.ProxyBean")
    @Proxy(value = "test2", globalProxyClass = true)
    public static void test2() {
        Log.d(TAG, "lancet test2");
        Origin.callVoid();
    }

//    @TargetClass(value = "com.example.testapp.MainActivity")
//    @Proxy("sayHelloaa")
//    private void sayHahaa() {
//        Log.d(TAG, "Hook Helloaaa");
//        System.out.println("Hook Helloaaa");
//    }
}
