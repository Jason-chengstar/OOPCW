package com.crm.core;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class Communication {
    private String id;
    private String customerId;
    private String type; // phone, email, meeting
    private LocalDateTime timestamp;
    private String notes;
    private List<String> tags;

    public Communication(String customerId, String type, String notes) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.notes = notes;
        this.tags = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }

    public String getCustomerId() { return customerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getTags() { return tags; }
    public void addTag(String tag) { this.tags.add(tag); }
    public void removeTag(String tag) { this.tags.remove(tag); }

    @Override
    public String toString() {
        return "Communication{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", notes='" + notes + '\'' +
                ", tags=" + tags +
                '}';
    }
}