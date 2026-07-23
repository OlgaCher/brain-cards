package com.braincards.ai;

public class AiCoachException extends RuntimeException {

    public AiCoachException(String message) {
        super(message);
    }

    public AiCoachException(String message, Throwable cause) {
        super(message, cause);
    }
}
