package com.quantrity.antscaledisplay;

/** A lifecycle-replayable result that is handled by at most one active UI owner. */
final class OperationEvent<T> {
    private final T value;
    private boolean consumed;

    OperationEvent(T value) {
        this.value = value;
    }

    synchronized T consume() {
        if (consumed) return null;
        consumed = true;
        return value;
    }

    T peek() {
        return value;
    }
}
