package com.codenova.library;

import com.codenova.library.exception.*;
import com.codenova.library.model.*;
import com.codenova.library.repository.*;
import com.codenova.library.service.LibraryService;

import java.nio.file.*;
import java.time.*;

/** Dependency-free self-check with a controllable clock. Run: java -cp out com.codenova.library.SelfTest */
public class SelfTest {
    static int passed = 0, failed = 0;

    /** A clock we can move forward to simulate days passing. */
    static class TestClock extends Clock {
        Instant now = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        void plusDays(int d) { now = now.plus(Duration.ofDays(d)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId z) { return this; }
        @Override public Instant instant() { return now; }
    }

    interface Action { void run() throws Exception; }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }

    static void expect(String name, Class<? extends Throwable> type, Action a) {
        try { a.run(); check(name, false); }
        catch (Throwable t) { check(name, type.isInstance(t)); if (!type.isInstance(t)) t.printStackTrace(System.out); }
    }

    static LibraryService open(Path dir, Clock clock) {
        return new LibraryService(new ItemCsvRepository(dir.resolve("items.csv")),
                new MemberCsvRepository(dir.resolve("members.csv")),
                new LoanCsvRepository(dir.resolve("loans.csv")), clock);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("lib");
        TestClock clock = new TestClock();
        LibraryService s = open(dir, clock);

        // ---- catalog + inheritance/polymorphism ----
        PhysicalBook b1 = s.addPhysicalBook("Clean Code", "Robert Martin", 2, "978-0132350884", "A1");
        DigitalMedia d1 = s.addDigitalMedia("Java Course", "Tim B", 1, "video", 800);
        check("physical book stored with cleaned ISBN", b1.getIsbn().equals("9780132350884"));
        check("digital format upper-cased", d1.getFormat().equals("VIDEO"));
        check("polymorphism: book loan 14 days, digital 7 days", b1.getLoanDays() == 14 && d1.getLoanDays() == 7);
        check("polymorphism: book fine Rs.2/day, digital Rs.1/day", b1.getFinePerDay() == 2 && d1.getFinePerDay() == 1);
        check("new items fully available", s.availableCopies(b1.getId()) == 2 && s.availableCopies(d1.getId()) == 1);
        expect("bad ISBN rejected", ValidationException.class, () -> s.addPhysicalBook("X", "Y", 1, "123", "A"));
        expect("duplicate ISBN rejected", ValidationException.class, () -> s.addPhysicalBook("Z", "Y", 1, "9780132350884", "A"));
        expect("bad digital format rejected", ValidationException.class, () -> s.addDigitalMedia("X", "Y", 1, "PDF", 5));
        expect("empty title rejected", ValidationException.class, () -> s.addPhysicalBook(" ", "Y", 1, "9780132350884", "A"));
        expect("zero copies rejected", ValidationException.class, () -> s.addDigitalMedia("X", "Y", 0, "EBOOK", 5));

        // ---- members + card ----
        Member m1 = s.registerMember("Asha K", "asha@example.com", "9876501234");
        Member m2 = s.registerMember("Ravi P", "ravi@example.com", "9876501235");
        Member m3 = s.registerMember("Divya M", "divya@example.com", "9876501236");
        check("library card issued automatically", m1.getCardNumber().equals("LIB-0001") && m2.getCardNumber().equals("LIB-0002"));
        expect("bad email rejected", ValidationException.class, () -> s.registerMember("X", "bad.email", "9876501299"));
        expect("bad phone rejected", ValidationException.class, () -> s.registerMember("X", "x@example.com", "12345"));
        expect("duplicate email rejected", ValidationException.class, () -> s.registerMember("X", "ASHA@example.com", "9876501299"));
        expect("duplicate phone rejected", ValidationException.class, () -> s.registerMember("X", "x@example.com", "9876501234"));

        // ---- borrowing / inventory ----
        Loan l1 = s.borrow(m1.getId(), b1.getId());
        check("loan due date = today + 14 for a book", l1.getDueDate().equals(LocalDate.of(2026, 10, 15)));
        check("inventory drops after borrow", s.availableCopies(b1.getId()) == 1);
        expect("same member cannot borrow same item twice", ValidationException.class, () -> s.borrow(m1.getId(), b1.getId()));
        Loan l2 = s.borrow(m2.getId(), b1.getId());
        check("last copy borrowed -> 0 available", s.availableCopies(b1.getId()) == 0);
        expect("no copies left -> BookNotAvailableException", BookNotAvailableException.class, () -> s.borrow(m3.getId(), b1.getId()));
        Loan ld = s.borrow(m3.getId(), d1.getId());
        check("loan due date = today + 7 for digital", ld.getDueDate().equals(LocalDate.of(2026, 10, 8)));

        // ---- return on time ----
        clock.plusDays(5);
        s.returnItem(l2.getId());
        check("on-time return has no fine", l2.isReturned() && l2.getFine() == 0);
        check("inventory restored after return", s.availableCopies(b1.getId()) == 1);
        expect("returning twice rejected", AlreadyReturnedException.class, () -> s.returnItem(l2.getId()));

        // ---- overdue + fines ----
        clock.plusDays(5); // day 10: digital (due day 7) is 3 days late, book (due day 14) still fine
        check("digital loan flagged overdue", s.getOverdueLoans().contains(ld) && !s.getOverdueLoans().contains(l1));
        check("fine so far = 3 days x Rs.1", s.currentFine(ld) == 3);
        expect("overdue member blocked from borrowing", MemberBlockedException.class, () -> s.borrow(m3.getId(), b1.getId()));
        s.returnItem(ld.getId());
        check("late digital return fine = Rs.3", ld.getFine() == 3);

        clock.plusDays(10); // day 20: book due day 14 -> 6 days late
        check("book fine = 6 days x Rs.2", s.currentFine(l1) == 12);
        s.returnItem(l1.getId());
        check("late book return fine = Rs.12", l1.getFine() == 12);

        // ---- borrow limit ----
        LibraryItem[] many = new LibraryItem[4];
        for (int i = 0; i < 4; i++) many[i] = s.addDigitalMedia("Title " + i, "Auth", 1, "EBOOK", 5);
        for (int i = 0; i < 3; i++) s.borrow(m1.getId(), many[i].getId());
        expect("4th item exceeds limit of 3", BorrowLimitExceededException.class, () -> s.borrow(m1.getId(), many[3].getId()));

        // ---- relational integrity ----
        expect("cannot remove item that is on loan", IntegrityException.class, () -> s.removeItem(many[0].getId()));
        expect("cannot remove member with active loans", IntegrityException.class, () -> s.removeMember(m1.getId()));
        s.removeItem(many[3].getId());
        check("unborrowed item can be removed", s.itemOrNull(many[3].getId()) == null);
        expect("borrowing unknown item fails", ItemNotFoundException.class, () -> s.borrow(m2.getId(), 999));
        expect("borrowing for unknown member fails", MemberNotFoundException.class, () -> s.borrow(999, b1.getId()));

        // ---- persistence ----
        LibraryService again = open(dir, clock);
        check("items persist across restart", again.searchItems("").size() == s.searchItems("").size());
        check("members persist across restart", again.getMembers().size() == 3);
        check("loans + fines persist across restart", again.getAllLoans().size() == s.getAllLoans().size()
                && again.findLoan(l1.getId()).getFine() == 12);
        check("available copies identical after restart", again.availableCopies(many[0].getId()) == 0);

        // ---- corrupt file ----
        Files.writeString(dir.resolve("loans.csv"), "id,itemId,memberId,borrowDate,dueDate,returnDate,fine\n1,2,x,bad\n");
        expect("corrupt data file -> StorageException", StorageException.class, () -> open(dir, clock));

        // ---- shipped sample data loads ----
        LibraryService sample = open(Paths.get("data"), clock);
        check("sample data files load", sample.searchItems("").size() == 6 && sample.getMembers().size() == 3);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
