package com.example.backend.exception;

public class InvalidPhysicsInputException extends RuntimeException {

    public InvalidPhysicsInputException(String message) {
        super(message);
    }

    public InvalidPhysicsInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
