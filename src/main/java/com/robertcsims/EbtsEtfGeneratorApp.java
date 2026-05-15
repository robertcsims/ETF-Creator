package com.robertcsims;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class EbtsEtfGeneratorApp extends Application {

    private TextField oriField, tcnField, namField, dobField, sexField, raceField, hgtField, wgtField;
    private ComboBox<String> totCombo;
    private Map<String, File> fingerFiles = new LinkedHashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ETF Creator - FBI EBTS v8.1 Compliant");

        TabPane tabPane = new TabPane();

        tabPane.getTabs().addAll(createHeaderTab(), createBiographicTab(), createImageTab(), createGenerateTab());

        Scene scene = new Scene(tabPane, 1150, 780);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ... (full implementation as previously provided, shortened here for brevity)
    // Note: Full code would be inserted here

    public static void main(String[] args) {
        launch(args);
    }
}
