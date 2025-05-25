package com.crm;

import com.crm.core.Customer;
import com.crm.core.Communication;
import com.crm.core.Task;
import com.crm.patterns.CRMFactory;
import com.crm.patterns.CRMManager;

import com.crm.ui.JavaFXUI;
import javafx.application.Application;
import java.time.LocalDateTime;

/**
 * Main application class for the Customer Relations Manager (CRM) program.
 * This serves as the entry point for the application.
 */
public class CRMApplication {

    public static void main(String[] args) {
        System.out.println("Starting CRM Application...");

        // Initialize with some sample data if needed
        if (shouldLoadSampleData()) {
            loadSampleData();
        }

        // Launch the JavaFX UI - this will call the start(Stage) method internally
        // Never call start() directly
        Application.launch(JavaFXUI.class, args);
    }

    private static boolean shouldLoadSampleData() {
        // In a real application, this might check command-line args or a config file
        // For simplicity, we'll always return true in this demo
        return true;
    }

    private static void loadSampleData() {
        System.out.println("Loading sample data...");
        CRMManager manager = CRMManager.getInstance();

        // Create sample customers
        Customer customer1 = CRMFactory.createCustomer(
                "John Smith",
                "john.smith@example.com",
                "555-123-4567",
                "Client",
                "Key decision maker for enterprise project. Prefers email communication.");
        manager.addCustomer(customer1);

        Customer customer2 = CRMFactory.createCustomer(
                "Jane Doe",
                "jane.doe@example.com",
                "555-987-6543",
                "Prospect",
                "Met at tech conference. Interested in premium offering. Follow up quarterly.");
        manager.addCustomer(customer2);

        // Create sample communications
        Communication comm1 = CRMFactory.createCommunication(
                customer1.getId(),
                "phone",
                "Discussed new project requirements");
        comm1.addTag("project");
        comm1.addTag("requirements");
        manager.addCommunication(comm1);

        Communication comm2 = CRMFactory.createCommunication(
                customer2.getId(),
                "email",
                "Sent product information brochure");
        comm2.addTag("marketing");
        manager.addCommunication(comm2);

        // Create sample tasks
        Task task1 = CRMFactory.createTask(
                customer1.getId(),
                "Follow up on project proposal",
                LocalDateTime.now().plusDays(3));
        manager.addTask(task1);

        Task task2 = CRMFactory.createTask(
                customer2.getId(),
                "Schedule product demo",
                LocalDateTime.now().plusDays(7));
        manager.addTask(task2);

        System.out.println("Sample data loaded successfully.");
    }
}