package org.sandboxpowered.bootstrap.util.download;

import org.sandboxpowered.bootstrap.util.callback.CloseCallback;
import org.sandboxpowered.bootstrap.util.callback.PostDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.PreDownloadCallback;
import org.sandboxpowered.bootstrap.util.callback.ProgressCallback;

public interface DownloadHandler extends ProgressCallback, PreDownloadCallback, PostDownloadCallback, CloseCallback {
}
