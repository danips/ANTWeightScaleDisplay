package com.quantrity.antscaledisplay;

final class RepositoryResult<T> {
    final T value;
    final String message;
    final Exception error;

    private RepositoryResult(T value, String message, Exception error) {
        this.value = value;
        this.message = message;
        this.error = error;
    }

    static <T> RepositoryResult<T> success(T value) {
        return new RepositoryResult<>(value, null, null);
    }

    static <T> RepositoryResult<T> failure(String message, Exception error) {
        return new RepositoryResult<>(null, message, error);
    }

    boolean isSuccess() {
        return error == null;
    }
}
