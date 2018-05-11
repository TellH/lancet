package me.ele.lancet.weaver.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by tlh on 2018/3/15.
 */

public class SpiModel {
    private Map<String, String> spiServices;
    private String injectClassName;
    private Map<String, Boolean> serviceClassExistRecords; // 检查SPI接口和实现类是否存在

    public SpiModel(Map<String, String> spiServices, String injectClassName) {
        this.spiServices = spiServices;
        this.injectClassName = injectClassName;
        serviceClassExistRecords = new HashMap<>();
        for (Map.Entry<String, String> entry : spiServices.entrySet()) {
            serviceClassExistRecords.put(entry.getKey(), false);
            String configFile = entry.getValue();
            String[] implementClasses = configFile.split("\n");
            for (String implementClass : implementClasses) {
                String className = fromConfig(implementClass);
                if (className != null && !className.isEmpty()) {
                    serviceClassExistRecords.put(className, false);
                }
            }
        }
        serviceClassExistRecords.put(injectClassName.replace("/", "."), false);
    }

    public Map<String, String> getSpiServices() {
        return spiServices;
    }

    public void setSpiServices(Map<String, String> spiServices) {
        this.spiServices = spiServices;
    }

    public String getInjectClassName() {
        return injectClassName;
    }

    public void setInjectClassName(String injectClassName) {
        this.injectClassName = injectClassName;
    }

    public static String fromConfig(String line) {
        if (line != null && !line.isEmpty()) {
            String[] segments = line.split(":");
            return segments[0];
        } else {
            return null;
        }
    }

    public void recordSpiService(String className) {
        className = className.replaceAll("/", ".");
        if (!serviceClassExistRecords.containsKey(className)) {
            return;
        }
        serviceClassExistRecords.put(className, true);
    }

    public List<String> getNotExistSpiClass() {
        return serviceClassExistRecords.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
