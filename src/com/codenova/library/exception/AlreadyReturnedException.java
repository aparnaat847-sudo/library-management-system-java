package com.codenova.library.exception;

public class AlreadyReturnedException extends LibraryException {
    public AlreadyReturnedException(int loanId) {
        super("Loan " + loanId + " has already been returned.");
    }
}
