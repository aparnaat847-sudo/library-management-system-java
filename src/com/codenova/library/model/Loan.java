package com.codenova.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One borrowing record: which member took which item, when it is due, and when it came back. */
public class Loan {
    private final int id;
    private final int itemId;
    private final int memberId;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate; // null while the item is still out
    private int fine;             // rupees, fixed when the item is returned

    public Loan(int id, int itemId, int memberId, LocalDate borrowDate, LocalDate dueDate,
                LocalDate returnDate, int fine) {
        this.id = id;
        this.itemId = itemId;
        this.memberId = memberId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
    }

    public int getId() { return id; }
    public int getItemId() { return itemId; }
    public int getMemberId() { return memberId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getFine() { return fine; }

    public boolean isReturned() { return returnDate != null; }

    /** Still out and past its due date. */
    public boolean isOverdue(LocalDate today) {
        return !isReturned() && today.isAfter(dueDate);
    }

    /** Days late: measured to the return date if returned, otherwise to 'today'. */
    public int overdueDays(LocalDate today) {
        LocalDate end = isReturned() ? returnDate : today;
        return (int) Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
    }

    public void markReturned(LocalDate date, int fineAmount) {
        this.returnDate = date;
        this.fine = fineAmount;
    }

    /** Undo markReturned (used to roll back if saving to disk fails). */
    public void reopen() {
        this.returnDate = null;
        this.fine = 0;
    }
}
