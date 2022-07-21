package com.ibm.aiops.connectors.template;

public class Configuration {
    private int cpuThreshold = 80;
    private int severityLevel = 6;
    private int expirySeconds = 60;
    private boolean enableCPUHeavyWorkload = false;
    private boolean enableGatherMetrics = true;
    private boolean isLiveData = false;
    private String historicStartDate;
    private String historicEndDate;
    private String metricName = "Usage%";
    private int numCPUWorkloadThreads = 1;
    private boolean enableTopologySampleGeneration = false;

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

    public boolean getEnableGatherMetrics() {
        return enableGatherMetrics;
    }

    public void setEnableGatherMetrics(boolean enableGatherMetrics) {
        this.enableGatherMetrics = enableGatherMetrics;
    }

    public boolean getIsLiveData() {
        return isLiveData;
    }

    public void setIsLiveData(boolean isLiveData) {
        this.isLiveData = isLiveData;
    }

    public String getHistoricStartDate() {
        return historicStartDate;
    }

    public void setHistoricStartDate(String historicStartDate) {
        this.historicStartDate = historicStartDate;
    }

    public String getHistoricEndDate() {
        return historicEndDate;
    }

    public void setHistoricEndDate(String historicEndDate) {
        this.historicEndDate = historicEndDate;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
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

    public boolean getEnableTopologySampleGeneration() {
        return enableTopologySampleGeneration;
    }

    public void setEnableTopologySampleGeneration(boolean enableTopologySampleGeneration) {
        this.enableTopologySampleGeneration = enableTopologySampleGeneration;
    }
}
