package com.crm.ui;

import com.crm.core.Communication;
import com.crm.core.Customer;
import com.crm.core.Task;
import com.crm.patterns.CRMFactory;
import com.crm.patterns.CRMManager;
import com.crm.util.TaskScheduler;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JavaFX-based user interface for the CRM system.
 * Implements the Singleton pattern to ensure only one instance exists.
 */
public class JavaFXUI extends Application {
    private static JavaFXUI instance;
    private CRMManager crmManager;
    private DateTimeFormatter dateFormatter;
    private TaskScheduler taskScheduler;

    // JavaFX components
    private TabPane mainTabPane;
    private TableView<Customer> customerTable;
    private TableView<Task> taskTable;
    private TableView<Communication> communicationTable;
    private Customer selectedCustomer;

    /**
     * Constructor to initialize data structures and set up the date formatter.
     */
    public JavaFXUI() {
        crmManager = CRMManager.getInstance();
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Initialize the task scheduler
        taskScheduler = new TaskScheduler(crmManager);
        taskScheduler.setNotificationHandler(new TaskScheduler.NotificationHandler() {
            @Override
            public void showNotification(Task task, Customer customer) {
                // You can customize notification display here
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Task Reminder");
                alert.setHeaderText("Reminder for task");
                alert.setContentText("Task: " + task.getDescription() + "\n" +
                        "Customer: " + (customer != null ? customer.getName() : "Unknown") + "\n" +
                        "Due Date: " + task.getDueDate().format(dateFormatter));
                alert.show();
            }
        });

        // Start the scheduler
        taskScheduler.start();
    }

    /**
     * Gets the single instance of JavaFXUI (Singleton pattern).
     * Creates the instance if it doesn't already exist.
     *
     * @return The single JavaFXUI instance
     */
    public static JavaFXUI getInstance() {
        if (instance == null) {
            instance = new JavaFXUI();
        }
        return instance;
    }

    /**
     * Initializes and starts the JavaFX UI.
     * Sets up the main window with tabs for different CRM functions.
     *
     * @param primaryStage The primary stage for this JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Customer Relations Manager");

        // Create main layout
        mainTabPane = new TabPane();

        // Create tabs
        Tab customerTab = createCustomerTab();
        Tab communicationTab = createCommunicationTab();
        Tab taskTab = createTaskTab();
        Tab reportingTab = createReportingTab();

        mainTabPane.getTabs().addAll(customerTab, communicationTab, taskTab, reportingTab);

        // Create the main scene
        Scene scene = new Scene(mainTabPane, 900, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Register observers for updates (Observer pattern)
        registerObservers();
    }

    /**
     * Stops the application and performs cleanup.
     */
    @Override
    public void stop() {
        // Stop the task scheduler when the application closes
        if (taskScheduler != null) {
            taskScheduler.stop();
        }

        // Do any other cleanup needed
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Register observers for UI updates when data changes
     */
    private void registerObservers() {
        // When a customer is added, update the customer table
        crmManager.registerObserver(CRMManager.EventType.CUSTOMER_ADDED, data -> {
            updateCustomerTable();
        });

        // When a communication is added, update the communication table
        crmManager.registerObserver(CRMManager.EventType.COMMUNICATION_ADDED, data -> {
            updateCommunicationTable();
        });

        // When a communication is updated, update the communication table
        crmManager.registerObserver(CRMManager.EventType.COMMUNICATION_UPDATED, data -> {
            updateCommunicationTable();
        });

        // When a task is added or updated, update the task table
        crmManager.registerObserver(CRMManager.EventType.TASK_ADDED, data -> {
            updateTaskTable();
        });

        crmManager.registerObserver(CRMManager.EventType.TASK_UPDATED, data -> {
            updateTaskTable();
        });
    }

    /**
     * Creates the Customer Management tab.
     * This tab provides a table view of customers and buttons for adding,
     * viewing, and updating customer information.
     *
     * @return A Tab containing the customer management interface
     */
    private Tab createCustomerTab() {
        Tab tab = new Tab("Customer Management");
        tab.setClosable(false);

        BorderPane borderPane = new BorderPane();

        // Create customer table
        customerTable = new TableView<>();
        customerTable.setEditable(false);

        TableColumn<Customer, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Add notes column
        TableColumn<Customer, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        customerTable.getColumns().addAll(idCol, nameCol, emailCol, phoneCol, roleCol, notesCol);

        // Set column widths
        idCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.1));
        nameCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.15));
        emailCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.2));
        phoneCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.15));
        roleCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.1));
        notesCol.prefWidthProperty().bind(customerTable.widthProperty().multiply(0.3));

        // Add selection listener to customer table
        customerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCustomer = newSelection;
            }
        });

        // Add search/filter functionality
        VBox searchBox = new VBox(10);
        searchBox.setPadding(new Insets(10));

        HBox filterControls = new HBox(10);
        Label searchLabel = new Label("Search:");
        TextField searchField = new TextField();
        searchField.setPromptText("Enter name, email or role");

        // Updated ComboBox with more filter options including direct role filtering
        ComboBox<String> filterOptions = new ComboBox<>();
        filterOptions.getItems().addAll(
                "All Customers",
                "Client",
                "Prospect",
                "Partner",
                "With Communications",
                "No Communications"
        );
        filterOptions.setValue("All Customers");

        filterControls.getChildren().addAll(searchLabel, searchField, new Label("Filter:"), filterOptions);
        searchBox.getChildren().add(filterControls);

        // Add filter event handlers
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filterCustomerTable(newValue, filterOptions.getValue());
        });

        filterOptions.valueProperty().addListener((obs, oldValue, newValue) -> {
            filterCustomerTable(searchField.getText(), newValue);
        });

        borderPane.setTop(searchBox);
        borderPane.setCenter(customerTable);

        // Create button panel
        HBox buttonPanel = new HBox(10);
        buttonPanel.setPadding(new Insets(10));

        Button addButton = new Button("Add Customer");
        Button updateButton = new Button("Update Customer");
        Button deleteButton = new Button("Delete Customer");

        buttonPanel.getChildren().addAll(addButton, updateButton, deleteButton);

        // Set button actions
        addButton.setOnAction(e -> showAddCustomerDialog());
        updateButton.setOnAction(e -> {
            if (selectedCustomer != null) {
                showUpdateCustomerDialog(selectedCustomer);
            } else {
                showAlert("Please select a customer first.");
            }
        });
        deleteButton.setOnAction(e -> {
            if (selectedCustomer != null) {
                // In a real application, you would add confirmation
                // We would also need to add a delete method to CRMManager
                showAlert("Delete functionality would remove customer: " + selectedCustomer.getName());
            } else {
                showAlert("Please select a customer first.");
            }
        });

        borderPane.setBottom(buttonPanel);

        tab.setContent(borderPane);

        // Initialize the table
        updateCustomerTable();

        return tab;
    }

    /**
     * Creates the Communication Tracking tab.
     * This tab displays a table of communications and provides functionality
     * for logging new communications, adding tags, and filtering by customer.
     *
     * @return A Tab containing the communication tracking interface
     */
    private Tab createCommunicationTab() {
        Tab tab = new Tab("Communication Tracking");
        tab.setClosable(false);

        BorderPane borderPane = new BorderPane();

        // Create communication table
        communicationTable = new TableView<>();
        communicationTable.setEditable(false);

        TableColumn<Communication, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Communication, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            String customerId = cellData.getValue().getCustomerId();
            Customer customer = crmManager.getCustomer(customerId);
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> customer != null ? customer.getName() : "Unknown"
            );
        });

        TableColumn<Communication, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Communication, LocalDateTime> dateCol = new TableColumn<>("Date/Time");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        dateCol.setCellFactory(column -> {
            return new TableCell<Communication, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(dateFormatter.format(item));
                    }
                }
            };
        });

        TableColumn<Communication, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        TableColumn<Communication, List<String>> tagsCol = new TableColumn<>("Tags");
        tagsCol.setCellValueFactory(new PropertyValueFactory<>("tags"));
        tagsCol.setCellFactory(column -> {
            return new TableCell<Communication, List<String>>() {
                @Override
                protected void updateItem(List<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(String.join(", ", item));
                    }
                }
            };
        });

        communicationTable.getColumns().addAll(idCol, customerCol, typeCol, dateCol, notesCol, tagsCol);

        // Set column widths
        idCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.15));
        customerCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.15));
        typeCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.1));
        dateCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.15));
        notesCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.3));
        tagsCol.prefWidthProperty().bind(communicationTable.widthProperty().multiply(0.15));

        // Add filter controls
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(10));

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "phone", "email", "meeting");
        typeFilter.setValue("All Types");

        TextField tagSearchField = new TextField();
        tagSearchField.setPromptText("Search by tag");

        filterBox.getChildren().addAll(
                new Label("Type:"), typeFilter,
                new Label("Tag:"), tagSearchField
        );

        // Add filter event handlers
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            filterCommunicationTable(newValue, tagSearchField.getText());
        });

        tagSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filterCommunicationTable(typeFilter.getValue(), newValue);
        });

        borderPane.setTop(filterBox);
        borderPane.setCenter(communicationTable);

        // Create button panel
        HBox buttonPanel = new HBox(10);
        buttonPanel.setPadding(new Insets(10));

        Button addButton = new Button("Log Communication");
        Button addTagsButton = new Button("Add Tags");

        buttonPanel.getChildren().addAll(addButton, addTagsButton);

        // Set button actions
        addButton.setOnAction(e -> showLogCommunicationDialog());

        addTagsButton.setOnAction(e -> {
            Communication selectedComm = communicationTable.getSelectionModel().getSelectedItem();
            if (selectedComm != null) {
                showAddTagsDialog(selectedComm);
            } else {
                showAlert("Please select a communication first.");
            }
        });

        borderPane.setBottom(buttonPanel);

        tab.setContent(borderPane);

        // Initialize the table
        updateCommunicationTable();

        return tab;
    }

    /**
     * Creates the Task Management tab.
     * Displays a table of tasks with options to add, update, and mark tasks as completed.
     *
     * @return A Tab containing the task management interface
     */
    private Tab createTaskTab() {
        Tab tab = new Tab("Task Management");
        tab.setClosable(false);

        BorderPane borderPane = new BorderPane();

        // Create task table
        taskTable = new TableView<>();
        taskTable.setEditable(false);

        TableColumn<Task, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Task, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            String customerId = cellData.getValue().getCustomerId();
            Customer customer = crmManager.getCustomer(customerId);
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> customer != null ? customer.getName() : "Unknown"
            );
        });

        TableColumn<Task, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Task, LocalDateTime> dueDateCol = new TableColumn<>("Due Date");
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dueDateCol.setCellFactory(column -> {
            return new TableCell<Task, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(dateFormatter.format(item));
                    }
                }
            };
        });

        TableColumn<Task, Boolean> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(new PropertyValueFactory<>("completed"));
        completedCol.setCellFactory(column -> {
            return new TableCell<Task, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item ? "Yes" : "No");
                    }
                }
            };
        });

        taskTable.getColumns().addAll(idCol, customerCol, descriptionCol, dueDateCol, completedCol);

        // Set column widths
        idCol.prefWidthProperty().bind(taskTable.widthProperty().multiply(0.15));
        customerCol.prefWidthProperty().bind(taskTable.widthProperty().multiply(0.2));
        descriptionCol.prefWidthProperty().bind(taskTable.widthProperty().multiply(0.35));
        dueDateCol.prefWidthProperty().bind(taskTable.widthProperty().multiply(0.15));
        completedCol.prefWidthProperty().bind(taskTable.widthProperty().multiply(0.15));

        // Add filter controls
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(10));

        CheckBox showCompletedBox = new CheckBox("Show Completed Tasks");
        showCompletedBox.setSelected(true);

        filterBox.getChildren().addAll(showCompletedBox);

        // Add filter event handler
        showCompletedBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            filterTaskTable(newValue);
        });

        borderPane.setTop(filterBox);
        borderPane.setCenter(taskTable);

        // Create button panel
        HBox buttonPanel = new HBox(10);
        buttonPanel.setPadding(new Insets(10));

        Button addButton = new Button("Add Task");
        Button markCompletedButton = new Button("Mark as Completed");
        Button reminderButton = new Button("Reminder");

        buttonPanel.getChildren().addAll(addButton, markCompletedButton, reminderButton);

        // Set button actions
        addButton.setOnAction(e -> showAddTaskDialog());
        markCompletedButton.setOnAction(e -> {
            Task selectedTask = taskTable.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                selectedTask.setCompleted(true);
                crmManager.updateTask(selectedTask);
                updateTaskTable();
            } else {
                showAlert("Please select a task first.");
            }
        });

        // Set action for the Reminder button
        reminderButton.setOnAction(e -> showReminderDialog());

        borderPane.setBottom(buttonPanel);

        tab.setContent(borderPane);

        // Initialize the table
        updateTaskTable();

        return tab;
    }

    /**
     * Creates the Reporting tab.
     * Provides statistical reports and visualizations of CRM data.
     *
     * @return A Tab containing the reporting interface
     */
    private Tab createReportingTab() {
        Tab tab = new Tab("Reporting");
        tab.setClosable(false);

        TabPane reportTabs = new TabPane();

        // Communication Frequency Report Tab
        Tab commFrequencyTab = new Tab("Communication Frequency");
        commFrequencyTab.setClosable(false);

        VBox commFrequencyBox = new VBox(20);
        commFrequencyBox.setPadding(new Insets(20));

        // Communication frequency chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> communicationFrequencyChart = new BarChart<>(xAxis, yAxis);
        communicationFrequencyChart.setTitle("Communication Frequency");
        xAxis.setLabel("Time Period");
        yAxis.setLabel("Number of Communications");

        // Add time period selector
        HBox periodSelector = new HBox(10);
        Label periodLabel = new Label("Time Period:");
        ComboBox<String> periodComboBox = new ComboBox<>();
        periodComboBox.getItems().addAll("Daily", "Weekly", "Monthly");
        periodComboBox.setValue("Weekly");

        // Add period selector change listener
        periodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateCommunicationFrequencyChart(communicationFrequencyChart, newVal);
        });

        periodSelector.getChildren().addAll(periodLabel, periodComboBox);

        Button refreshCommFreqButton = new Button("Refresh Report");
        refreshCommFreqButton.setOnAction(e -> updateCommunicationFrequencyChart(
                communicationFrequencyChart, periodComboBox.getValue()));

        commFrequencyBox.getChildren().addAll(
                new Label("Communication Frequency Report"),
                periodSelector,
                communicationFrequencyChart,
                refreshCommFreqButton
        );

        commFrequencyTab.setContent(commFrequencyBox);

        // Add tabs to the reports tab pane
        reportTabs.getTabs().addAll(commFrequencyTab);

        tab.setContent(reportTabs);

        // Initialize the charts
        updateCommunicationFrequencyChart(communicationFrequencyChart, "Weekly");

        return tab;
    }

    /**
     * Updates the communication frequency chart based on selected time period
     */
    private void updateCommunicationFrequencyChart(BarChart<String, Number> chart, String timePeriod) {
        // Clear existing data
        chart.getData().clear();

        // Get current date for reference
        LocalDateTime now = LocalDateTime.now();

        // Create series for each communication type
        XYChart.Series<String, Number> phoneSeries = new XYChart.Series<>();
        phoneSeries.setName("Phone");

        XYChart.Series<String, Number> emailSeries = new XYChart.Series<>();
        emailSeries.setName("Email");

        XYChart.Series<String, Number> meetingSeries = new XYChart.Series<>();
        meetingSeries.setName("Meeting");

        // Map to hold the communication counts
        Map<String, Map<String, Integer>> timePeriodCounts = new HashMap<>();

        // Get all communications
        List<Communication> allCommunications = new ArrayList<>();
        for (Customer customer : crmManager.getAllCustomers()) {
            allCommunications.addAll(crmManager.getCustomerCommunications(customer.getId()));
        }

        // Process according to selected time period
        switch (timePeriod) {
            case "Daily":
                // For last 7 days
                for (int i = 6; i >= 0; i--) {
                    LocalDateTime date = now.minusDays(i);
                    String dateLabel = date.format(DateTimeFormatter.ofPattern("MM/dd"));

                    Map<String, Integer> typeCounts = new HashMap<>();
                    typeCounts.put("phone", 0);
                    typeCounts.put("email", 0);
                    typeCounts.put("meeting", 0);

                    timePeriodCounts.put(dateLabel, typeCounts);
                }

                // Count communications by day
                for (Communication comm : allCommunications) {
                    LocalDateTime commDate = comm.getTimestamp();
                    if (commDate.isAfter(now.minusDays(7))) {
                        String dateLabel = commDate.format(DateTimeFormatter.ofPattern("MM/dd"));
                        if (timePeriodCounts.containsKey(dateLabel)) {
                            Map<String, Integer> typeCounts = timePeriodCounts.get(dateLabel);
                            typeCounts.put(comm.getType(), typeCounts.get(comm.getType()) + 1);
                        }
                    }
                }
                break;

            case "Weekly":
                // For last 4 weeks
                for (int i = 3; i >= 0; i--) {
                    LocalDateTime weekStart = now.minusWeeks(i);
                    String weekLabel = "Week " + (4-i) + " (" +
                            weekStart.format(DateTimeFormatter.ofPattern("MM/dd")) + ")";

                    Map<String, Integer> typeCounts = new HashMap<>();
                    typeCounts.put("phone", 0);
                    typeCounts.put("email", 0);
                    typeCounts.put("meeting", 0);

                    timePeriodCounts.put(weekLabel, typeCounts);
                }

                // Count communications by week
                for (Communication comm : allCommunications) {
                    LocalDateTime commDate = comm.getTimestamp();
                    if (commDate.isAfter(now.minusWeeks(4))) {
                        int weekIndex = 0;
                        if (commDate.isAfter(now.minusWeeks(1))) {
                            weekIndex = 3;
                        } else if (commDate.isAfter(now.minusWeeks(2))) {
                            weekIndex = 2;
                        } else if (commDate.isAfter(now.minusWeeks(3))) {
                            weekIndex = 1;
                        }

                        String weekLabel = "Week " + (4-weekIndex) + " (" +
                                now.minusWeeks(weekIndex).format(DateTimeFormatter.ofPattern("MM/dd")) + ")";

                        if (timePeriodCounts.containsKey(weekLabel)) {
                            Map<String, Integer> typeCounts = timePeriodCounts.get(weekLabel);
                            typeCounts.put(comm.getType(), typeCounts.get(comm.getType()) + 1);
                        }
                    }
                }
                break;

            case "Monthly":
                // For last 6 months
                for (int i = 5; i >= 0; i--) {
                    LocalDateTime monthStart = now.minusMonths(i);
                    String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy"));

                    Map<String, Integer> typeCounts = new HashMap<>();
                    typeCounts.put("phone", 0);
                    typeCounts.put("email", 0);
                    typeCounts.put("meeting", 0);

                    timePeriodCounts.put(monthLabel, typeCounts);
                }

                // Count communications by month
                for (Communication comm : allCommunications) {
                    LocalDateTime commDate = comm.getTimestamp();
                    if (commDate.isAfter(now.minusMonths(6))) {
                        String monthLabel = commDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));

                        if (timePeriodCounts.containsKey(monthLabel)) {
                            Map<String, Integer> typeCounts = timePeriodCounts.get(monthLabel);
                            typeCounts.put(comm.getType(), typeCounts.get(comm.getType()) + 1);
                        }
                    }
                }
                break;
        }

        // Add data to series
        for (Map.Entry<String, Map<String, Integer>> entry : timePeriodCounts.entrySet()) {
            String label = entry.getKey();
            Map<String, Integer> typeCounts = entry.getValue();

            phoneSeries.getData().add(new XYChart.Data<>(label, typeCounts.get("phone")));
            emailSeries.getData().add(new XYChart.Data<>(label, typeCounts.get("email")));
            meetingSeries.getData().add(new XYChart.Data<>(label, typeCounts.get("meeting")));
        }

        // Add series to chart
        chart.getData().addAll(phoneSeries, emailSeries, meetingSeries);
    }

    /**
     * Shows a dialog to set reminders for tasks associated with a customer.
     * Allows the user to enter a customer name and set reminders.
     */
    private void showReminderDialog() {
        // Create a dialog for the customer selection
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Set Task Reminder");
        dialog.setHeaderText("Select a customer to set task reminders");

        // Set the button types
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        // Create a layout for the dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label customerLabel = new Label("Enter Customer Name:");
        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Type customer name");

        // Create a filtered list of customers
        List<Customer> allCustomers = crmManager.getAllCustomers();
        ListView<Customer> customerListView = new ListView<>();
        FilteredList<Customer> filteredCustomers = new FilteredList<>(
                FXCollections.observableArrayList(allCustomers),
                p -> true
        );

        customerListView.setItems(filteredCustomers);
        customerListView.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getName());
                }
            }
        });

        // Filter the list as the user types
        customerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredCustomers.setPredicate(customer -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                return customer.getName().toLowerCase().contains(newValue.toLowerCase());
            });
        });

        content.getChildren().addAll(customerLabel, customerNameField, customerListView);
        dialog.getDialogPane().setContent(content);

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return customerListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // Show the dialog and handle the result
        dialog.showAndWait().ifPresent(customer -> {
            if (customer != null) {
                showTaskRemindersForCustomer(customer);
            }
        });
    }

    /**
     * Shows tasks for the selected customer and allows setting reminders.
     * @param customer The customer whose tasks to show
     */
    private void showTaskRemindersForCustomer(Customer customer) {
        // Get tasks for the selected customer
        List<Task> customerTasks = crmManager.getCustomerTasks(customer.getId());

        // Filter out completed tasks
        List<Task> pendingTasks = customerTasks.stream()
                .filter(task -> !task.isCompleted())
                .collect(Collectors.toList());

        if (pendingTasks.isEmpty()) {
            showAlert("No pending tasks found for " + customer.getName());
            return;
        }

        // Create a dialog to display and set reminders for tasks
        Dialog<List<Task>> reminderDialog = new Dialog<>();
        reminderDialog.setTitle("Set Task Reminders");
        reminderDialog.setHeaderText("Set reminders for " + customer.getName() + "'s tasks");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save Reminders", ButtonBar.ButtonData.OK_DONE);
        reminderDialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label infoLabel = new Label("Set reminder times for the following tasks:");
        content.getChildren().add(infoLabel);

        // Create a map to store the reminder date/time fields for each task
        Map<String, DatePicker> datePickerMap = new HashMap<>();
        Map<String, ComboBox<String>> timePickerMap = new HashMap<>();

        // For each task, create a row with the task description and reminder setting controls
        for (Task task : pendingTasks) {
            HBox taskRow = new HBox(10);
            taskRow.setAlignment(Pos.CENTER_LEFT);

            Label taskLabel = new Label(task.getDescription());
            taskLabel.setPrefWidth(200);

            Label dueLabel = new Label("Due: " + dateFormatter.format(task.getDueDate()));
            dueLabel.setPrefWidth(150);

            // Create date picker for reminder date
            DatePicker datePicker = new DatePicker();
            // Set default to current reminder time if it exists
            datePicker.setValue(task.getReminderTime().toLocalDate());
            datePickerMap.put(task.getId(), datePicker);

            // Create time picker (simplified as a ComboBox with hour selections)
            ComboBox<String> timePicker = new ComboBox<>();
            for (int i = 0; i < 24; i++) {
                timePicker.getItems().add(String.format("%02d:00", i));
                timePicker.getItems().add(String.format("%02d:30", i));
            }
            // Set default to current reminder hour/minute
            timePicker.setValue(String.format("%02d:%02d",
                    task.getReminderTime().getHour(),
                    task.getReminderTime().getMinute()));
            timePickerMap.put(task.getId(), timePicker);

            taskRow.getChildren().addAll(taskLabel, dueLabel, new Label("Remind on:"), datePicker, timePicker);
            content.getChildren().add(taskRow);
        }

        reminderDialog.getDialogPane().setContent(new ScrollPane(content));

        // Convert the result when the save button is clicked
        reminderDialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                List<Task> updatedTasks = new ArrayList<>();

                for (Task task : pendingTasks) {
                    DatePicker datePicker = datePickerMap.get(task.getId());
                    ComboBox<String> timePicker = timePickerMap.get(task.getId());

                    if (datePicker.getValue() != null && timePicker.getValue() != null) {
                        // Parse time from the ComboBox
                        String[] timeParts = timePicker.getValue().split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);

                        // Create LocalDateTime from date and time components
                        LocalDateTime reminderDateTime = LocalDateTime.of(
                                datePicker.getValue(),
                                LocalTime.of(hour, minute)
                        );

                        // Update the task's reminder time
                        task.setReminderTime(reminderDateTime);
                        updatedTasks.add(task);
                    }
                }

                return updatedTasks;
            }
            return null;
        });

        // Show the dialog and process the result
        reminderDialog.showAndWait().ifPresent(updatedTasks -> {
            if (!updatedTasks.isEmpty()) {
                // Update each task in the CRM manager
                for (Task task : updatedTasks) {
                    crmManager.updateTask(task);

                    // Reset notification status to allow new notifications
                    if (taskScheduler != null) {
                        taskScheduler.clearNotificationStatus(task.getId());
                    }
                }

                showAlert("Reminder times updated for " + updatedTasks.size() + " tasks");
                updateTaskTable();
            }
        });
    }

    /**
     * Get access to the task scheduler.
     *
     * @return The task scheduler instance
     */
    private TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    /**
     * Updates the customer table with current data
     */
    private void updateCustomerTable() {
        customerTable.setItems(FXCollections.observableArrayList(crmManager.getAllCustomers()));
    }

    /**
     * Filters the customer table based on search text and filter option
     * Enhanced to include direct role-based filtering
     */
    private void filterCustomerTable(String searchText, String filterOption) {
        List<Customer> allCustomers = crmManager.getAllCustomers();
        List<Customer> filteredList;

        // First apply the dropdown filter
        switch(filterOption) {
            case "All Customers":
                filteredList = new ArrayList<>(allCustomers);
                break;
            case "Client":
                filteredList = allCustomers.stream()
                        .filter(c -> "Client".equals(c.getRole()))
                        .collect(Collectors.toList());
                break;
            case "Prospect":
                filteredList = allCustomers.stream()
                        .filter(c -> "Prospect".equals(c.getRole()))
                        .collect(Collectors.toList());
                break;
            case "Partner":
                filteredList = allCustomers.stream()
                        .filter(c -> "Partner".equals(c.getRole()))
                        .collect(Collectors.toList());
                break;

            case "With Communications":
                filteredList = allCustomers.stream()
                        .filter(c -> !crmManager.getCustomerCommunications(c.getId()).isEmpty())
                        .collect(Collectors.toList());
                break;
            case "No Communications":
                filteredList = allCustomers.stream()
                        .filter(c -> crmManager.getCustomerCommunications(c.getId()).isEmpty())
                        .collect(Collectors.toList());
                break;
            default:
                filteredList = new ArrayList<>(allCustomers);
        }

        // Then apply the search text filter if provided
        if (searchText != null && !searchText.isEmpty()) {
            String searchLower = searchText.toLowerCase();
            filteredList = filteredList.stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchLower) ||
                            c.getEmail().toLowerCase().contains(searchLower) ||
                            c.getRole().toLowerCase().contains(searchLower) ||
                            c.getNotes().toLowerCase().contains(searchLower)) // Added notes to search
                    .collect(Collectors.toList());
        }

        customerTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    /**
     * Helper method to determine if a customer is active
     */
    private boolean isActive(Customer customer) {
        // Example logic: customer with communication in last 90 days is considered active
        List<Communication> comms = crmManager.getCustomerCommunications(customer.getId());
        if (comms.isEmpty()) return false;

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        return comms.stream()
                .anyMatch(comm -> comm.getTimestamp().isAfter(ninetyDaysAgo));
    }

    /**
     * Helper method to get customers created within specified days
     */
    private List<Customer> getRecentCustomers(int days) {
        // Since Customer class doesn't have a creation date field,
        // We'll use a workaround based on communication timestamps
        List<Customer> allCustomers = crmManager.getAllCustomers();
        List<Customer> recentCustomers = new ArrayList<>();

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        for (Customer customer : allCustomers) {
            List<Communication> comms = crmManager.getCustomerCommunications(customer.getId());
            if (!comms.isEmpty()) {
                // Find earliest communication
                LocalDateTime earliestComm = comms.stream()
                        .map(Communication::getTimestamp)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                if (earliestComm != null && earliestComm.isAfter(cutoffDate)) {
                    recentCustomers.add(customer);
                }
            }
        }

        return recentCustomers;
    }

    /**
     * Updates the communication table with all communications
     */
    private void updateCommunicationTable() {
        List<Communication> allCommunications = new ArrayList<>();

        // Collect all communications from all customers
        for (Customer customer : crmManager.getAllCustomers()) {
            allCommunications.addAll(crmManager.getCustomerCommunications(customer.getId()));
        }

        communicationTable.setItems(FXCollections.observableArrayList(allCommunications));
    }

    /**
     * Filters the communication table based on type and tag
     */
    private void filterCommunicationTable(String typeFilter, String tagFilter) {
        List<Communication> allCommunications = new ArrayList<>();

        // Collect all communications
        for (Customer customer : crmManager.getAllCustomers()) {
            allCommunications.addAll(crmManager.getCustomerCommunications(customer.getId()));
        }

        // Apply filters
        List<Communication> filteredList = allCommunications.stream()
                .filter(comm -> {
                    // Apply type filter if not "All Types"
                    if (!"All Types".equals(typeFilter) && !comm.getType().equals(typeFilter)) {
                        return false;
                    }

                    // Apply tag filter if provided
                    if (tagFilter != null && !tagFilter.isEmpty()) {
                        String tagLower = tagFilter.toLowerCase();
                        return comm.getTags().stream()
                                .anyMatch(tag -> tag.toLowerCase().contains(tagLower));
                    }

                    return true;
                })
                .collect(Collectors.toList());

        communicationTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    /**
     * Updates the task table with all tasks
     */
    private void updateTaskTable() {
        List<Task> allTasks = new ArrayList<>();

        // Collect all tasks from all customers
        for (Customer customer : crmManager.getAllCustomers()) {
            allTasks.addAll(crmManager.getCustomerTasks(customer.getId()));
        }

        taskTable.setItems(FXCollections.observableArrayList(allTasks));
    }

    /**
     * Filters the task table based on completion status
     */
    private void filterTaskTable(boolean showCompleted) {
        List<Task> allTasks = new ArrayList<>();

        // Collect all tasks
        for (Customer customer : crmManager.getAllCustomers()) {
            allTasks.addAll(crmManager.getCustomerTasks(customer.getId()));
        }

        // Filter tasks based on completion status
        List<Task> filteredList = allTasks;
        if (!showCompleted) {
            filteredList = allTasks.stream()
                    .filter(task -> !task.isCompleted())
                    .collect(Collectors.toList());
        }

        taskTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    /**
     * Displays a dialog for adding a new customer.
     * Creates form fields for entering customer details.
     */
    private void showAddCustomerDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText("Enter customer details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Name");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Client", "Prospect", "Partner");
        roleComboBox.setValue("Client");

        // Add Notes TextArea
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter notes about this customer");
        notesArea.setPrefRowCount(5);
        notesArea.setPrefWidth(300);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleComboBox, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the name field by default
        nameField.requestFocus();

        // Convert the result to a customer when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || emailField.getText().isEmpty()) {
                    showAlert("Name and email are required.");
                    return null;
                }

                // Use Factory pattern to create customer with notes
                return CRMFactory.createCustomer(
                        nameField.getText(),
                        emailField.getText(),
                        phoneField.getText(),
                        roleComboBox.getValue(),
                        notesArea.getText()
                );
            }
            return null;
        });

        // Show the dialog and process the result
        dialog.showAndWait().ifPresent(customer -> {
            if (customer != null) {
                crmManager.addCustomer(customer);
                updateCustomerTable();
            }
        });
    }

    /**
     * Displays a dialog for updating customer information.
     *
     * @param customer The customer to update
     */
    private void showUpdateCustomerDialog(Customer customer) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Update Customer");
        dialog.setHeaderText("Update information for " + customer.getName());

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(customer.getName());
        TextField emailField = new TextField(customer.getEmail());
        TextField phoneField = new TextField(customer.getPhone());

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Client", "Prospect", "Partner");
        roleComboBox.setValue(customer.getRole());

        // Add Notes TextArea with existing notes
        TextArea notesArea = new TextArea(customer.getNotes());
        notesArea.setPromptText("Enter notes about this customer");
        notesArea.setPrefRowCount(5);
        notesArea.setPrefWidth(300);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Phone:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleComboBox, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert the result when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || emailField.getText().isEmpty()) {
                    showAlert("Name and email are required.");
                    return false;
                }

                customer.setName(nameField.getText());
                customer.setEmail(emailField.getText());
                customer.setPhone(phoneField.getText());
                customer.setRole(roleComboBox.getValue());
                customer.setNotes(notesArea.getText());

                return true;
            }
            return false;
        });

        // Show the dialog and process the result
        dialog.showAndWait().ifPresent(updated -> {
            if (updated) {
                crmManager.updateCustomer(customer);
                updateCustomerTable();
            }
        });
    }

    /**
     * Displays a dialog for logging a new communication.
     * Allows selection of customer, communication type, and notes.
     */
    private void showLogCommunicationDialog() {
        Dialog<Communication> dialog = new Dialog<>();
        dialog.setTitle("Log Communication");
        dialog.setHeaderText("Record a new customer communication");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setItems(FXCollections.observableArrayList(crmManager.getAllCustomers()));
        customerComboBox.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getName());
                }
            }
        });
        customerComboBox.setButtonCell(new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getName());
                }
            }
        });

        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("phone", "email", "meeting");
        typeComboBox.setValue("phone");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter notes about the communication");
        notesArea.setPrefRowCount(5);

        TextField tagsField = new TextField();
        tagsField.setPromptText("Enter tags separated by commas");

        grid.add(new Label("Customer:"), 0, 0);
        grid.add(customerComboBox, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeComboBox, 1, 1);
        grid.add(new Label("Notes:"), 0, 2);
        grid.add(notesArea, 1, 2);
        grid.add(new Label("Tags:"), 0, 3);
        grid.add(tagsField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a communication when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Customer selectedCustomer = customerComboBox.getValue();
                if (selectedCustomer == null || notesArea.getText().isEmpty()) {
                    showAlert("Customer and notes are required.");
                    return null;
                }

                // Use Factory pattern to create communication
                Communication communication = CRMFactory.createCommunication(
                        selectedCustomer.getId(),
                        typeComboBox.getValue(),
                        notesArea.getText()
                );

                // Add tags if provided
                String tagsText = tagsField.getText().trim();
                if (!tagsText.isEmpty()) {
                    String[] tags = tagsText.split(",");
                    for (String tag : tags) {
                        communication.addTag(tag.trim());
                    }
                }

                return communication;
            }
            return null;
        });

        // Show the dialog and process the result
        dialog.showAndWait().ifPresent(communication -> {
            if (communication != null) {
                crmManager.addCommunication(communication);
                updateCommunicationTable();
            }
        });
    }

    /**
     * Displays a dialog for adding tags to a communication.
     *
     * @param communication The communication to add tags to
     */
    private void showAddTagsDialog(Communication communication) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Tags");
        dialog.setHeaderText("Add tags to the selected communication");

        // Set the button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label currentTagsLabel = new Label("Current tags: " +
                (communication.getTags().isEmpty() ? "None" : String.join(", ", communication.getTags())));

        TextField newTagField = new TextField();
        newTagField.setPromptText("Enter new tags separated by commas");

        grid.add(currentTagsLabel, 0, 0, 2, 1);
        grid.add(new Label("New Tags:"), 0, 1);
        grid.add(newTagField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the new tag field
        newTagField.requestFocus();

        // Convert the result when the add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return newTagField.getText().trim();
            }
            return null;
        });

        // Show the dialog and process the result
        dialog.showAndWait().ifPresent(tagsText -> {
            if (!tagsText.isEmpty()) {
                String[] tags = tagsText.split(",");
                for (String tag : tags) {
                    communication.addTag(tag.trim());
                }
                updateCommunicationTable();
            }
        });
    }

    /**
     * Displays a dialog for adding a new task.
     */
    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add Task");
        dialog.setHeaderText("Create a new task");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setItems(FXCollections.observableArrayList(crmManager.getAllCustomers()));
        customerComboBox.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getName());
                }
            }
        });
        customerComboBox.setButtonCell(new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getName());
                }
            }
        });

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Enter task description");

        TextField dueDateField = new TextField();
        dueDateField.setPromptText("YYYY-MM-DD HH:MM");

        grid.add(new Label("Customer:"), 0, 0);
        grid.add(customerComboBox, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDateField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the description field
        descriptionField.requestFocus();

        // Convert the result to a task when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Customer selectedCustomer = customerComboBox.getValue();
                if (selectedCustomer == null || descriptionField.getText().isEmpty() || dueDateField.getText().isEmpty()) {
                    showAlert("All fields are required.");
                    return null;
                }

                try {
                    LocalDateTime dueDate = LocalDateTime.parse(dueDateField.getText(), dateFormatter);

                    // Use Factory pattern to create task
                    return CRMFactory.createTask(
                            selectedCustomer.getId(),
                            descriptionField.getText(),
                            dueDate
                    );
                } catch (DateTimeParseException e) {
                    showAlert("Invalid date format. Please use YYYY-MM-DD HH:MM format.");
                    return null;
                }
            }
            return null;
        });

        // Show the dialog and process the result
        dialog.showAndWait().ifPresent(task -> {
            if (task != null) {
                crmManager.addTask(task);
                updateTaskTable();
            }
        });
    }

    /**
     * Displays an alert dialog with the given message.
     *
     * @param message The message to display
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for customer activity summary in the reporting tab
     */
    public static class CustomerActivitySummary {
        private final String customerName;
        private final int communicationCount;
        private final int taskCount;
        private final int completedTaskCount;
        private final double completionRate;

        public CustomerActivitySummary(String customerName, int communicationCount, int taskCount, int completedTaskCount) {
            this.customerName = customerName;
            this.communicationCount = communicationCount;
            this.taskCount = taskCount;
            this.completedTaskCount = completedTaskCount;
            this.completionRate = taskCount > 0 ? (double) completedTaskCount / taskCount * 100 : 0;
        }

        public String getCustomerName() { return customerName; }
        public int getCommunicationCount() { return communicationCount; }
        public int getTaskCount() { return taskCount; }
        public int getCompletedTaskCount() { return completedTaskCount; }
        public double getCompletionRate() { return completionRate; }
    }
}