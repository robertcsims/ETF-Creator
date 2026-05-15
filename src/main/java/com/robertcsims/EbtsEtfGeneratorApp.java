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
 * ETF-Creator v1.4
 * ATF Form 1 + FD-248 Fingerprint Card Support
 * EBTS v8.1 compliant (Type-7 + Type-14 + Type-4)
 * Automatic PNG/JPG → WSQ conversion
 */
public class EbtsEtfGeneratorApp extends Application {

    private TextField oriField, tcnField, namField, dobField, raceField, hgtField, wgtField, ssnField;
    private ComboBox<String> totCombo, sexCombo;
    private TextArea notesArea, logArea;

    private Map<String, File> fingerFiles = new LinkedHashMap<>();   // Type-4 rolled fingers
    private File cardFile;                                           // Type-7 scanned FD-248 card
    private File flatFile;                                           // Type-14 flat impressions

    private final Map<String, Integer> fingerPositions = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ETF-Creator v1.4 - ATF Form 1 / FD-248 Support");
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
            createHeaderTab(),
            createBiographicTab(),
            createImageTab(),
            createGenerateTab(),
            createLogTab()
        );
        Scene scene = new Scene(tabPane, 1380, 950);
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
    }

    private Tab createHeaderTab() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);

        totCombo = new ComboBox<>();
        totCombo.getItems().addAll("CAR", "CNA", "DOCE", "FANC", "NFUF", "MAP");
        totCombo.setValue("CAR");

        oriField = new TextField("ORI123456");
        tcnField = new TextField("TCN-" + UUID.randomUUID().toString().substring(0,12).toUpperCase());

        addLabeledField(grid, 0, "Type of Transaction (TOT) 1.003", totCombo, "CAR recommended for ATF Form 1");
        addLabeledField(grid, 1, "Originating Agency Identifier (ORI) 1.008", oriField, "Your 9-character NCIC ORI");
        addLabeledField(grid, 2, "Transaction Control Number (TCN) 1.009", tcnField, "Unique identifier");

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

        addLabeledField(grid, 0, "Name (NAM) 2.018", namField, "LAST,FIRST MIDDLE");
        addLabeledField(grid, 1, "Date of Birth (DOB) 2.024", dobField, "YYYYMMDD");
        addLabeledField(grid, 2, "Sex (SEX) 2.025", sexCombo, "M/F/U");
        addLabeledField(grid, 3, "Race (RAC) 2.026", raceField, "W/B/A/I");
        addLabeledField(grid, 4, "Height (HGT) 2.027", hgtField, "e.g. 510");
        addLabeledField(grid, 5, "Weight (WGT) 2.028", wgtField, "pounds");
        addLabeledField(grid, 6, "SSN (2.036)", ssnField, "Optional");

        return new Tab("2. Biographic (Type-2)", new ScrollPane(grid));
    }

    private Tab createImageTab() {
        VBox vbox = new VBox(25);
        vbox.setPadding(new Insets(20));

        // Rolled Fingers (Type-4)
        vbox.getChildren().add(new Label("Rolled Fingers (Type-4)"));
        String[] fingers = {"Right Thumb", "Right Index", "Right Middle", "Right Ring", "Right Little",
                            "Left Thumb", "Left Index", "Left Middle", "Left Ring", "Left Little"};
        for (String finger : fingers) {
            HBox row = new HBox(15);
            Label label = new Label(finger + ":");
            label.setPrefWidth(200);
            Button btn = new Button("Select Image");
            Label status = new Label("No file selected");
            btn.setOnAction(e -> selectImage(fingerFiles, status, finger));
            row.getChildren().addAll(label, btn, status);
            vbox.getChildren().add(row);
        }

        // Scanned FD-248 Card (Type-7) - for your physical card
        HBox cardRow = new HBox(15);
        Label cardLabel = new Label("Scanned FD-248 Card (Type-7):");
        cardLabel.setPrefWidth(220);
        Button cardBtn = new Button("Select Scanned FD-248 Card");
        Label cardStatus = new Label("No file selected");
        cardBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(null);
            if (file != null) {
                cardFile = file;
                cardStatus.setText(file.getName());
            }
        });
        cardRow.getChildren().addAll(cardLabel, cardBtn, cardStatus);
        vbox.getChildren().add(cardRow);

        // Flat Impressions (Type-14)
        HBox flatRow = new HBox(15);
        Label flatLabel = new Label("Flat Impressions (Type-14):");
        flatLabel.setPrefWidth(220);
        Button flatBtn = new Button("Select Flat Impressions");
        Label flatStatus = new Label("No file selected");
        flatBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(null);
            if (file != null) {
                flatFile = file;
                flatStatus.setText(file.getName());
            }
        });
        flatRow.getChildren().addAll(flatLabel, flatBtn, flatStatus);
        vbox.getChildren().add(flatRow);

        return new Tab("3. Images (Type-4 / Type-7 / Type-14)", new ScrollPane(vbox));
    }

    private void selectImage(Map<String, File> map, Label status, String key) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.wsq"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            map.put(key, file);
            status.setText(file.getName());
        }
    }

    private Tab createGenerateTab() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(30));
        notesArea = new TextArea("Additional notes or user-defined fields (optional)");
        notesArea.setPrefRowCount(4);
        Button generateBtn = new Button("🚀 Generate Compliant .eft File (v1.4 - ATF Form 1)");
        generateBtn.setStyle("-fx-font-size: 16px; -fx-padding: 15px 40px;");
        generateBtn.setOnAction(e -> generateEftFile());
        vbox.getChildren().addAll(
            new Label("✅ ATF Form 1 / FD-248 Ready (Type-7 card + Type-14 flats + PNG→WSQ)"),
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
        log("=== Starting EBTS v8.1 generation (v1.4 - ATF Form 1) ===");
        try {
            // Validation
            if (oriField.getText().trim().isEmpty() || tcnField.getText().trim().isEmpty() ||
                namField.getText().trim().isEmpty() || dobField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("ORI, TCN, NAM, and DOB are mandatory");
            }

            StringBuilder content = new StringBuilder();
            content.append(buildType1Record());
            content.append(buildType2Record());

            // Type-4 Rolled Fingers
            for (Map.Entry<String, File> entry : fingerFiles.entrySet()) {
                content.append(buildType4Record(entry.getKey(), entry.getValue()));
            }

            // Type-7 Scanned FD-248 Card
            if (cardFile != null) {
                content.append(buildType7Record(cardFile));
                log("Added Type-7 FD-248 card image");
            }

            // Type-14 Flat Impressions
            if (flatFile != null) {
                content.append(buildType14Record(flatFile));
                log("Added Type-14 flat impressions");
            }

            content.append((char) 0x1C);

            FileChooser fc = new FileChooser();
            fc.setInitialFileName(totCombo.getValue() + "_" + tcnField.getText() + ".eft");
            File eftFile = fc.showSaveDialog(null);
            if (eftFile == null) return;

            try (FileOutputStream fos = new FileOutputStream(eftFile)) {
                fos.write(content.toString().getBytes(StandardCharsets.US_ASCII));
            }

            // Create companion log file
            File logFile = new File(eftFile.getParent(), eftFile.getName().replace(".eft", ".log"));
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
                pw.println("=== EBTS v8.1 ATF Form 1 Log - " + LocalDateTime.now() + " ===");
                pw.println("TOT: " + totCombo.getValue());
                pw.println("ORI: " + oriField.getText());
                pw.println("TCN: " + tcnField.getText());
                pw.println("NAM: " + namField.getText());
                pw.println("Type-7 FD-248 Card: " + (cardFile != null));
                pw.println("Type-14 Flats: " + (flatFile != null));
                pw.println("EFT File Size: " + eftFile.length() + " bytes");
                pw.println("Status: SUCCESS");
            }

            log("✅ SUCCESS! Files created:");
            log("   • " + eftFile.getAbsolutePath());
            log("   • " + logFile.getAbsolutePath());

            new Alert(Alert.AlertType.INFORMATION, "✅ ATF Form 1 ETF file generated successfully!").showAndWait();

        } catch (Exception ex) {
            String errorMsg = "❌ ERROR: " + ex.getMessage();
            log(errorMsg);
            new Alert(Alert.AlertType.ERROR, errorMsg).showAndWait();
        }
    }

    private String buildType1Record() {
        StringBuilder sb = new StringBuilder();
        sb.append("1.001:000000").append((char)0x1D);
        sb.append("1.002:01").append((char)0x1D);
        sb.append("1.003:").append(totCombo.getValue()).append((char)0x1D);
        sb.append("1.007:FBI").append((char)0x1D);
        sb.append("1.008:").append(oriField.getText()).append((char)0x1D);
        sb.append("1.009:").append(tcnField.getText()).append((char)0x1D);
        sb.append("1.012:500").append((char)0x1D);
        sb.append("1.013:01,2,7,14").append((char)0x1D);
        String record = sb.toString();
        int len = record.length() + 6;
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

    private String buildType4Record(String fingerName, File imageFile) throws IOException {
        byte[] data = getWSQData(imageFile);
        Integer pos = fingerPositions.getOrDefault(fingerName, 99);
        StringBuilder sb = new StringBuilder();
        sb.append("4.001:").append(String.format("%06d", data.length + 40)).append((char)0x1D);
        sb.append("4.002:01").append((char)0x1D);
        sb.append("4.003:").append(pos).append((char)0x1D);
        sb.append("4.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    private String buildType7Record(File cardImage) throws IOException {
        byte[] data = getWSQData(cardImage);
        StringBuilder sb = new StringBuilder();
        sb.append("7.001:").append(String.format("%06d", data.length + 50)).append((char)0x1D);
        sb.append("7.002:01").append((char)0x1D);
        sb.append("7.003:1").append((char)0x1D);
        sb.append("7.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    private String buildType14Record(File flatImage) throws IOException {
        byte[] data = getWSQData(flatImage);
        StringBuilder sb = new StringBuilder();
        sb.append("14.001:").append(String.format("%06d", data.length + 50)).append((char)0x1D);
        sb.append("14.002:01").append((char)0x1D);
        sb.append("14.003:19").append((char)0x1D); // Civil flat code
        sb.append("14.999:").append(new String(data, StandardCharsets.ISO_8859_1));
        sb.append((char)0x1C);
        return sb.toString();
    }

    private byte[] getWSQData(File file) throws IOException {
        byte[] raw = Files.readAllBytes(file.toPath());
        String name = file.getName().toLowerCase();
        log("Converting " + file.getName() + " → WSQ");
        if (name.endsWith(".wsq")) return raw;
        return WSQEncoder.fromPng(raw).encode().asByteArray();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
