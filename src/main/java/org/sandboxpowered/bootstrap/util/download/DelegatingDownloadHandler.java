package org.sandboxpowered.bootstrap.util.download;

import org.sandboxpowered.bootstrap.util.callback.CloseCallback;
import org.sandboxpowered.bootstrap.util.callback.PostDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.PreDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.ProgressCallback;

public class DelegatingDownloadHandler implements DownloadHandler {

    private final ProgressCallback progressCallback;
    private final PreDownloadCallback preDownloadCallback;
    private final PostDownloadCallback postDownloadCallback;
    private final CloseCallback closeCallback;

    private DelegatingDownloadHandler(ProgressCallback progressCallback, PreDownloadCallback preDownloadCallback, PostDownloadCallback postDownloadCallback, CloseCallback closeCallback) {
        this.progressCallback = progressCallback;
        this.preDownloadCallback = preDownloadCallback;
        this.postDownloadCallback = postDownloadCallback;
        this.closeCallback = closeCallback;
    }

    public static DelegatingDownloadHandler of(ProgressCallback progressCallback, PreDownloadCallback preDownloadCallback, PostDownloadCallback postDownloadCallback, CloseCallback closeCallback) {
        return new DelegatingDownloadHandler(progressCallback, preDownloadCallback, postDownloadCallback, closeCallback);
    }

    @Override
    public void accept(long bytesDownloaded, long bytesTotal, Stage stage) {
        this.progressCallback.accept(bytesDownloaded, bytesTotal, stage);
    }

    @Override
    public void onClose() {
        this.closeCallback.onClose();
    }

    @Override
    public void onFinishedDownloading() {
        this.postDownloadCallback.onFinishedDownloading();
    }

    @Override
    public void onStartDownloading() {
        this.preDownloadCallback.onStartDownloading();
    }
}
