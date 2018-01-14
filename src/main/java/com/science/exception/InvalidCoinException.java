package com.science.exception;

public class InvalidCoinException extends Exception {

    public InvalidCoinException() { super(); }
    public InvalidCoinException(String message) { super(message); }
    public InvalidCoinException(String message, Throwable cause) { super(message, cause); }
    public InvalidCoinException(Throwable cause) { super(cause); }

}
