package com.messenger.api.messaging.service;

public class MessagingAccessDeniedException extends RuntimeException {
    public MessagingAccessDeniedException(String message) {
        super(message);
    }
}
