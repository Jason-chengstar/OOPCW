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
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Singleton Pattern - CRM Application State Manager
public class CRMManager {
    private static CRMManager instance;

    private Map<String, Customer> customers;
    private Map<String, List<Communication>> communications;
    private Map<String, List<Task>> tasks;
    private Map<String, Object> globalSettings;

    private CRMManager() {
        customers = new HashMap<>();
        communications = new HashMap<>();
        tasks = new HashMap<>();
        globalSettings = new HashMap<>();

        // Default settings
        globalSettings.put("notificationsEnabled", true);
        globalSettings.put("reminderTimeInHours", 24);
    }

    public static synchronized CRMManager getInstance() {
        if (instance == null) {
            instance = new CRMManager();
        }
        return instance;
    }

    // Customer Management
    public void addCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
        notifyObservers(EventType.CUSTOMER_ADDED, customer);
    }

    public void updateCustomer(Customer customer) {
        if (customers.containsKey(customer.getId())) {
            customers.put(customer.getId(), customer);
            notifyObservers(EventType.CUSTOMER_UPDATED, customer);
        }
    }

    public void deleteCustomer(String customerId) {
        if (customers.containsKey(customerId)) {
            Customer customer = customers.get(customerId);
            customers.remove(customerId);
            communications.remove(customerId);
            tasks.remove(customerId);
            notifyObservers(EventType.CUSTOMER_DELETED, customer);
        }
    }

    public Customer getCustomer(String id) {
        return customers.get(id);
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers.values());
    }

    // Enhanced Customer Filtering
    public List<Customer> searchCustomers(String searchTerm, String role) {
        if (searchTerm == null) searchTerm = "";
        final String searchTermLower = searchTerm.toLowerCase();
        final boolean filterByRole = role != null && !role.equals("All Roles");

        return customers.values().stream()
                .filter(customer -> {
                    boolean matchesSearch = searchTermLower.isEmpty() ||
                            customer.getName().toLowerCase().contains(searchTermLower) ||
                            customer.getEmail().toLowerCase().contains(searchTermLower) ||
                            customer.getPhone().toLowerCase().contains(searchTermLower) ||
                            customer.getRole().toLowerCase().contains(searchTermLower) ||
                            customer.getNotes().toLowerCase().contains(searchTermLower);

                    boolean matchesRole = !filterByRole || customer.getRole().equals(role);

                    return matchesSearch && matchesRole;
                })
                .collect(Collectors.toList());
    }

    // Advanced filtering API
    public List<Customer> filterCustomers(Predicate<Customer> filter) {
        return customers.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    // Common filter predicates for customer searches
    public Predicate<Customer> nameContains(String text) {
        return customer -> customer.getName().toLowerCase().contains(text.toLowerCase());
    }

    public Predicate<Customer> emailContains(String text) {
        return customer -> customer.getEmail().toLowerCase().contains(text.toLowerCase());
    }

    public Predicate<Customer> phoneContains(String text) {
        return customer -> customer.getPhone().toLowerCase().contains(text.toLowerCase());
    }

    public Predicate<Customer> hasRole(String role) {
        return customer -> customer.getRole().equals(role);
    }

    public Predicate<Customer> hasRecentCommunication(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return customer -> {
            List<Communication> comms = getCustomerCommunications(customer.getId());
            return comms.stream()
                    .anyMatch(comm -> comm.getTimestamp().isAfter(cutoff));
        };
    }

    public Predicate<Customer> hasPendingTasks() {
        return customer -> {
            List<Task> customerTasks = getCustomerTasks(customer.getId());
            return customerTasks.stream()
                    .anyMatch(task -> !task.isCompleted());
        };
    }

    // Communication Management
    public void addCommunication(Communication communication) {
        String customerId = communication.getCustomerId();
        if (!communications.containsKey(customerId)) {
            communications.put(customerId, new ArrayList<>());
        }
        communications.get(customerId).add(communication);
        notifyObservers(EventType.COMMUNICATION_ADDED, communication);
    }

    public void updateCommunication(Communication communication) {
        String customerId = communication.getCustomerId();
        if (communications.containsKey(customerId)) {
            List<Communication> customerComms = communications.get(customerId);
            for (int i = 0; i < customerComms.size(); i++) {
                if (customerComms.get(i).getId().equals(communication.getId())) {
                    customerComms.set(i, communication);
                    notifyObservers(EventType.COMMUNICATION_UPDATED, communication);
                    break;
                }
            }
        }
    }

    public List<Communication> getCustomerCommunications(String customerId) {
        return communications.getOrDefault(customerId, new ArrayList<>());
    }

    // Enhanced communication filtering
    public List<Communication> searchCommunications(String customerId, String type, String tagSearch) {
        List<Communication> results = new ArrayList<>();

        // If customerId is provided, only search that customer's communications
        List<String> customerIds = new ArrayList<>();
        if (customerId != null && !customerId.isEmpty()) {
            customerIds.add(customerId);
        } else {
            customerIds.addAll(communications.keySet());
        }

        for (String id : customerIds) {
            List<Communication> customerComms = communications.getOrDefault(id, new ArrayList<>());

            for (Communication comm : customerComms) {
                boolean matchesType = type == null || type.equals("All Types") || comm.getType().equals(type);
                boolean matchesTag = tagSearch == null || tagSearch.isEmpty() ||
                        comm.getTags().stream()
                                .anyMatch(tag -> tag.toLowerCase().contains(tagSearch.toLowerCase()));

                if (matchesType && matchesTag) {
                    results.add(comm);
                }
            }
        }

        return results;
    }

    // Task Management
    public void addTask(Task task) {
        String customerId = task.getCustomerId();
        if (!tasks.containsKey(customerId)) {
            tasks.put(customerId, new ArrayList<>());
        }
        tasks.get(customerId).add(task);
        notifyObservers(EventType.TASK_ADDED, task);
    }

    public void updateTask(Task task) {
        String customerId = task.getCustomerId();
        if (tasks.containsKey(customerId)) {
            List<Task> customerTasks = tasks.get(customerId);
            for (int i = 0; i < customerTasks.size(); i++) {
                if (customerTasks.get(i).getId().equals(task.getId())) {
                    customerTasks.set(i, task);
                    notifyObservers(EventType.TASK_UPDATED, task);
                    break;
                }
            }
        }
    }

    public List<Task> getCustomerTasks(String customerId) {
        return tasks.getOrDefault(customerId, new ArrayList<>());
    }

    public List<Task> getPendingTasks() {
        List<Task> pendingTasks = new ArrayList<>();
        for (List<Task> customerTasks : tasks.values()) {
            for (Task task : customerTasks) {
                if (!task.isCompleted()) {
                    pendingTasks.add(task);
                }
            }
        }
        return pendingTasks;
    }

    // Enhanced task filtering
    public List<Task> searchTasks(String customerId, boolean showCompleted) {
        List<Task> results = new ArrayList<>();

        // If customerId is provided, only search that customer's tasks
        List<String> customerIds = new ArrayList<>();
        if (customerId != null && !customerId.isEmpty()) {
            customerIds.add(customerId);
        } else {
            customerIds.addAll(tasks.keySet());
        }

        for (String id : customerIds) {
            List<Task> customerTasks = tasks.getOrDefault(id, new ArrayList<>());

            for (Task task : customerTasks) {
                if (showCompleted || !task.isCompleted()) {
                    results.add(task);
                }
            }
        }

        return results;
    }

    // Advanced task filtering
    public List<Task> filterTasks(Predicate<Task> filter) {
        List<Task> allTasks = new ArrayList<>();

        for (List<Task> customerTasks : tasks.values()) {
            allTasks.addAll(customerTasks);
        }

        return allTasks.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    // Settings Management
    public void updateSetting(String key, Object value) {
        globalSettings.put(key, value);
    }

    public Object getSetting(String key) {
        return globalSettings.get(key);
    }

    // Reporting
    public Map<String, Integer> getCommunicationStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalCommunications", 0);

        for (List<Communication> comms : communications.values()) {
            stats.put("totalCommunications", stats.get("totalCommunications") + comms.size());
        }

        return stats;
    }

    public Map<String, Integer> getTaskCompletionStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalTasks", 0);
        stats.put("completedTasks", 0);

        for (List<Task> customerTasks : tasks.values()) {
            stats.put("totalTasks", stats.get("totalTasks") + customerTasks.size());
            for (Task task : customerTasks) {
                if (task.isCompleted()) {
                    stats.put("completedTasks", stats.get("completedTasks") + 1);
                }
            }
        }

        return stats;
    }

    // Observer Pattern Implementation
    public enum EventType {
        CUSTOMER_ADDED,
        CUSTOMER_UPDATED,
        CUSTOMER_DELETED,
        COMMUNICATION_ADDED,
        COMMUNICATION_UPDATED,
        TASK_ADDED,
        TASK_UPDATED
    }

    private Map<EventType, List<Consumer<Object>>> observers = new HashMap<>();

    public void registerObserver(EventType eventType, Consumer<Object> observer) {
        if (!observers.containsKey(eventType)) {
            observers.put(eventType, new ArrayList<>());
        }
        observers.get(eventType).add(observer);
    }

    public void removeObserver(EventType eventType, Consumer<Object> observer) {
        if (observers.containsKey(eventType)) {
            observers.get(eventType).remove(observer);
        }
    }

    private void notifyObservers(EventType eventType, Object data) {
        if (observers.containsKey(eventType)) {
            for (Consumer<Object> observer : observers.get(eventType)) {
                observer.accept(data);
            }
        }
    }
}