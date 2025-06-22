package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Controller {

    @FXML private TextField nameField;
    @FXML private TextField gradeField;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TableView<Student> tableView;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, Double> gradeColumn;
    @FXML private Label averageLabel;
    @FXML private Label highestLabel;
    @FXML private Label lowestLabel;
    @FXML private PieChart gradeChart;
    @FXML private Button addButton;
    @FXML private Button saveButton;

    private final ObservableList<Student> studentList = FXCollections.observableArrayList();
    private final FilteredList<Student> filteredList = new FilteredList<>(studentList, p -> true);

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        tableView.setItems(filteredList);

        addButton.setOnAction(e -> handleAddGrade());
        saveButton.setOnAction(e -> handleSaveToFile());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterCombo.getItems().addAll("All", "Excellent", "Good", "Average", "Poor");
        filterCombo.setValue("All");
        filterCombo.setOnAction(e -> applyFilters());
    }

    private void handleAddGrade() {
        String name = nameField.getText().trim();
        String gradeText = gradeField.getText().trim();

        if (name.isEmpty() || gradeText.isEmpty()) {
            showAlert("Please enter both name and grade.");
            return;
        }

        double grade;
        try {
            grade = Double.parseDouble(gradeText);
            if (grade < 0 || grade > 100) {
                showAlert("Grade must be between 0 and 100.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid grade. Please enter a number.");
            return;
        }

        studentList.add(new Student(name, grade));
        nameField.clear();
        gradeField.clear();
        updateStats();
        updateChart();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String filter = filterCombo.getValue();

        filteredList.setPredicate(student -> {
            boolean matchesName = student.getName().toLowerCase().contains(search);
            boolean matchesCategory = switch (filter) {
                case "Excellent" -> student.getGrade() >= 85;
                case "Good" -> student.getGrade() >= 70 && student.getGrade() < 85;
                case "Average" -> student.getGrade() >= 50 && student.getGrade() < 70;
                case "Poor" -> student.getGrade() < 50;
                default -> true;
            };
            return matchesName && matchesCategory;
        });
    }

    private void updateStats() {
        if (studentList.isEmpty()) {
            averageLabel.setText("Average: -");
            highestLabel.setText("Highest: -");
            lowestLabel.setText("Lowest: -");
            return;
        }

        double sum = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Student s : studentList) {
            double g = s.getGrade();
            sum += g;
            if (g > max) max = g;
            if (g < min) min = g;
        }

        double avg = sum / studentList.size();
        averageLabel.setText(String.format("Average: %.2f", avg));
        highestLabel.setText(String.format("Highest: %.2f", max));
        lowestLabel.setText(String.format("Lowest: %.2f", min));
    }

    private void updateChart() {
        int excellent = 0, good = 0, average = 0, poor = 0;

        for (Student s : studentList) {
            double g = s.getGrade();
            if (g >= 85) excellent++;
            else if (g >= 70) good++;
            else if (g >= 50) average++;
            else poor++;
        }

        gradeChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Excellent (85+)", excellent),
                new PieChart.Data("Good (70–84)", good),
                new PieChart.Data("Average (50–69)", average),
                new PieChart.Data("Poor (<50)", poor)
        ));
    }

    private void handleSaveToFile() {
        if (studentList.isEmpty()) {
            showAlert("No student data to save.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Grades to File");
        fileChooser.setInitialFileName("grades.txt");
        File file = fileChooser.showSaveDialog(saveButton.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                for (Student s : studentList) {
                    writer.write(s.getName() + " - " + s.getGrade() + "\n");
                }
                showAlert("Data saved successfully to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Failed to save data: " + e.getMessage());
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Grade Tracker");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}