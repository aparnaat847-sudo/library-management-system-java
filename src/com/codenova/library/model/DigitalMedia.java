package com.codenova.library.model;

/** An e-book, audiobook or video licence. 7-day loan, Rs.1 per overdue day. */
public class DigitalMedia extends LibraryItem {
    public static final int LOAN_DAYS = 7;
    public static final int FINE_PER_DAY = 1;

    private final String format;
    private final int sizeMb;

    public DigitalMedia(int id, String title, String creator, int totalCopies, String format, int sizeMb) {
        super(id, title, creator, totalCopies);
        this.format = format;
        this.sizeMb = sizeMb;
    }

    public String getFormat() { return format; }
    public int getSizeMb() { return sizeMb; }

    @Override public String getKind() { return "DIGITAL"; }
    @Override public String getTypeLabel() { return "Digital Media"; }
    @Override public int getLoanDays() { return LOAN_DAYS; }
    @Override public int getFinePerDay() { return FINE_PER_DAY; }
    @Override public String getDetails() { return format + " | " + sizeMb + " MB"; }
    @Override public String[] getExtraFields() { return new String[]{format, String.valueOf(sizeMb)}; }
}
