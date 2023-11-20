package org.example;

import com.monk_digital.MonkHttp;
import org.example.service.NotificationServiceI;
import org.example.service.NotificationServiceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Client {

    private final static Logger LOGGER = Logger.getLogger(Client.class.toString());

    private final String host;
    private final int port;
    private final int threadCount;
    private static ExecutorService executorService = null;

    public Client(String host, int port, int threadCount) {
        this.host        = host;
        this.port        = port;
        this.threadCount = threadCount;
        executorService = Executors.newFixedThreadPool(threadCount);

        /*
            NotificationServiceI notificationService = new NotificationServiceImpl();
            notificationService.sendNotification();
        */
    }

    // Entry point
    public void run() {
        try
        {
            // Retrieving the list file
            LOGGER.info("Initialized the client executing the first request");
            List<String> response = MonkHttp.get(host, port, "list.txt");

            // Extracting the body
            List<String> textFileList = extractBody(response)
                    .stream()
                    .filter(item -> item.contains(".txt"))
                    .toList();

            LOGGER.info("Retrieved the list file and extracted the body");

            // Retrieving and saving the files
            int pivot = 0;
            for (String textFile : textFileList) {
                pivot += 1;
                String content = retrieveFile(textFile);
                saveFile(("file-" + pivot), content);
            }
        }
        catch (InterruptedException | IOException | ExecutionException e)
        {
            LOGGER.warning("Failed to execute the client workflow. \nException message: " + e.getMessage());
            System.exit(-1);
        }
        finally {
            executorService.shutdown();
        }
    }

    private String retrieveFile(String path) throws IOException, ExecutionException, InterruptedException {

        // Retrieving the size information about the size
        long size = retrieveFileSize(path);

        // Checks
        if (size == 0) {
            LOGGER.warning("Size for the file at path: " + path + " is zero");
            return "";
        }

        if (size < threadCount) {
            LOGGER.warning("Number of threads can't be larger than the size of the file - path: " + path);
            return "";
        }

        // Creating the boundaries for the range requests

        List<Long> ranges = new ArrayList<>();
        long partSize = size / threadCount;

        for (int counter = 1; counter <= threadCount; counter++) {
            if (counter == threadCount) {
                ranges.add(size);
                break;
            }
            ranges.add(partSize * counter);
        }

        // Creating and executing the futures
        List<Future<List<String>>> responseFutures = new ArrayList<>();

        for (int counter = 0; counter < threadCount; counter++) {
            if (counter == 0) {
                responseFutures.add(
                        sendAsyncRangeRequest(host, port, path, 0, ranges.get(0))
                );
            }
            else {
                responseFutures.add(
                        sendAsyncRangeRequest(host, port, path, ranges.get(counter - 1) + 1, ranges.get(counter))
                );
            }
        }

        List<String> response = new LinkedList<>();

        for (Future<List<String>> responseFuture : responseFutures) {
            response.addAll(extractBody(responseFuture.get()));
        }

        StringBuilder output = new StringBuilder();

        for (String line : response) {
            output.append(line);
        }
        return output.toString();
    }

    private long retrieveFileSize(String filePath) throws IOException {
        List<String> response = MonkHttp.head(host, port, filePath);
        return extractContentLength(response);
    }

    public Future<List<String>> sendAsyncRangeRequest(
            String host,
            int    port,
            String path,
            long   lowerBound,
            long   upperBound
    )
    {
        return executorService.submit(() ->
                MonkHttp.range(host, port, path, lowerBound, upperBound)
        );
    }

    private void saveFile(String name, String content) throws IOException {
        File file = new File(name + ".txt");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private static boolean checkIfResponseOk(List<String> response) {
        if (response.isEmpty()) {
            return false;
        }

        String firstLine = response.get(0);

        if (firstLine.isEmpty()) {
            return false;
        }

        return firstLine.contains("200 OK") || firstLine.contains("206");
    }

    private static List<String> extractBody(List<String> response) {
        if (!checkIfResponseOk(response)) {
            LOGGER.warning("Failed to fetch the resource. \nHTTP response not OK");
            System.out.println(response);
            return Collections.emptyList();
        }
        List<String> body = new ArrayList<>();
        boolean endOfHead = false;

        for (String line : response) {
            if (endOfHead) {
                body.add(line.stripTrailing());
            }
            if(line.isBlank() || line.isEmpty()) {
                endOfHead = true;
            }
        }
        return body;
    }

    private static long extractContentLength(List<String> response) {
        if (!checkIfResponseOk(response)) {
            LOGGER.warning("Failed to fetch the resource. \nHTTP response not OK");
            System.exit(-1);
        }

        for (String line : response) {
            if (line.contains("Content-Length")) {
                return Long.parseLong(
                        line
                                .split(":")[1].substring(1)
                                .stripTrailing()
                );
            }
        }
        LOGGER.warning("Error extracting the content length");
        System.exit(-1);
        return -1L;
    }
}
