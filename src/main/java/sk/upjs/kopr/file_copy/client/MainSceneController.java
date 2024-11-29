package sk.upjs.kopr.file_copy.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

public class MainSceneController {

    @FXML
    private Label destinatioLabel;
    @FXML
    private Label destinationRoadLabel;
    @FXML
    private Label fileRoadLabel;
    @FXML
    private Label fileToCopyLabel;

    @FXML
    private ProgressBar progress1;

    @FXML
    private Label filesProgressLabel;
    @FXML
    private Label mbProgressLabel;
    @FXML
    private TextField numberOfThreadsTextField;
    @FXML
    private Button startButton;

    private final String sourceFilePath = "C:/Users/terez/Desktop/kopr/dow/kopr.exe";
    private final String destinationFilePath = "C:/Users/terez/Desktop/kopr/up/kopr.exe";
    private final String progressFilePath = "C:/Users/terez/Desktop/kopr/up/progress.txt";

    private File destinationFile;
    private FileDownloadService fileDownloadService;
    private Progress progress = new Progress(progressFilePath, destinationFilePath);

    @FXML
    void initialize() throws IOException, ClassNotFoundException {
        fileRoadLabel.setText(sourceFilePath);
        destinationRoadLabel.setText(destinationFilePath);

        //ak existuje subor s progresom, nacitaj ho, a pokracuj v stahovani
        if (Files.exists(Paths.get(progressFilePath))) {
            progress.loadProgressFromFile(progressFilePath);
            numberOfThreadsTextField.setDisable(true);
            numberOfThreadsTextField.setText(progress.getProgress().size() + "");
            startButton.setText("Resume");
        }
    }


    @FXML
    void onStartButtonClick(ActionEvent event) throws IOException {
        Path path = Paths.get(destinationFilePath);
        //kontrolujem, ci existuje destinantion subor, ak nie, vytvorim ho
        if (!Files.exists(path)) {
            destinationFile = Files.createFile(path).toFile();
        } else {
            destinationFile = path.toFile();
        }

        startButton.setDisable(true);
        filesProgressLabel.setText("Downloading...");
        int threadCount;
//                = Integer.parseInt(numberOfThreadsTextField.getText());

        //podmienka, thread count musi byt kladne cislo, ak nie je, tak vypiseme chybu
        try {
            threadCount = Integer.parseInt(numberOfThreadsTextField.getText());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid number of threads.");
            alert.showAndWait();
            startButton.setDisable(false);
            return;
        }
        //volam file download service, ktory stahuje subor
        fileDownloadService = new FileDownloadService(destinationFile, progress, threadCount, this::onDownloadComplete);
        startButton.setVisible(false);
        //nemozem menit pocet threadov pocas stahovania
        numberOfThreadsTextField.setDisable(true);

        //listener na progress bar, ktory zobrazuje aktualny stav stahovania
        fileDownloadService.valueProperty().addListener((ob, oldV, value) -> {
            if (value == null) {
                return;
            }
            progress1.progressProperty().setValue(value.getDown() / value.getTotal());
            mbProgressLabel.setText(value.getDown() + " / " + value.getTotal() + " bytes");

        });
        //zacni stahovanie
        fileDownloadService.start();
    }


    private void onDownloadComplete() {
        //pouziva sa na spustenie špecifikovaného Runnable na aplikačnom vlákne JavaFX v nejakom nešpecifikovanom čase v budúcnosti
        Platform.runLater(() -> {
            startButton.setDisable(false);
            filesProgressLabel.setText("Download completed");
        });
        //ak je stiahnuty cely subor, tak zmazem progress file
        if (progress.getProgress().stream().mapToLong(AtomicLong::get).sum() == destinationFile.length()) {
            try {
                Files.delete(Paths.get(progressFilePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //zatvaram, koncim file download service
        fileDownloadService.cancel();
    }
//    private void onServerStopped() {
//        Platform.runLater(() -> {
//            System.out.println("Server was stopped");
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Server Stopped");
//            alert.setContentText("The server has stopped. Please restart the server and try again.");
//            alert.showAndWait();
//
//            // Properly close the application window
//            Stage stage = (Stage) startButton.getScene().getWindow();
//            stage.close(); // Ensure the window closes here
//        });
//    }


    //        }
//        try {
//            progress.saveProgressToFile(progressFilePath);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

}
