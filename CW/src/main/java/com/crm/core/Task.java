package com.crm.core;

import java.time.LocalDateTime;
import java.util.UUID;

public class Task {
    private String id;
    private String customerId;
    private String description;
    private LocalDateTime dueDate;
    private boolean completed;
    private Priority priority; // New field for task priority
    private LocalDateTime reminderTime; // New field for specific reminder time

    // Enum for task priorities
    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public Task(String customerId, String description, LocalDateTime dueDate) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = false;
        this.priority = Priority.MEDIUM; // Default priority
        this.reminderTime = dueDate.minusHours(24); // Default reminder: 24h before due
    }

    // New constructor with priority
    public Task(String customerId, String description, LocalDateTime dueDate, Priority priority) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = false;
        this.priority = priority;

        // Set reminder time based on priority
        switch (priority) {
            case HIGH:
                this.reminderTime = dueDate.minusHours(48); // 48h for high priority
                break;
            case LOW:
                this.reminderTime = dueDate.minusHours(12); // 12h for low priority
                break;
            default:
                this.reminderTime = dueDate.minusHours(24); // 24h for medium priority
        }
    }

    // Getters and setters
    public String getId() { return id; }

    public String getCustomerId() { return customerId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                ", description='" + description + '\'' +
                ", dueDate=" + dueDate +
                ", completed=" + completed +
                ", priority=" + priority +
                ", reminderTime=" + reminderTime +
                '}';
    }
}