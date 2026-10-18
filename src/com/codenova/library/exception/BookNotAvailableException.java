package com.codenova.library.exception;

/** All copies of the requested item are currently borrowed. */
public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String title) {
        super("'" + title + "' is not available right now. All copies are borrowed.");
    }
}
