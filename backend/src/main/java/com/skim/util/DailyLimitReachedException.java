package com.skim.util;

public class DailyLimitReachedException extends RuntimeException {
    public DailyLimitReachedException(String message) {
        super(message);
    }
}
