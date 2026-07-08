package com.messenger.api.messaging.service;

public class MessagingNotFoundException extends RuntimeException {
    public MessagingNotFoundException(String message) {
        super(message);
    }
}
