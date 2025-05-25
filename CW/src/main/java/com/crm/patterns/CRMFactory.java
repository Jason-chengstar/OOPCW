package com.crm.patterns;
import com.crm.core.Customer;
import com.crm.core.Communication;
import com.crm.core.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
public class CRMFactory {
    public static Customer createCustomer(String name, String email, String phone, String role) {
        return new Customer(name, email, phone, role);
    }

    public static Customer createCustomer(String name, String email, String phone, String role, String notes) {
        return new Customer(name, email, phone, role, notes);
    }

    public static Communication createCommunication(String customerId, String type, String notes) {
        return new Communication(customerId, type, notes);
    }

    public static Task createTask(String customerId, String description, LocalDateTime dueDate) {
        return new Task(customerId, description, dueDate);
    }

    // New method to create a task with priority
    public static Task createTask(String customerId, String description, LocalDateTime dueDate, Task.Priority priority) {
        return new Task(customerId, description, dueDate, priority);
    }
}