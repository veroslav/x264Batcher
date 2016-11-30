package org.matic.x264batcher.model;

public final class SegmentEncoderResult {

    public static final int FAILED = -1;
    public static final int SUCCESS = 0;

    private final Exception exception;
    private final int exitCode;

    public SegmentEncoderResult(final int exitCode, final Exception exception) {
        this.exitCode = exitCode;
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public int getExitCode() {
        return exitCode;
    }
}