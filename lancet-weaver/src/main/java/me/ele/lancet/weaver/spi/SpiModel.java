package me.ele.lancet.weaver.spi;

import java.util.Map;

/**
 * Created by tlh on 2018/3/15.
 */

public class SpiModel {
    private Map<String, String> spiServices;
    private String spiServicePath;
    private String injectClassName;

    public SpiModel(Map<String, String> spiServices, String spiServicePath, String injectClassName) {
        this.spiServices = spiServices;
        this.spiServicePath = spiServicePath;
        this.injectClassName = injectClassName;
    }

    public Map<String, String> getSpiServices() {
        return spiServices;
    }

    public void setSpiServices(Map<String, String> spiServices) {
        this.spiServices = spiServices;
    }

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
}
