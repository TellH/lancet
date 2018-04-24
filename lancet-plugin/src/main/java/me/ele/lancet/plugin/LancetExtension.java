
package me.ele.lancet.plugin;

import com.google.common.base.Strings;

import me.ele.lancet.weaver.internal.log.Log;

import java.io.File;
import java.util.Objects;

public class LancetExtension {
    private Log.Level level = Log.Level.INFO;
    private String fileName = null;
    private boolean incremental = true;
    private boolean checkUselessProxyMethodEnable = false;
    private boolean checkMethodNotFoundEnable = true;
    private boolean strictMode;
    private SpiExtension spiExtension;
    private boolean shouldDebugEnableCheck;

    public void logLevel(Log.Level level) {
        this.level = Objects.requireNonNull(level, "Log.Level is null");
    }

    public void logLevel(int level) {
        this.level = Log.Level.values()[level];
    }

    public void logLevel(String logStr) {
        logLevel(strToLog(logStr));
    }

    private static Log.Level strToLog(String logStr) {
        logStr = logStr.toLowerCase();
        switch (logStr) {
            case "d":
            case "debug":
                return Log.Level.DEBUG;
            case "i":
            case "info":
                return Log.Level.INFO;
            case "w":
            case "warn":
                return Log.Level.WARN;
            case "e":
            case "error":
                return Log.Level.ERROR;
            default:
                throw new IllegalArgumentException("wrong log string: " + logStr);
        }
    }

    public void useFileLog(String fileName) {
        if (Strings.isNullOrEmpty(fileName) || fileName.contains(File.separator)) {
            throw new IllegalArgumentException("File name is illegal: " + fileName);
        }
        this.fileName = fileName;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean getIncremental() {
        return this.incremental;
    }

    public void incremental(boolean incremental) {
        this.incremental = incremental;
    }

    public Log.Level getLogLevel() {
        return level;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isCheckUselessProxyMethodEnable() {
        return checkUselessProxyMethodEnable;
    }

    public void setCheckUselessProxyMethodEnable(boolean enable) {
        checkUselessProxyMethodEnable = enable;
    }

    public boolean isCheckMethodNotFoundEnable() {
        return checkMethodNotFoundEnable;
    }

    public void setCheckMethodNotFoundEnable(boolean checkMethodNotFoundEnable) {
        this.checkMethodNotFoundEnable = checkMethodNotFoundEnable;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public SpiExtension getSpiExtension() {
        return spiExtension;
    }

    public void setSpiExtension(SpiExtension spiExtension) {
        this.spiExtension = spiExtension;
    }

    public boolean isShouldDebugEnableCheck() {
        return shouldDebugEnableCheck;
    }

    public void setShouldDebugEnableCheck(boolean shouldDebugEnableCheck) {
        this.shouldDebugEnableCheck = shouldDebugEnableCheck;
    }
}