package sk.upjs.kopr.file_copy.client;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import sk.upjs.kopr.file_copy.FileInfo;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileDownloadService extends Service<Props> {
    private final File destinationFile;
    private final Runnable completionCallback;
    private long totalFileSize;
    private long downloadedBytes;
    private ExecutorService executorService;
    private FileInfoReceiver fileInfoReceiver;
    private Progress progress;
    private int threadCount;

    public FileDownloadService(File destinationFile, Progress progress, int threadCount, Runnable completionCallback) {
        this.destinationFile = destinationFile;
        this.completionCallback = completionCallback;
        this.downloadedBytes = destinationFile.length();
        this.progress = progress;
        this.threadCount = threadCount;
    }
//    public boolean isNumeric(String str) {
//        return str != null && str.matches("\\d+");
//    }

    @Override
    protected Task<Props> createTask() {
        return new Task<>() {

            @Override
            protected Props call() throws InterruptedException, ExecutionException, IOException, RuntimeException{
                if (threadCount <= 0) {
                    throw new IllegalArgumentException("Thread count must be a positive integer.");
                }
                try {
                    //ziadam si info o subore, setujem totalFileSize pre progres
                    fileInfoReceiver = new FileInfoReceiver("localhost", 5000, destinationFile);
                    FileInfo fileInfo = fileInfoReceiver.getFileInfo();
                    totalFileSize = fileInfo.size;
                    progress.setTotalSize(totalFileSize);
                } catch (RuntimeException e) {
//                    // Ensure server errors stop the process
//                    serverStoppedCallback.run();
                    throw e;
                }

                //vytvaram executor service, countDownLatch
                executorService = Executors.newFixedThreadPool(threadCount);
                //bude cakat na dokoncenie uloh, synchronizuje vlakna
                CountDownLatch latch = new CountDownLatch(threadCount);

                long chunkSize = totalFileSize / threadCount;
                long extraBytes = totalFileSize % threadCount;
                long offset = 0;

                //ak je progress prazdny, pridam do neho atomicke longy s initial value 0
                if (progress.getProgress().size() == 0) {
                    for (int i = 0; i < threadCount; i++) {
                        progress.getProgress().add(new AtomicLong(0));
                    }
                }

                Props props = new Props(0, totalFileSize);
                //vytvorim novy thread na update progressu
                var propsExecutor = Executors.newScheduledThreadPool(1);

//                scheduleAtFixedRate(
//                Runnable command,   // Úloha, ktorú chcete vykonávať (tu lambda funkcia)
//                long initialDelay,  // Počiatočné oneskorenie pred prvým vykonaním
//                long period,        // Interval medzi začiatkami po sebe nasledujúcich vykonaní
//                TimeUnit unit       // Jednotka času (napr. milisekundy)
//)
                propsExecutor.scheduleAtFixedRate(
                        () -> {
                            double temp = 0;
                            for (AtomicLong val : progress.getProgress()) {
                                temp += val.get();
                            }
                            //nastavim hodnotu stiahnutych bytov do props
                            props.setDown(temp);
                            //updatnem hodnotu v progress na notifikaciu listenera
                            updateValue(new Props(props.getDown(), props.getTotal()));
                        },
                        0,
                        20,
                        TimeUnit.MILLISECONDS
                );

                //vytvaram tasky na stahovanie suboru
                for (int i = 0; i < threadCount; i++) {
                    //vypocitam chunkSize, v pripade, ze pokracujem v stahovani
                    long currentChunkSize = chunkSize - progress.getProgress().get(i).get();
                    //riesim posledny thread, ktory stahuje zvysok bytov
                    if (i == threadCount - 1) {
                        currentChunkSize += extraBytes;
                    }
                    //musim posunut offset, ak uz bol nejaky progress
                    offset += progress.getProgress().get(i).get();
                    try {
                        //vytvaram task, ktory submitnem
                        FileReceiveTask task = new FileReceiveTask(destinationFile, totalFileSize, offset, currentChunkSize, InetAddress.getLoopbackAddress(), 5000, latch, i, progress);
                        executorService.submit(task);
                        offset += currentChunkSize;
                    } catch (RuntimeException e) {
                        System.out.println("Downloading interrupted.");
                        throw new RuntimeException(e);
                    }
                }

                try {
                    //cakam kym vsetky tasky skoncia
                    latch.await();
                } catch (InterruptedException e) {
                    System.out.println("Downloading interrupted");
                    throw new RuntimeException(e);
                } catch (RuntimeException e) {
                    System.out.println("Downloading interrupted");
                    throw new RuntimeException(e);
                }finally {
                    //ukoncim executor service, zavolam callback, ze som skoncil
                    MyFileWriter.getInstance(destinationFile, totalFileSize).close();
                    completionCallback.run();
                    System.out.println("Downloading done");
                    executorService.shutdown();
                }
                throw new InterruptedException();
            }
        };
    }
}