package me.ele.lancet.plugin;

/**
 * Created by tlh on 2018/3/15.
 */

public class SpiExtension {
    private String[] spiServiceDirs;
    private String injectClassName;
    private String proguardFilePath;

    public String[] getSpiServiceDirs() {
        return spiServiceDirs;
    }

    public void setSpiServiceDirs(String... spiServiceDirs) {
        this.spiServiceDirs = spiServiceDirs;
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
