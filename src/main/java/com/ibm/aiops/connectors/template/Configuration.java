package com.ibm.aiops.connectors.template;

public class Configuration {
    private int cpuThreshold = 80;
    private int severityLevel = 6;
    private int expirySeconds = 60;
    private boolean enableCPUHeavyWorkload = false;
    private int numCPUWorkloadThreads = 1;

    public Configuration() {
    }

    public int getNumCPUWorkloadThreads() {
        return numCPUWorkloadThreads;
    }

    public void setNumCPUWorkloadThreads(int numCPUWorkloadThreads) {
        this.numCPUWorkloadThreads = numCPUWorkloadThreads;
    }

    public boolean getEnableCPUHeavyWorkload() {
        return enableCPUHeavyWorkload;
    }

    public void setEnableCPUHeavyWorkload(boolean enableCPUHeavyWorkload) {
        this.enableCPUHeavyWorkload = enableCPUHeavyWorkload;
    }

    public int getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public void setCpuThreshold(int cpuThreshold) {
        this.cpuThreshold = cpuThreshold;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(int severityLevel) {
        this.severityLevel = severityLevel;
    }
}
