package me.ele.lancet.plugin;

/**
 * Created by tlh on 2018/3/15.
 */

public class SpiExtension {
    private String spiServicePath;
    private String injectClassName;
    private String proguardFilePath;

    public String getSpiServicePath() {
        return spiServicePath;
    }

    public void setSpiServicePath(String spiServicePath) {
        this.spiServicePath = spiServicePath;
    }

    public String getInjectClassName() {
        return injectClassName;
    }

    public void setInjectClassName(String injectClassName) {
        this.injectClassName = injectClassName;
    }

    public String getProguardFilePath() {
        return proguardFilePath;
    }

    public void setProguardFilePath(String proguardFilePath) {
        this.proguardFilePath = proguardFilePath;
    }
}
