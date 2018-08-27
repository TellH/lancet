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

    private final Set<Exception> errorLog = new HashSet<>();

    public void throwException(IllegalStateException e) {
        errorLog.add(e);
        throw e;
    }

    public void addErrorLog(String log) {
        errorLog.add(new Exception(log));
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
        StringBuilder result = new StringBuilder();
        Iterator<Exception> it = errorLog.iterator();
        while (it.hasNext()) {
            Exception e = it.next();
            if (e != null) {
                result.append(e.getMessage()).append("\n");
            }
        }
        if (!TextUtils.isEmpty(result.toString())) {
            result.insert(0, "something error! see /build/lancet/log_out.log\n");
        }
        return result.toString();
    }
}
