package com.ibm.aiops.connectors.template;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetricManagerMetric {

    public static final String RESOURCE_METRIC_NAME_FIELD = "tempMetricName";
    public static final String RESOURCE_TIMESTAMP_FIELD = "timestamp";
    public static final String RESOURCE_RESOURCE_ID_FIELD = "resourceId";
    public static final String RESOURCE_METRICS_FIELD = "metrics";
    public static final String RESOURCE_GROUP_FIELD = "group";
    public static final String RESOURCE_NODE_FIELD = "node";

    private String id;

    public static final ObjectMapper mapper = new ObjectMapper();
    static {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        mapper.setDateFormat(df);
    }

    public static class Metrics {
        private Double responseTime;

        public Metrics() {
        }

        @Override
        public boolean equals(Object obj) {
            if (this == null || obj == null)
                return this == obj;
            if (!(obj instanceof Metrics))
                return false;
            Metrics other = (Metrics) obj;
            return Objects.equals(this.responseTime, other.responseTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(responseTime);
        }

        public Double getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(Double responseTime) {
            this.responseTime = responseTime;
        }
    }

    public static class Attributes {
        private String group;
        private String node;

        public Attributes() {
        }

        @Override
        public boolean equals(Object obj) {
            if (this == null || obj == null)
                return this == obj;
            if (!(obj instanceof Attributes))
                return false;
            Attributes other = (Attributes) obj;
            return Objects.equals(this.group, other.group) && Objects.equals(this.node, other.node);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, node);
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

    }

    private Long timestamp;
    private String resourceID;
    private Map<String, Double> metrics;
    private Map<String, String> attributes;

    public MetricManagerMetric() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == null || obj == null)
            return this == obj;
        if (!(obj instanceof MetricManagerMetric))
            return false;
        MetricManagerMetric other = (MetricManagerMetric) obj;
        return Objects.equals(this.timestamp, other.timestamp) && Objects.equals(this.resourceID, other.resourceID)
                && Objects.equals(this.metrics, other.metrics) && Objects.equals(this.attributes, other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timestamp, this.resourceID, this.metrics, this.attributes);
    }

    protected boolean datesEqual(Date a, Date b) {
        if (a == null || b == null)
            return a == b;
        // tolerance for up to a second difference (JSON conversion)
        return Math.abs(a.getTime() - b.getTime()) < 1000;
    }

    public String toJSON() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static MetricManagerMetric fromJSON(String value) throws JsonProcessingException {
        return mapper.readValue(value, MetricManagerMetric.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }

    public String getResourceID() {
        return resourceID;
    }

    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}