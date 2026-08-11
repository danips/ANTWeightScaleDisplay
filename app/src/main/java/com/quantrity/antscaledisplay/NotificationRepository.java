package com.quantrity.antscaledisplay;

import java.util.Objects;

/** Routes notification codes only to the currently active MFA request. */
public final class NotificationRepository {
    private static final NotificationRepository INSTANCE = new NotificationRepository();

    private final Object lock = new Object();
    private MfaRequest activeRequest;

    NotificationRepository() {}

    public static NotificationRepository getInstance() {
        return INSTANCE;
    }

    MfaRequest registerMfaRequest(long startedAtMillis, CodeConsumer codeConsumer) {
        MfaRequest request = new MfaRequest(
                this, startedAtMillis, Objects.requireNonNull(codeConsumer));
        synchronized (lock) {
            if (activeRequest != null) activeRequest.closed = true;
            activeRequest = request;
        }
        return request;
    }

    boolean postMfaCode(String code, long postedAtMillis) {
        CodeConsumer consumer;
        synchronized (lock) {
            MfaRequest request = activeRequest;
            if (request == null
                    || request.closed
                    || request.consumed
                    || postedAtMillis < request.startedAtMillis
                    || !isSixDigitCode(code)) {
                return false;
            }
            request.consumed = true;
            activeRequest = null;
            consumer = request.codeConsumer;
        }
        consumer.accept(code);
        return true;
    }

    private void close(MfaRequest request) {
        synchronized (lock) {
            request.closed = true;
            if (activeRequest == request) activeRequest = null;
        }
    }

    private static boolean isSixDigitCode(String code) {
        if (code == null || code.length() != 6) return false;
        for (int i = 0; i < code.length(); i++) {
            char digit = code.charAt(i);
            if (digit < '0' || digit > '9') return false;
        }
        return true;
    }

    interface CodeConsumer {
        void accept(String code);
    }

    static final class MfaRequest implements AutoCloseable {
        private final NotificationRepository owner;
        private final long startedAtMillis;
        private final CodeConsumer codeConsumer;
        private boolean consumed;
        private boolean closed;

        private MfaRequest(NotificationRepository owner, long startedAtMillis,
                           CodeConsumer codeConsumer) {
            this.owner = owner;
            this.startedAtMillis = startedAtMillis;
            this.codeConsumer = codeConsumer;
        }

        @Override
        public void close() {
            owner.close(this);
        }
    }
}
