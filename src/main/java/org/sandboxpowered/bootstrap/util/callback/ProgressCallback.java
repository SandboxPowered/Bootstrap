package org.sandboxpowered.bootstrap.util.callback;

public interface ProgressCallback {
    /**
     * @param bytesDownloaded the amount of bytes that have been downloaded already
     * @param bytesTotal      the total amout of bytes to download
     * @param stage           the stage of download
     */
    void accept(long bytesDownloaded, long bytesTotal, Stage stage);

    enum Stage {
        PREPARING,
        DOWNLOADING,
        COPYING,
        FINISHED
    }
}
