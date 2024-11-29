package sk.upjs.kopr.file_copy.client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.scene.control.Alert;
import sk.upjs.kopr.file_copy.FileInfo;
import sk.upjs.kopr.file_copy.server.Server;

public class FileInfoReceiver {
    private int serverPort;
    private InetAddress inetAddress;

    private static File destinationFile;

    public FileInfoReceiver(String serverHost, int serverPort, File destinationFile) throws UnknownHostException {
        inetAddress = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;
        this.destinationFile = destinationFile;
    }

    public static FileInfo getLocalhostServerFileInfo() {
        try {
            FileInfoReceiver fir = new FileInfoReceiver("localhost", Server.SERVER_PORT, destinationFile);
            return fir.getFileInfo();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public FileInfo getFileInfo() {
        FileInfo fileInfo = null;
        //pripajam sa na server, ziadam info o subore
        try (Socket socket = new Socket(inetAddress, serverPort)) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeUTF("info");
            oos.flush();
            fileInfo = (FileInfo) ois.readObject();
            oos.close();
            ois.close();
            //ak server nebezi, vyhodim chybu, vymazem destination file
        } catch (SocketException e) {
            System.err.println("Server was stopped or unreachable: " + e.getMessage());
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Server Error");
                alert.setHeaderText(null);
                alert.setContentText("Server is not running. Start the server and try again.");
                alert.show();
                try {
                    Files.delete(Paths.get(destinationFile.getAbsolutePath()));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            throw new RuntimeException("Server connection issue", e);
        } catch (ClassNotFoundException e) {
            System.err.println("Wrong FileInfo format received");
        } catch (IOException e) {
            System.err.println("Server connection problem. Is the server running?");
        }
        return fileInfo;
    }

}
