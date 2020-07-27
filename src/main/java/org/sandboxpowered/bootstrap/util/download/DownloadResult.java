package org.sandboxpowered.bootstrap.util.download;

import org.jetbrains.annotations.Nullable;

public class DownloadResult {
    @Nullable
    private final String message;
    @Nullable
    private final Throwable error;
    private final Type result;

    private DownloadResult(@Nullable String message, @Nullable Throwable error, Type result) {
        this.message = message;
        this.error = error;
        this.result = result;
    }

    public static DownloadResult fail(String message, Throwable cause) {
        return of(message, cause, Type.FAIL);
    }

    public static DownloadResult of(@Nullable String message, @Nullable Throwable error, Type result) {
        return new DownloadResult(message, error, result);
    }

    public static DownloadResult success() {
        return new DownloadResult(null, null, Type.SUCCESS);
    }

    public static DownloadResult fail() {
        return of(null, null, Type.FAIL);
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }

    public Type getResult() {
        return result;
    }

    public enum Type {
        SUCCESS,
        FAIL,
        ABORTED;
    }
}
