package com.quantrity.antscaledisplay;

/** Limits progress rendering to changes in the whole-number percentage. */
final class ProgressUpdateThrottler {
    private int lastTotal = -1;
    private int lastPercent = -1;

    synchronized void reset() {
        lastTotal = -1;
        lastPercent = -1;
    }

    synchronized boolean shouldReport(int completed, int total) {
        int safeTotal = Math.max(total, 1);
        int safeCompleted = Math.max(0, Math.min(completed, safeTotal));
        if (safeTotal != lastTotal) {
            lastTotal = safeTotal;
            lastPercent = -1;
        }

        int percent = (int) ((long) safeCompleted * 100 / safeTotal);
        if (percent <= lastPercent) return false;
        lastPercent = percent;
        return true;
    }
}
