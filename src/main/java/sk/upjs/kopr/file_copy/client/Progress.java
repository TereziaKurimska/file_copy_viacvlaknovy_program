package sk.upjs.kopr.file_copy.client;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Progress {
    private final String destinationFilePath;
    List<AtomicLong> progress = Collections.synchronizedList(new ArrayList<AtomicLong>());
    private String progressFile;
    private final DoubleProperty partSize = new SimpleDoubleProperty(0);
    private final DoubleProperty totalSize = new SimpleDoubleProperty(0);

    public DoubleProperty partSizeProperty() {
        return partSize;
    }


    public void setTotalSize(double totalSize) {
        this.totalSize.set(totalSize);
    }

    public Progress(String progressFile, String destinationFilePath) {
        this.progressFile = progressFile;
        this.destinationFilePath = destinationFilePath;
        //ked
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (Files.exists(Path.of(destinationFilePath)) && partSize != totalSize) {
                    saveProgressToFile(progressFile);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public List<AtomicLong> getProgress() {
        return progress;
    }

    public void setProgress(List<AtomicLong> progress) {
        this.progress = progress;
    }


    public void saveProgressToFile(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (AtomicLong atomicInteger : progress) {
                writer.write(atomicInteger.toString());
                writer.newLine();
            }
        }
    }

    public void loadProgressFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            progress.clear();
            while ((line = reader.readLine()) != null) {
                progress.add(new AtomicLong(Integer.parseInt(line)));
            }
        }
    }


//    public DoubleBinding valueProperty() {
//        return partSize.divide(totalSize);
//    }
}
