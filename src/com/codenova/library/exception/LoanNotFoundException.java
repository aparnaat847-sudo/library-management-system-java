package com.codenova.library.exception;

public class LoanNotFoundException extends LibraryException {
    public LoanNotFoundException(int id) {
        super("No loan found with ID " + id + ".");
    }
}
