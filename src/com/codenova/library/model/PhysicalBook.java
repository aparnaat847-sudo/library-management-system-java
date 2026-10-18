package com.codenova.library.model;

/** A printed book kept on a shelf. 14-day loan, Rs.2 per overdue day. */
public class PhysicalBook extends LibraryItem {
    public static final int LOAN_DAYS = 14;
    public static final int FINE_PER_DAY = 2;

    private final String isbn;
    private final String shelf;

    public PhysicalBook(int id, String title, String author, int totalCopies, String isbn, String shelf) {
        super(id, title, author, totalCopies);
        this.isbn = isbn;
        this.shelf = shelf;
    }

    public String getIsbn() { return isbn; }
    public String getShelf() { return shelf; }

    @Override public String getKind() { return "BOOK"; }
    @Override public String getTypeLabel() { return "Physical Book"; }
    @Override public int getLoanDays() { return LOAN_DAYS; }
    @Override public int getFinePerDay() { return FINE_PER_DAY; }
    @Override public String getDetails() { return "ISBN " + isbn + " | " + shelf; }
    @Override public String[] getExtraFields() { return new String[]{isbn, shelf}; }
}
