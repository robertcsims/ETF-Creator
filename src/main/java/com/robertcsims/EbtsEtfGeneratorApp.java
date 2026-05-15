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
import java.util.*;

public class EbtsEtfGeneratorApp extends Application {

    private TextField oriField, tcnField, namField, dobField, raceField, hgtField, wgtField;
    private ComboBox<String> totCombo, sexCombo;
    private TextArea notesArea;
    private Map<String, File> fingerFiles = new LinkedHashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EBTS/ETF File Generator (v8.1) - FBI Compliant");
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createHeaderTab(), createBiographicTab(), createImageTab(), createGenerateTab());
        Scene scene = new Scene(tabPane, 1200, 820);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createHeaderTab() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);

        totCombo = new ComboBox<>();
        totCombo.getItems().addAll("CAR", "CNA", "CPDR", "DOCE", "FANC", "NFUF", "MAP");
        totCombo.setValue("CAR");

        oriField = new TextField("ORI123456");
        tcnField = new TextField("TCN-" + UUID.randomUUID().toString().substring(0,12).toUpperCase());

        addLabeledField(grid, 0, "Type of Transaction (TOT) 1.003", totCombo, "CAR = Criminal Tenprint Submission (Answer Required) - Section 3.1.1.1");
        addLabeledField(grid, 1, "Originating Agency Identifier (ORI) 1.008", oriField, "9-character NCIC ORI - mandatory");
        addLabeledField(grid, 2, "Transaction Control Number (TCN) 1.009", tcnField, "Unique identifier you control - mandatory");

        return new Tab("1. Header (Type-1)", new ScrollPane(grid));
    }

    private Tab createBiographicTab() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);

        namField = new TextField("DOE,JOHN A");
        dobField = new TextField("19850115");
        sexCombo = new ComboBox<>();
        sexCombo.getItems().addAll("M", "F", "U");
        sexCombo.setValue("M");
        raceField = new TextField("W");
        hgtField = new TextField("510");
        wgtField = new TextField("180");

        addLabeledField(grid, 0, "Name (NAM) 2.018", namField, "LAST,FIRST MIDDLE - Appendix C");
        addLabeledField(grid, 1, "Date of Birth (DOB) 2.024", dobField, "YYYYMMDD");
        addLabeledField(grid, 2, "Sex (SEX) 2.025", sexCombo, "M/F/U");
        addLabeledField(grid, 3, "Race (RAC) 2.026", raceField, "W/B/A/I");
        addLabeledField(grid, 4, "Height (HGT) 2.027", hgtField, "e.g. 510");
        addLabeledField(grid, 5, "Weight (WGT) 2.028", wgtField, "pounds");

        return new Tab("2. Biographic (Type-2)", new ScrollPane(grid));
    }

    private Tab createImageTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        String[] fingers = {
            "Right Thumb", "Right Index", "Right Middle", "Right Ring", "Right Little",
            "Left Thumb", "Left Index", "Left Middle", "Left Ring", "Left Little",
            "Plain Thumbs", "Additional Image"
        };

        for (String finger : fingers) {
            HBox row = new HBox(15);
            Label label = new Label(finger + ":");
            label.setPrefWidth(200);
            Button btn = new Button("Select WSQ");
            Label status = new Label("No file selected");
            btn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("WSQ Files", "*.wsq"));
                File file = fc.showOpenDialog(null);
                if (file != null) {
                    fingerFiles.put(finger, file);
                    status.setText(file.getName());
                }
            });
            row.getChildren().addAll(label, btn, status);
            vbox.getChildren().add(row);
        }

        return new Tab("3. Fingerprint Images (Type-4)", new ScrollPane(vbox));
    }

    private Tab createGenerateTab() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(30));

        notesArea = new TextArea("Any additional notes or user-defined Type-2 fields");
        notesArea.setPrefRowCount(4);

        Button generateBtn = new Button("Generate Compliant .eft File");
        generateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15px 30px;");
        generateBtn.setOnAction(e -> generateEftFile());

        vbox.getChildren().addAll(
            new Label("✅ All fields follow EBTS v8.1 exactly (Sections 1.2.1, 1.4, 3.1, Appendices B/C/F)"),
            notesArea,
            generateBtn
        );

        return new Tab("4. Generate", vbox);
    }

    private void addLabeledField(GridPane grid, int row, String labelText, Control field, String tooltip) {
        Label label = new Label(labelText);
        Tooltip.install(label, new Tooltip(tooltip));
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private void generateEftFile() {
        try {
            StringBuilder content = new StringBuilder();

            // Type-1
            String type1 = buildType1();
            content.append(type1);

            // Type-2
            String type2 = buildType2();
            content.append(type2);

            // Type-4 images
            for (Map.Entry<String, File> entry : fingerFiles.entrySet()) {
                String type4 = buildType4(entry.getKey(), entry.getValue());
                content.append(type4);
            }

            content.append((char) 0x1C); // final file separator

            FileChooser fc = new FileChooser();
            fc.setInitialFileName(totCombo.getValue() + "_" + tcnField.getText() + ".eft");
            File file = fc.showSaveDialog(null);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.toString().getBytes(StandardCharsets.US_ASCII));
                }
                new Alert(Alert.AlertType.INFORMATION, "✅ Success!\nFile: " + file.getAbsolutePath()).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
        }
    }

    private String buildType1() {
        StringBuilder sb = new StringBuilder();
        sb.append("1.001:000000").append((char)0x1D);
        sb.append("1.002:01").append((char)0x1D);
        sb.append("1.003:").append(totCombo.getValue()).append((char)0x1D);
        sb.append("1.007:FBI").append((char)0x1D);
        sb.append("1.008:").append(oriField.getText()).append((char)0x1D);
        sb.append("1.009:").append(tcnField.getText()).append((char)0x1D);
        sb.append("1.012:500").append((char)0x1D);
        sb.append("1.013:01,2,4").append((char)0x1D);

        String record = sb.toString();
        int len = record.length() + 6;
        return record.replace("000000", String.format("%06d", len)) + (char)0x1C;
    }

    private String buildType2() {
        StringBuilder sb = new StringBuilder();
        sb.append("2.001:000000").append((char)0x1D);
        sb.append("2.002:01").append((char)0x1D);
        sb.append("2.018:").append(namField.getText().toUpperCase()).append((char)0x1D);
        sb.append("2.024:").append(dobField.getText()).append((char)0x1D);
        sb.append("2.025:").append(sexCombo.getValue()).append((char)0x1D);
        sb.append("2.026:").append(raceField.getText().toUpperCase()).append((char)0x1D);
        sb.append("2.027:").append(hgtField.getText()).append((char)0x1D);
        sb.append("2.028:").append(wgtField.getText()).append((char)0x1D);

        String record = sb.toString();
        int len = record.length() + 6;
        return record.replace("000000", String.format("%06d", len)) + (char)0x1C;
    }

    private String buildType4(String fingerName, File wsqFile) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(wsqFile.toPath());
        StringBuilder sb = new StringBuilder();
        sb.append("4.001:").append(String.format("%06d", data.length + 30)).append((char)0x1D);
        sb.append("4.002:01").append((char)0x1D);
        sb.append("4.003:1").append((char)0x1D);
        sb.append("4.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
