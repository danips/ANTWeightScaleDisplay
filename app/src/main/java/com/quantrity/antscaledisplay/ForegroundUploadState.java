package com.quantrity.antscaledisplay;

/** Immutable progress rendered by the current Activity during a retained upload. */
final class ForegroundUploadState {
    final boolean running;
    final int completed;
    final int total;

    private ForegroundUploadState(boolean running, int completed, int total) {
        this.running = running;
        this.completed = completed;
        this.total = total;
    }

    static ForegroundUploadState idle() {
        return new ForegroundUploadState(false, 0, 0);
    }

    static ForegroundUploadState running(int total) {
        return new ForegroundUploadState(true, 0, Math.max(0, total));
    }

    ForegroundUploadState operationCompleted() {
        return running
                ? new ForegroundUploadState(true, Math.min(total, completed + 1), total)
                : this;
    }
}
