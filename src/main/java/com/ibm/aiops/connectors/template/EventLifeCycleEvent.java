package com.ibm.aiops.connectors.template;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(Include.NON_NULL)
public class EventLifeCycleEvent {

    public static final ObjectMapper mapper = new ObjectMapper();
    static {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(df);
    }

    public static class Type {
        private String eventType;
        private String classification;
        private String condition;

        public Type() {
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
    }

    public static class Link {
        private String linkType;
        private String name;
        private String description;
        private String url;

        public Link() {
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLinkType() {
            return linkType;
        }

        public void setLinkType(String linkType) {
            this.linkType = linkType;
        }
    }

    private String id;
    private Date occurrenceTime;
    private String summary;
    private Integer severity;
    private Type type;
    private Map<String, Object> sender;
    private Map<String, Object> resource;
    private Integer expirySeconds;
    private Link[] links;
    private Map<String, String> details;

    public EventLifeCycleEvent() {
    }

    public String toJSON() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public Link[] getLinks() {
        return links;
    }

    public void setLinks(Link[] links) {
        this.links = links;
    }

    public Integer getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(Integer expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public Map<String, Object> getResource() {
        return resource;
    }

    public void setResource(Map<String, Object> resource) {
        this.resource = resource;
    }

    public Map<String, Object> getSender() {
        return sender;
    }

    public void setSender(Map<String, Object> sender) {
        this.sender = sender;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        if (severity > 6) {
            this.severity = 6;
        } else if (severity < 1) {
            this.severity = 1;
        } else {
            this.severity = severity;
        }
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Date getOccurrenceTime() {
        return occurrenceTime;
    }

    public void setOccurrenceTime(Date occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }
}
