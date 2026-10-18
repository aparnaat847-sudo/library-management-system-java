package com.codenova.library.exception;

/** Invalid user input (empty title, bad email/phone/ISBN, duplicates...). */
public class ValidationException extends LibraryException {
    public ValidationException(String message) {
        super(message);
    }
}
