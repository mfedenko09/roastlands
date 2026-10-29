package io.github.mfedenko09.roastlands.game;

final class ErrorReason {

    private ErrorReason() {
    }

    static String of(Throwable error) {
        String reason = error.getClass().getSimpleName();
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                reason = cause.getMessage();
            }
        }
        return reason;
    }
}
