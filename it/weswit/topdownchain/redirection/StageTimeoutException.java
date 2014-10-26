package it.weswit.topdownchain.redirection;

class StageTimeoutException extends Exception {
    private long timeout;

    StageTimeoutException(String context, long timeout) {
        super(context);
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }
}