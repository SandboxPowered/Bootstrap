package org.sandboxpowered.bootstrap.util.download;

public interface ProgressCallback {
    /**
     * @param percentage the percentage of the file downloaded
     * @param stage the stage of download
     */
    void accept(int percentage, Stage stage);

    enum Stage {
        PREPARING,
        DOWNLOADING,
        COPYING,
        FINISHED
    }
}
