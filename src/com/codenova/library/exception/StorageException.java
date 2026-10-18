package com.codenova.library.exception;

/** Wraps file read/write problems so the UI never sees raw IOExceptions. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
