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

    /**
     * the maximum amount of downloads that should be running in parallel
     */
    private static final int MAX_CONNECTIONS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    /**
     * the size of each connection's download buffer, in {@code bytes}
     */
    private static final int DOWNLOAD_BUFFER_SIZE = 1024;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(MAX_CONNECTIONS, r -> {
        Thread t = new Thread(r, "Sandbox Bootstrap/Download");
        t.setDaemon(true);
        return t;
    });

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
                long bytesTotal = httpConnection.getContentLength();
                progressCallback.accept(0, bytesTotal, ProgressCallback.Stage.PREPARING);
                try (BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream(), DOWNLOAD_BUFFER_SIZE); BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(temp), DOWNLOAD_BUFFER_SIZE)) {
                    byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
                    long bytesDownloaded = 0;

                    //TODO maybe change buffer size to make the progress bar smoother? or make it configurable?
                    for (int i = inputStream.read(data, 0, DOWNLOAD_BUFFER_SIZE); i >= 0; i = inputStream.read(data, 0, DOWNLOAD_BUFFER_SIZE)) {
                        //Add data and update progress bar
                        bytesDownloaded += i;
                        progressCallback.accept(bytesDownloaded, bytesTotal, ProgressCallback.Stage.DOWNLOADING);
                        outputStream.write(data, 0, i);
                    }
                    progressCallback.accept(bytesTotal, bytesTotal, ProgressCallback.Stage.COPYING);
                }
                Files.createDirectories(target.getParent());
                try (InputStream inputStream = Files.newInputStream(temp, StandardOpenOption.DELETE_ON_CLOSE)) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
                progressCallback.accept(bytesTotal, bytesTotal, ProgressCallback.Stage.FINISHED);
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
