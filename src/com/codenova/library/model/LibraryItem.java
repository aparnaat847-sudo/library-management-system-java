package com.codenova.library.model;

/**
 * Abstract parent of everything that can be borrowed.
 * Subclasses (PhysicalBook, DigitalMedia) decide their own loan period and fine rate,
 * so the service can treat every item polymorphically.
 */
public abstract class LibraryItem {
    private final int id;
    private final String title;
    private final String creator;
    private final int totalCopies;

    protected LibraryItem(int id, String title, String creator, int totalCopies) {
        this.id = id;
        this.title = title;
        this.creator = creator;
        this.totalCopies = totalCopies;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCreator() { return creator; }
    public int getTotalCopies() { return totalCopies; }

    /** Storage code: BOOK or DIGITAL. */
    public abstract String getKind();
    /** Human readable type for the UI. */
    public abstract String getTypeLabel();
    /** How many days this kind of item may be kept. */
    public abstract int getLoanDays();
    /** Fine in rupees per overdue day. */
    public abstract int getFinePerDay();
    /** Short description of the type-specific fields. */
    public abstract String getDetails();
    /** The two type-specific values written to the data file. */
    public abstract String[] getExtraFields();

    @Override
    public String toString() {
        return title + " (" + getTypeLabel() + ")";
    }
}
