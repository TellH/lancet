package me.ele.lancet.weaver.internal.util;

/**
 * Created by leo.zhong on 2018/5/7.
 */
public class TextUtils {
    public static boolean isEmpty(CharSequence s) {
        if (s == null) {
            return true;
        } else {
            return s.length() == 0;
        }
    }
}
