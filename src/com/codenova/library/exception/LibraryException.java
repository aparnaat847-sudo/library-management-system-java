package com.codenova.library.exception;

/** Base class of all business-rule exceptions in the library system. */
public class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}
