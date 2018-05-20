package me.ele.lancet.weaver.internal.exception;

import me.ele.lancet.weaver.internal.util.TextUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ErrorManager {

    private static ErrorManager instance;

    private ErrorManager() {
    }

    public static synchronized ErrorManager getInstance() {
        if (instance == null) {
            instance = new ErrorManager();
        }
        return instance;
    }

    final Set<Exception> errorLog = new HashSet<>();

    public void throwException(IllegalStateException e) {
        errorLog.add(e);
        throw e;
    }

    public void clearErrorSet() {
        errorLog.clear();
    }

    public Set<Exception> getErrorSet() {
        return errorLog;
    }

    public boolean isHasError() {
        return errorLog.size() > 0;
    }

    public String toErrorString() {
        String result = "";
        Iterator<Exception> it = errorLog.iterator();
        while (it.hasNext()) {
            Exception e = it.next();
            if (e != null) {
                result = result + e.getMessage() + "\n";
            }
        }
        if (!TextUtils.isEmpty(result)) {
            result = "something error! see /build/lancet/log_out.log\n" + result;
        }
        return result;
    }
}
