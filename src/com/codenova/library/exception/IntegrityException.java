package com.codenova.library.exception;

/** An action would break relational integrity (e.g. deleting an item that is on loan). */
public class IntegrityException extends LibraryException {
    public IntegrityException(String message) {
        super(message);
    }
}
