import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class ExpenseTrackerMultiCurrency extends Application {
    private final ObservableList<Expense> expenseData = FXCollections.observableArrayList();
    private final ObservableList<String> categoryList = FXCollections.observableArrayList("Food", "Travel", "Bills", "Shopping", "Other");
    private final ObservableList<String> currencyList = FXCollections.observableArrayList("USD", "EUR", "GBP", "INR", "JPY", "AUD");

    private String baseCurrency = "USD";

    private TableView<Expense> expenseTable;
    private Label totalLabel;
    private PieChart categoryChart;
    private ComboBox<String> baseCurrencyCombo;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        HBox form = new HBox(10);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> categoryBox = new ComboBox<>(categoryList);
        categoryBox.setPromptText("Category");
        ComboBox<String> currencyBox = new ComboBox<>(currencyList);
        currencyBox.setPromptText("Currency");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        TextField descField = new TextField();
        descField.setPromptText("Description");
        Button addBtn = new Button("Add");

        form.getChildren().addAll(datePicker, categoryBox, currencyBox, amountField, descField, addBtn);

        expenseTable = new TableView<>(expenseData);
        TableColumn<Expense, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Expense, String> curCol = new TableColumn<>("Currency");
        curCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        expenseTable.getColumns().addAll(dateCol, catCol, curCol, amountCol, descCol);
        expenseTable.setPrefHeight(200);

        totalLabel = new Label("Total: $0.00");

        categoryChart = new PieChart();
        categoryChart.setTitle("Spending by Category");

        HBox settings = new HBox(10);
        baseCurrencyCombo = new ComboBox<>(currencyList);
        baseCurrencyCombo.setValue("USD");
        baseCurrencyCombo.setOnAction(e -> {
            baseCurrency = baseCurrencyCombo.getValue();
            updateTotal();
            updatePieChart();
        });
        settings.getChildren().addAll(new Label("Base Currency:"), baseCurrencyCombo);

        addBtn.setOnAction(e -> {
            try {
                LocalDate date = datePicker.getValue();
                String cat = categoryBox.getValue();
                String cur = currencyBox.getValue();
                double amt = Double.parseDouble(amountField.getText());
                String desc = descField.getText();

                if (date == null || cat == null || cur == null || desc.isEmpty()) {
                    showAlert("Please fill all fields.");
                    return;
                }

                double baseAmt = amt * getExchangeRate(cur, baseCurrency);
                Expense exp = new Expense(date, cat, cur, amt, baseAmt, desc);
                expenseData.add(exp);

                updateTotal();
                updatePieChart();

                datePicker.setValue(LocalDate.now());
                categoryBox.setValue(null);
                currencyBox.setValue(null);
                amountField.clear();
                descField.clear();

            } catch (NumberFormatException ex) {
                showAlert("Amount must be a valid number.");
            }
        });

        root.getChildren().addAll(form, settings, expenseTable, totalLabel, categoryChart);

        Scene scene = new Scene(root, 850, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Multi-Currency Expense Tracker");
        primaryStage.show();
    }

    private void updateTotal() {
        double total = expenseData.stream().mapToDouble(Expense::getBaseAmount).sum();
        totalLabel.setText(String.format("Total: %.2f %s", total, baseCurrency));
    }

    private void updatePieChart() {
        categoryChart.getData().clear();
        for (String cat : categoryList) {
            double sum = expenseData.stream().filter(e -> e.getCategory().equals(cat)).mapToDouble(Expense::getBaseAmount).sum();
            if (sum > 0) categoryChart.getData().add(new PieChart.Data(cat, sum));
        }
    }

    private double getExchangeRate(String from, String to) {
        if (from.equals(to)) return 1.0;
        if (from.equals("USD") && to.equals("INR")) return 82.0;
        if (from.equals("INR") && to.equals("USD")) return 0.012;
        return 1.1;
    }

    public static class Expense {
        private LocalDate date;
        private String category, currency, description;
        private double amount, baseAmount;

        public Expense(LocalDate d, String c, String cur, double a, double b, String desc) {
            date = d; category = c; currency = cur; amount = a; baseAmount = b; description = desc;
        }

        public LocalDate getDate() { return date; }
        public String getCategory() { return category; }
        public String getCurrency() { return currency; }
        public double getAmount() { return amount; }
        public double getBaseAmount() { return baseAmount; }
        public String getDescription() { return description; }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
