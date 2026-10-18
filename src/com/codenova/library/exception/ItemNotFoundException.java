package com.codenova.library.exception;

public class ItemNotFoundException extends LibraryException {
    public ItemNotFoundException(int id) {
        super("No catalog item found with ID " + id + ".");
    }
}
