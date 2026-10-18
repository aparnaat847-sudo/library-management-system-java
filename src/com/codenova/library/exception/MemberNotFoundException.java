package com.codenova.library.exception;

public class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(int id) {
        super("No member found with ID " + id + ".");
    }
}
