package org.sandboxpowered.bootstrap.util.download;

import org.apache.commons.io.IOUtils;
import org.sandboxpowered.bootstrap.util.SandboxFolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//TODO hash verification
public class DownloadManager {

    private static final int MAX_CONNECTIONS = 4;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(MAX_CONNECTIONS); //r -> new Thread(r, "SandboxBootstrap|Download")

    public static CompletableFuture<DownloadResult> download(String source, Path target, String tempFileName, ProgressCallback progressCallback) throws MalformedURLException {
        return download(new URL(source), target, tempFileName, progressCallback);
    }

    public static CompletableFuture<DownloadResult> download(URL source, Path target, String tempFileName, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            if (Files.isDirectory(target)) {
                return DownloadResult.of("Expected file but target was a directory!", null, DownloadResult.Type.ABORTED);
            }
            try {
                Path temp = Files.createTempFile(SandboxFolder.getDownloadPath(), tempFileName, null);

                //Open connection and get download size
                URLConnection httpConnection = source.openConnection();
                long completeFileSize = httpConnection.getContentLength();
                progressCallback.accept(0L, completeFileSize, ProgressCallback.Stage.PREPARING);
                try (BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream()); BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(temp))) {
                    byte[] data = new byte[1024];
                    long downloadedFileSize = 0;

                    int x;
                    while ((x = inputStream.read(data, 0, 1024)) >= 0) {
                        //Add data and update progress bar
                        downloadedFileSize += x;
                        progressCallback.accept(downloadedFileSize, completeFileSize, ProgressCallback.Stage.DOWNLOADING);
                        outputStream.write(data, 0, x);
                    }
                    progressCallback.accept(completeFileSize, completeFileSize, ProgressCallback.Stage.COPYING);
                }
                Files.createDirectories(target.getParent());
                try (InputStream inputStream = Files.newInputStream(temp, StandardOpenOption.DELETE_ON_CLOSE)) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
                progressCallback.accept(completeFileSize, completeFileSize, ProgressCallback.Stage.FINISHED);
                return DownloadResult.success();
            } catch (IOException e) {
                return DownloadResult.fail("unable to download " + source.toString(), e);
            }
        }, EXECUTOR_SERVICE);
    }

    public static String readStringFromURL(String requestURL) throws IOException {
        return readStringFromURL(new URL(requestURL));
    }

    public static String readStringFromURL(URL requestURL) throws IOException {
        return IOUtils.toString(requestURL, StandardCharsets.UTF_8);
    }

}