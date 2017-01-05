package com.amarsoft.p2ptrade.webservice.exception;

public class InputValidException extends Exception {
    public InputValidException() {
        super();
    }
    /**
     * 根据给定信息构造例外
     *
     * @param message 详细的Exception信息
     */
    public InputValidException(String message) {
        super(message);
    }

    /**
     * 根据给定的原始例外和消息构成一个新的异常，详细信息是给定的信息和源例外的组合
     *
     * @param message 详细信息
     * @param cause 原始例外
     */
    public InputValidException(String message, Throwable cause) {
        super(message + " (Caused by " + cause + ")");
        this.cause = cause; // Two-argument version requires JDK 1.4 or later
    }

    /**
     * 原始例外.
     */
    protected Throwable cause = null;
    /**
     * Return the underlying cause of this exception (if any).
     */
    public Throwable getCause() {
        return (this.cause);
    }
}
