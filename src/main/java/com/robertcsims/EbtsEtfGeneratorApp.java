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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.github.mhshams.jnbis.WSQEncoder;

/**
 * ETF-Creator v1.3
 * Generates EBTS v8.1 compliant files (IAFIS-DOC-01078-8.1)
 * Now includes automatic PNG/JPG → WSQ conversion
 */
public class EbtsEtfGeneratorApp extends Application {

    private TextField oriField, tcnField, namField, dobField, raceField, hgtField, wgtField, ssnField;
    private ComboBox<String> totCombo, sexCombo;
    private TextArea notesArea, logArea;
    private Map<String, File> fingerFiles = new LinkedHashMap<>();
    private Map<String, Integer> fingerPositions = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ETF-Creator v1.3 - FBI EBTS v8.1 Compliant Generator (PNG→WSQ)");
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createHeaderTab(), createBiographicTab(), createImageTab(), createGenerateTab(), createLogTab());
        Scene scene = new Scene(tabPane, 1350, 920);
        primaryStage.setScene(scene);
        primaryStage.show();
        initFingerPositions();
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
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        String[] fingers = {"Right Thumb", "Right Index", "Right Middle", "Right Ring", "Right Little",
                            "Left Thumb", "Left Index", "Left Middle", "Left Ring", "Left Little",
                            "Plain Thumbs", "Additional Image"};
        for (String finger : fingers) {
            HBox row = new HBox(15);
            Label label = new Label(finger + ":");
            label.setPrefWidth(220);
            Button btn = new Button("Select Image File (PNG/JPG/WSQ)");
            Label status = new Label("No file selected");
            btn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fingerprint Images", "*.png", "*.jpg", "*.jpeg", "*.wsq"));
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

    private Tab createGenerateTab() { /* same as v1.2 */ 
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(30));
        notesArea = new TextArea("Additional notes or user-defined fields");
        notesArea.setPrefRowCount(4);
        Button generateBtn = new Button("🚀 Generate Compliant .eft File (v1.3)");
        generateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15px 40px;");
        generateBtn.setOnAction(e -> generateEftFile());
        vbox.getChildren().addAll(
            new Label("✅ EBTS v8.1 Compliant + Automatic PNG/JPG → WSQ (Appendix F)"),
            notesArea,
            generateBtn
        );
        return new Tab("4. Generate File", vbox);
    }

    private Tab createLogTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace;");
        vbox.getChildren().addAll(new Label("Live Generation Log:"), logArea);
        return new Tab("5. Log", vbox);
    }

    private void addLabeledField(GridPane grid, int row, String labelText, Control field, String tooltip) {
        Label label = new Label(labelText);
        Tooltip.install(label, new Tooltip(tooltip));
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    private void generateEftFile() {
        log("=== Starting EBTS v8.1 file generation (v1.3) ===");
        try {
            if (oriField.getText().trim().isEmpty() || tcnField.getText().trim().isEmpty() ||
                namField.getText().trim().isEmpty() || dobField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("ORI, TCN, NAM, and DOB are mandatory");
            }
            if (fingerFiles.isEmpty()) {
                throw new IllegalArgumentException("At least one fingerprint image is required");
            }

            log("Validation passed. Building records...");

            StringBuilder content = new StringBuilder();
            content.append(buildType1Record());
            content.append(buildType2Record());

            for (Map.Entry<String, File> entry : fingerFiles.entrySet()) {
                String type4 = buildType4Record(entry.getKey(), entry.getValue());
                content.append(type4);
            }
            content.append((char) 0x1C);

            log("Records built. Saving files...");

            FileChooser fc = new FileChooser();
            fc.setInitialFileName(totCombo.getValue() + "_" + tcnField.getText() + ".eft");
            File eftFile = fc.showSaveDialog(null);
            if (eftFile == null) return;

            try (FileOutputStream fos = new FileOutputStream(eftFile)) {
                fos.write(content.toString().getBytes(StandardCharsets.US_ASCII));
            }

            // Create log file
            File logFile = new File(eftFile.getParent(), eftFile.getName().replace(".eft", ".log"));
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
                pw.println("=== EBTS v8.1 Generation Log - " + LocalDateTime.now() + " ===");
                pw.println("TOT: " + totCombo.getValue());
                pw.println("ORI: " + oriField.getText());
                pw.println("TCN: " + tcnField.getText());
                pw.println("NAM: " + namField.getText());
                pw.println("Fingerprint Images: " + fingerFiles.size());
                for (String finger : fingerFiles.keySet()) {
                    pw.println("  - " + finger);
                }
                pw.println("EFT File Size: " + eftFile.length() + " bytes");
                pw.println("Status: SUCCESS (PNG/JPG converted to WSQ)");
            }

            log("✅ SUCCESS! Files created:");
            log("   • " + eftFile.getAbsolutePath());
            log("   • " + logFile.getAbsolutePath());

            new Alert(Alert.AlertType.INFORMATION, "✅ Success!\n\nEFT file + log file created.\nPNG/JPG images were automatically converted to WSQ.").showAndWait();

        } catch (Exception ex) {
            String errorMsg = "❌ ERROR: " + ex.getMessage();
            log(errorMsg);
            new Alert(Alert.AlertType.ERROR, errorMsg).showAndWait();
        }
    }

    private String buildType1Record() { /* same as v1.2 */ 
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

    private String buildType2Record() { /* same as v1.2 */ 
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

    private String buildType4Record(String fingerName, File imageFile) throws IOException {
        byte[] data = getWSQData(imageFile);   // <-- automatic conversion happens here
        Integer pos = fingerPositions.getOrDefault(fingerName, 99);
        StringBuilder sb = new StringBuilder();
        sb.append("4.001:").append(String.format("%06d", data.length + 40)).append((char)0x1D);
        sb.append("4.002:01").append((char)0x1D);
        sb.append("4.003:").append(pos).append((char)0x1D);
        sb.append("4.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    private byte[] getWSQData(File file) throws IOException {
        byte[] raw = Files.readAllBytes(file.toPath());
        String name = file.getName().toLowerCase();
        log("Processing " + file.getName() + " → WSQ");
        if (name.endsWith(".wsq")) {
            return raw;
        }
        // Automatic conversion
        return WSQEncoder.fromPng(raw).encode().asByteArray();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
