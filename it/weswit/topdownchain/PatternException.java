package it.weswit.topdownchain;

public class PatternException extends RuntimeException {

    public PatternException(String msg) {
        super(msg);
    }

    public PatternException(String msg, Throwable cause) {
        super(msg, cause);
    }

}