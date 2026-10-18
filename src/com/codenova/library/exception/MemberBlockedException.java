package com.codenova.library.exception;

/** The member has an overdue item and may not borrow until it is returned. */
public class MemberBlockedException extends LibraryException {
    public MemberBlockedException(String member) {
        super(member + " has an overdue item. Return it before borrowing anything new.");
    }
}
