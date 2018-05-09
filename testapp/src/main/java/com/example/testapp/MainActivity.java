package com.example.testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.example.testapp.proxy.ProxyTest1;
import com.example.testapp.proxy.ProxyTest2;

import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = AopTest.class.getSimpleName();
    private String name = "abc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sayHello();
        TestBean bean = new TestBean("abc");
        System.out.println(bean.hashCode());
        System.out.println(bean.toString());
        ITestB b = new TestBean("abc");
        b.a();
        b.b();
        TestList list = new TestList();
        list.add("haha");
        list.get(0);
        Log.d(TAG, ConfigLoader.getCLASSES().size() + "");

        ProxyTest1.doTest();
        ProxyTest2.doTest();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
        System.out.println("onStop() called");
    }

    private void sayHello() {
        Log.d(TAG, "Hello");
        System.out.println("Hello");
    }

}
