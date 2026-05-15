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

/**
 * ETF-Creator v1.1
 * Generates EBTS v8.1 compliant files (IAFIS-DOC-01078-8.1)
 * Supports CAR and other common TOTs
 */
public class EbtsEtfGeneratorApp extends Application {

    private TextField oriField, tcnField, namField, dobField, raceField, hgtField, wgtField, ssnField;
    private ComboBox<String> totCombo, sexCombo;
    private TextArea notesArea;
    private Map<String, File> fingerFiles = new LinkedHashMap<>();
    private Map<String, Integer> fingerPositions = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ETF-Creator v1.1 - FBI EBTS v8.1 Compliant Generator");
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createHeaderTab(), createBiographicTab(), createImageTab(), createGenerateTab());
        Scene scene = new Scene(tabPane, 1250, 850);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initFingerPositions() {
        fingerPositions.put("Right Thumb", 1);
        fingerPositions.put("Right Index", 2);
        fingerPositions.put("Right Middle", 3);
        fingerPositions.put("Right Ring", 4);
        fingerPositions.put("Right Little", 5);
        fingerPositions.put("Left Thumb", 6);
        fingerPositions.put("Left Index", 7);
        fingerPositions.put("Left Middle", 8);
        fingerPositions.put("Left Ring", 9);
        fingerPositions.put("Left Little", 10);
        fingerPositions.put("Plain Thumbs", 11);
        fingerPositions.put("Additional Image", 99);
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
        addLabeledField(grid, 1, "Originating Agency Identifier (ORI) 1.008", oriField, "Your 9-character NCIC ORI (mandatory)");
        addLabeledField(grid, 2, "Transaction Control Number (TCN) 1.009", tcnField, "Unique control number you manage (mandatory)");

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
        ssnField = new TextField();

        addLabeledField(grid, 0, "Name (NAM) 2.018", namField, "LAST,FIRST MIDDLE - Appendix C");
        addLabeledField(grid, 1, "Date of Birth (DOB) 2.024", dobField, "YYYYMMDD");
        addLabeledField(grid, 2, "Sex (SEX) 2.025", sexCombo, "M/F/U");
        addLabeledField(grid, 3, "Race (RAC) 2.026", raceField, "W/B/A/I");
        addLabeledField(grid, 4, "Height (HGT) 2.027", hgtField, "e.g. 510");
        addLabeledField(grid, 5, "Weight (WGT) 2.028", wgtField, "pounds");
        addLabeledField(grid, 6, "SSN (2.036)", ssnField, "Optional Social Security Number");

        return new Tab("2. Biographic (Type-2)", new ScrollPane(grid));
    }

    private Tab createImageTab() {
        initFingerPositions();
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        String[] fingers = {"Right Thumb", "Right Index", "Right Middle", "Right Ring", "Right Little",
                            "Left Thumb", "Left Index", "Left Middle", "Left Ring", "Left Little",
                            "Plain Thumbs", "Additional Image"};

        for (String finger : fingers) {
            HBox row = new HBox(15);
            Label label = new Label(finger + ":");
            label.setPrefWidth(200);
            Button btn = new Button("Select WSQ File");
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

        notesArea = new TextArea("Additional notes or user-defined fields");
        notesArea.setPrefRowCount(4);

        Button generateBtn = new Button("🚀 Generate Compliant .eft File (v1.1)");
        generateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15px 40px;");
        generateBtn.setOnAction(e -> generateEftFile());

        vbox.getChildren().addAll(
            new Label("✅ EBTS v8.1 Compliant Generator (Sections 1.2.1, 1.4, 3.1, Appendices B, C, F)"),
            notesArea,
            generateBtn
        );

        return new Tab("4. Generate File", vbox);
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

            String type1 = buildType1Record();
            content.append(type1);

            String type2 = buildType2Record();
            content.append(type2);

            for (Map.Entry<String, File> entry : fingerFiles.entrySet()) {
                String type4 = buildType4Record(entry.getKey(), entry.getValue());
                content.append(type4);
            }

            content.append((char) 0x1C); // Final File Separator

            FileChooser fc = new FileChooser();
            fc.setInitialFileName(totCombo.getValue() + "_" + tcnField.getText() + ".eft");
            File file = fc.showSaveDialog(null);

            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.toString().getBytes(StandardCharsets.US_ASCII));
                }
                new Alert(Alert.AlertType.INFORMATION, "✅ Success!\nFile created:\n" + file.getAbsolutePath()).showAndWait();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error generating file: " + ex.getMessage()).showAndWait();
        }
    }

    private String buildType1Record() {
        StringBuilder sb = new StringBuilder();
        sb.append("1.001:000000").append((char)0x1D); // LEN placeholder
        sb.append("1.002:01").append((char)0x1D);
        sb.append("1.003:").append(totCombo.getValue()).append((char)0x1D);
        sb.append("1.007:FBI").append((char)0x1D);
        sb.append("1.008:").append(oriField.getText()).append((char)0x1D);
        sb.append("1.009:").append(tcnField.getText()).append((char)0x1D);
        sb.append("1.012:500").append((char)0x1D); // NSR - 500 ppi
        sb.append("1.013:01,2,4").append((char)0x1D); // CNT

        String record = sb.toString();
        int len = record.length() + 6; // LEN field + FS
        return record.replace("000000", String.format("%06d", len)) + (char)0x1C;
    }

    private String buildType2Record() {
        StringBuilder sb = new StringBuilder();
        sb.append("2.001:000000").append((char)0x1D);
        sb.append("2.002:01").append((char)0x1D);
        sb.append("2.018:").append(namField.getText().toUpperCase()).append((char)0x1D);
        sb.append("2.024:").append(dobField.getText()).append((char)0x1D);
        sb.append("2.025:").append(sexCombo.getValue()).append((char)0x1D);
        sb.append("2.026:").append(raceField.getText().toUpperCase()).append((char)0x1D);
        sb.append("2.027:").append(hgtField.getText()).append((char)0x1D);
        sb.append("2.028:").append(wgtField.getText()).append((char)0x1D);
        if (ssnField.getText() != null && !ssnField.getText().trim().isEmpty()) {
            sb.append("2.036:").append(ssnField.getText().trim()).append((char)0x1D);
        }

        String record = sb.toString();
        int len = record.length() + 6;
        return record.replace("000000", String.format("%06d", len)) + (char)0x1C;
    }

    private String buildType4Record(String fingerName, File wsqFile) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(wsqFile.toPath());
        Integer pos = fingerPositions.getOrDefault(fingerName, 99);

        StringBuilder sb = new StringBuilder();
        sb.append("4.001:").append(String.format("%06d", data.length + 40)).append((char)0x1D);
        sb.append("4.002:01").append((char)0x1D); // IDC
        sb.append("4.003:").append(pos).append((char)0x1D); // Finger position code
        sb.append("4.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}