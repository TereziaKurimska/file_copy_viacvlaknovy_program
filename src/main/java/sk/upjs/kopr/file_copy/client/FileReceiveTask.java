package sk.upjs.kopr.file_copy.client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import javafx.scene.control.Alert;
import sk.upjs.kopr.file_copy.FileRequest;

public class FileReceiveTask implements Callable<Void> {
    private static final int BUFFER_SIZE = 16384;
    private MyFileWriter myFileWriter;
    private long offset;
    private long length; // length of data to be received
    private InetAddress inetAddress;
    private int serverPort;
    private int taskIndex;

    private Progress progress;
    private CountDownLatch latch;

    public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort,
                           CountDownLatch latch, int taskIndex, Progress progress) throws IOException {
        this.offset = offset;
        this.length = length;
        this.latch = latch;
        this.inetAddress = inetAddress;
        this.serverPort = serverPort;
        myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
        this.taskIndex = taskIndex;
        this.progress = progress;
    }

    public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort,
                           CountDownLatch latch) throws IOException {
        this.offset = offset;
        this.length = length;
        this.latch = latch;
        this.inetAddress = inetAddress;
        this.serverPort = serverPort;
        myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
    }


    @Override
    public Void call() throws Exception {
        //pripajam sa na server
        try (Socket socket = new Socket(inetAddress, serverPort)) {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeUTF("file");
            oos.flush();
            //request, ktory posielam serveru, z kadial chcem cast a aku velku
            FileRequest fileRequest = new FileRequest(offset, length);
            oos.writeObject(fileRequest);
            oos.flush();
            long fileOffset = offset;
            while (true) {
                byte[] bytes = ois.readNBytes(BUFFER_SIZE);
                //ak je prazdny, koniec
                if (bytes.length == 0) {
                    break;
                }
                if (bytes.length > 0) {
                    //zapisujem do suboru, aktualizujem progress, v jednom threade
                    synchronized (progress) {
                        myFileWriter.write(fileOffset, bytes, 0, bytes.length);
                        //ziskam aktualnu hodnotu progressu, pridam k nej velkost bytes
                        progress.getProgress().get(taskIndex).addAndGet(bytes.length);
                        //ziskam aktualnu hodnotu partSize, pridam k nej velkost bytes, pre progress bar
                        progress.partSizeProperty().add(bytes.length);
                    }
//					progress.getPartSize().addAndGet(bytes.length);
                }
                if (bytes.length < BUFFER_SIZE) {
                    break;
                }
                fileOffset += bytes.length;
//				if ((fileOffset / BUFFER_SIZE) % 100 == 0)
//					System.out.println(fileOffset);
            }
        } catch (SocketException e) {
            //ak v strede stahovania vypnem server, tak vyskoci tato vynimka a zavriem vsetko
            System.out.println("Connection reset: " + e.getMessage());
            System.exit(0);

        }
//		System.out.println("Task " + taskIndex + " progress: " + progress.getProgress().get(taskIndex).get());

//		System.out.println("File received");

        //potvdrim, ze uloha je dokončená, aby vlákno mohlo pokračovať
        latch.countDown();
        return null;
    }

}
