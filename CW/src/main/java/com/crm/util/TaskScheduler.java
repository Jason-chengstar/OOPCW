package com.crm.util;

import com.crm.core.Customer;
import com.crm.core.Task;
import com.crm.patterns.CRMManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Task scheduler to handle reminders for pending tasks.
 * This implements a background thread that periodically checks for upcoming tasks
 * and triggers notifications as needed.
 */
public class TaskScheduler {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final CRMManager crmManager;
    private final Timer timer;
    private static final long CHECK_INTERVAL = 60 * 1000; // Check every minute

    // Store tasks that have already been reminded about to avoid duplicate notifications
    private final List<String> notifiedTaskIds = new ArrayList<>();

    // Callback interface for UI notifications
    public interface NotificationHandler {
        void showNotification(Task task, Customer customer);
    }

    private NotificationHandler notificationHandler;

    public TaskScheduler(CRMManager crmManager) {
        this.crmManager = crmManager;
        this.timer = new Timer(true); // Run as daemon thread
    }

    public void setNotificationHandler(NotificationHandler handler) {
        this.notificationHandler = handler;
    }

    /**
     * Start the task scheduler.
     */
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkPendingTasks();
            }
        }, 0, CHECK_INTERVAL);

        System.out.println("Task scheduler started.");
    }

    /**
     * Stop the task scheduler.
     */
    public void stop() {
        timer.cancel();
        System.out.println("Task scheduler stopped.");
    }

    /**
     * Check for pending tasks and send reminders if needed.
     */
    private void checkPendingTasks() {
        if (crmManager.getSetting("notificationsEnabled") == null ||
                !(boolean) crmManager.getSetting("notificationsEnabled")) {
            return; // Notifications are disabled
        }

        LocalDateTime now = LocalDateTime.now();
        List<Task> pendingTasks = crmManager.getPendingTasks();

        for (Task task : pendingTasks) {
            // If task is not completed and it's past the reminder time but before the due date
            // and we haven't already sent a notification for this task
            if (!task.isCompleted() &&
                    task.getReminderTime() != null &&
                    task.getReminderTime().isBefore(now) &&
                    task.getDueDate().isAfter(now) &&
                    !notifiedTaskIds.contains(task.getId())) {

                sendReminder(task);
                notifiedTaskIds.add(task.getId()); // Mark this task as notified
            }

            // If task is overdue and not completed, send another type of reminder
            if (!task.isCompleted() &&
                    task.getDueDate().isBefore(now) &&
                    !notifiedTaskIds.contains("overdue-" + task.getId())) {

                sendOverdueReminder(task);
                notifiedTaskIds.add("overdue-" + task.getId()); // Mark this task as having received an overdue notification
            }
        }
    }

    /**
     * Send a reminder notification for a task.
     * This method handles both console output and UI notifications if available.
     */
    private void sendReminder(Task task) {
        String customerId = task.getCustomerId();
        Customer customer = crmManager.getCustomer(customerId);
        String customerName = customer != null ? customer.getName() : "Unknown";

        // Always log to console
        System.out.println("\n=========== TASK REMINDER ===========");
        System.out.println("Task: " + task.getDescription());
        System.out.println("Customer: " + customerName);
        System.out.println("Due Date: " + task.getDueDate().format(formatter));
        System.out.println("Priority: " + task.getPriority());
        System.out.println("=======================================\n");

        // If UI notification handler is available, use it
        if (notificationHandler != null) {
            Platform.runLater(() -> {
                notificationHandler.showNotification(task, customer);
            });
        }
    }

    /**
     * Send an overdue notification for a task.
     */
    private void sendOverdueReminder(Task task) {
        String customerId = task.getCustomerId();
        Customer customer = crmManager.getCustomer(customerId);
        String customerName = customer != null ? customer.getName() : "Unknown";

        // Always log to console
        System.out.println("\n=========== OVERDUE TASK ALERT ===========");
        System.out.println("OVERDUE Task: " + task.getDescription());
        System.out.println("Customer: " + customerName);
        System.out.println("Due Date: " + task.getDueDate().format(formatter));
        System.out.println("Priority: " + task.getPriority());
        System.out.println("==========================================\n");

        // If UI notification handler is available, use it
        if (notificationHandler != null) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Overdue Task Alert");
                alert.setHeaderText("Task is overdue!");
                alert.setContentText("Task: " + task.getDescription() + "\n" +
                        "Customer: " + customerName + "\n" +
                        "Due Date: " + task.getDueDate().format(formatter) + "\n" +
                        "Priority: " + task.getPriority());
                alert.show();
            });
        }
    }

    /**
     * Clear the notification status for a specific task.
     * Call this when a task is updated to allow new notifications.
     */
    public void clearNotificationStatus(String taskId) {
        notifiedTaskIds.remove(taskId);
        notifiedTaskIds.remove("overdue-" + taskId);
    }
}