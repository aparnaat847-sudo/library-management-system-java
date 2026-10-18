package com.codenova.library.exception;

public class BorrowLimitExceededException extends LibraryException {
    public BorrowLimitExceededException(String member, int limit) {
        super(member + " already has " + limit + " items borrowed (the maximum). Return one first.");
    }
}
