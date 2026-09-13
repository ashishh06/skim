package com.skim.util;

public class ServerBusyException extends RuntimeException {
    public ServerBusyException(String message) {
        super(message);
    }
}
