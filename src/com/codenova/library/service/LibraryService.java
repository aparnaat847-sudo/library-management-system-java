package com.codenova.library.service;

import com.codenova.library.exception.*;
import com.codenova.library.model.*;
import com.codenova.library.repository.Repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Business logic: catalog, members, borrowing, returning, overdue flags and fines.
 * Inventory is never stored as a number that could drift: available copies are always
 * calculated as (total copies - loans that are still out), so they always match reality.
 */
public class LibraryService {
    public static final int MAX_LOANS = 3;

    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Set<String> FORMATS = Set.of("EBOOK", "AUDIOBOOK", "VIDEO");

    private final Repository<LibraryItem> itemRepo;
    private final Repository<Member> memberRepo;
    private final Repository<Loan> loanRepo;
    private final Clock clock;

    private final List<LibraryItem> items;
    private final List<Member> members;
    private final List<Loan> loans;

    public LibraryService(Repository<LibraryItem> itemRepo, Repository<Member> memberRepo,
                          Repository<Loan> loanRepo, Clock clock) {
        this.itemRepo = itemRepo;
        this.memberRepo = memberRepo;
        this.loanRepo = loanRepo;
        this.clock = clock;
        this.items = new ArrayList<>(itemRepo.loadAll());
        this.members = new ArrayList<>(memberRepo.loadAll());
        this.loans = new ArrayList<>(loanRepo.loadAll());
    }

    public LocalDate today() { return LocalDate.now(clock); }

    // ===================== catalog =====================

    public PhysicalBook addPhysicalBook(String title, String author, int copies, String isbn, String shelf)
            throws ValidationException {
        title = clean(title); author = clean(author); shelf = clean(shelf);
        String digits = clean(isbn).replace("-", "").replace(" ", "");
        requireText(title, "Title");
        requireText(author, "Author");
        requireCopies(copies);
        if (!digits.matches("\\d{10}|\\d{13}")) throw new ValidationException("ISBN must be 10 or 13 digits.");
        for (LibraryItem i : items) {
            if (i instanceof PhysicalBook p && p.getIsbn().equals(digits)) {
                throw new ValidationException("A book with this ISBN already exists ('" + p.getTitle() + "').");
            }
        }
        if (shelf.isEmpty()) shelf = "Unassigned";
        PhysicalBook book = new PhysicalBook(nextItemId(), title, author, copies, digits, shelf);
        items.add(book);
        try {
            itemRepo.saveAll(items);
        } catch (StorageException e) {
            items.remove(book);
            throw e;
        }
        return book;
    }

    public DigitalMedia addDigitalMedia(String title, String creator, int copies, String format, int sizeMb)
            throws ValidationException {
        title = clean(title); creator = clean(creator);
        String fmt = clean(format).toUpperCase();
        requireText(title, "Title");
        requireText(creator, "Creator");
        requireCopies(copies);
        if (!FORMATS.contains(fmt)) throw new ValidationException("Format must be EBOOK, AUDIOBOOK or VIDEO.");
        if (sizeMb <= 0) throw new ValidationException("Size (MB) must be a positive number.");
        DigitalMedia media = new DigitalMedia(nextItemId(), title, creator, copies, fmt, sizeMb);
        items.add(media);
        try {
            itemRepo.saveAll(items);
        } catch (StorageException e) {
            items.remove(media);
            throw e;
        }
        return media;
    }

    public void removeItem(int id) throws ItemNotFoundException, IntegrityException {
        LibraryItem item = findItem(id);
        long out = loans.stream().filter(l -> l.getItemId() == id && !l.isReturned()).count();
        if (out > 0) {
            throw new IntegrityException("Cannot remove '" + item.getTitle() + "': " + out
                    + " cop" + (out == 1 ? "y is" : "ies are") + " currently borrowed.");
        }
        items.remove(item);
        try {
            itemRepo.saveAll(items);
        } catch (StorageException e) {
            items.add(item);
            throw e;
        }
    }

    public List<LibraryItem> searchItems(String query) {
        String q = clean(query).toLowerCase();
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem i : items) {
            if (q.isEmpty() || i.getTitle().toLowerCase().contains(q) || i.getCreator().toLowerCase().contains(q)
                    || i.getDetails().toLowerCase().contains(q)) {
                result.add(i);
            }
        }
        result.sort(Comparator.comparing(i -> i.getTitle().toLowerCase()));
        return result;
    }

    public LibraryItem findItem(int id) throws ItemNotFoundException {
        for (LibraryItem i : items) if (i.getId() == id) return i;
        throw new ItemNotFoundException(id);
    }

    /** Total copies minus copies that are out on loan. */
    public int availableCopies(int itemId) {
        LibraryItem item = itemOrNull(itemId);
        if (item == null) return 0;
        long out = loans.stream().filter(l -> l.getItemId() == itemId && !l.isReturned()).count();
        return item.getTotalCopies() - (int) out;
    }

    public LibraryItem itemOrNull(int id) {
        for (LibraryItem i : items) if (i.getId() == id) return i;
        return null;
    }

    // ===================== members =====================

    public Member registerMember(String name, String email, String phone) throws ValidationException {
        name = clean(name); email = clean(email); phone = clean(phone);
        requireText(name, "Name");
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Email format is invalid (example: name@example.com).");
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Phone must be exactly 10 digits (numbers only).");
        }
        for (Member m : members) {
            if (m.getEmail().equalsIgnoreCase(email)) {
                throw new ValidationException("This email is already registered (" + m.getName() + ").");
            }
            if (m.getPhone().equals(phone)) {
                throw new ValidationException("This phone number is already registered (" + m.getName() + ").");
            }
        }
        int id = nextMemberId();
        Member member = new Member(id, String.format("LIB-%04d", id), name, email, phone, today());
        members.add(member);
        try {
            memberRepo.saveAll(members);
        } catch (StorageException e) {
            members.remove(member);
            throw e;
        }
        return member;
    }

    public void removeMember(int id) throws MemberNotFoundException, IntegrityException {
        Member member = findMember(id);
        if (activeLoanCount(id) > 0) {
            throw new IntegrityException("Cannot remove " + member.getName()
                    + ": they still have borrowed items. Return them first.");
        }
        members.remove(member);
        try {
            memberRepo.saveAll(members);
        } catch (StorageException e) {
            members.add(member);
            throw e;
        }
    }

    public List<Member> getMembers() {
        List<Member> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingInt(Member::getId));
        return sorted;
    }

    public Member findMember(int id) throws MemberNotFoundException {
        for (Member m : members) if (m.getId() == id) return m;
        throw new MemberNotFoundException(id);
    }

    public Member memberOrNull(int id) {
        for (Member m : members) if (m.getId() == id) return m;
        return null;
    }

    public int activeLoanCount(int memberId) {
        return (int) loans.stream().filter(l -> l.getMemberId() == memberId && !l.isReturned()).count();
    }

    // ===================== circulation =====================

    public Loan borrow(int memberId, int itemId) throws LibraryException {
        Member member = findMember(memberId);
        LibraryItem item = findItem(itemId);
        LocalDate today = today();

        for (Loan l : loans) {
            if (l.getMemberId() != memberId || l.isReturned()) continue;
            if (l.isOverdue(today)) throw new MemberBlockedException(member.getName());
            if (l.getItemId() == itemId) {
                throw new ValidationException(member.getName() + " already has '" + item.getTitle() + "' borrowed.");
            }
        }
        if (activeLoanCount(memberId) >= MAX_LOANS) throw new BorrowLimitExceededException(member.getName(), MAX_LOANS);
        if (availableCopies(itemId) <= 0) throw new BookNotAvailableException(item.getTitle());

        Loan loan = new Loan(nextLoanId(), itemId, memberId, today, today.plusDays(item.getLoanDays()), null, 0);
        loans.add(loan);
        try {
            loanRepo.saveAll(loans);
        } catch (StorageException e) {
            loans.remove(loan);
            throw e;
        }
        return loan;
    }

    public Loan returnItem(int loanId) throws LibraryException {
        Loan loan = findLoan(loanId);
        if (loan.isReturned()) throw new AlreadyReturnedException(loanId);
        LibraryItem item = itemOrNull(loan.getItemId());
        int perDay = item == null ? PhysicalBook.FINE_PER_DAY : item.getFinePerDay();
        LocalDate today = today();
        loan.markReturned(today, loan.overdueDays(today) * perDay);
        try {
            loanRepo.saveAll(loans);
        } catch (StorageException e) {
            loan.reopen();
            throw e;
        }
        return loan;
    }

    /** Fine owed so far: fixed amount if returned, otherwise accrued up to today. */
    public int currentFine(Loan loan) {
        if (loan.isReturned()) return loan.getFine();
        LibraryItem item = itemOrNull(loan.getItemId());
        int perDay = item == null ? PhysicalBook.FINE_PER_DAY : item.getFinePerDay();
        return loan.overdueDays(today()) * perDay;
    }

    public Loan findLoan(int id) throws LoanNotFoundException {
        for (Loan l : loans) if (l.getId() == id) return l;
        throw new LoanNotFoundException(id);
    }

    public List<Loan> getActiveLoans() {
        List<Loan> list = new ArrayList<>();
        for (Loan l : loans) if (!l.isReturned()) list.add(l);
        list.sort(Comparator.comparing(Loan::getDueDate));
        return list;
    }

    public List<Loan> getOverdueLoans() {
        LocalDate today = today();
        List<Loan> list = new ArrayList<>();
        for (Loan l : loans) if (l.isOverdue(today)) list.add(l);
        list.sort(Comparator.comparing(Loan::getDueDate));
        return list;
    }

    public List<Loan> getAllLoans() {
        List<Loan> list = new ArrayList<>(loans);
        list.sort(Comparator.comparingInt(Loan::getId).reversed());
        return list;
    }

    // ===================== helpers =====================

    private int nextItemId() { return items.stream().mapToInt(LibraryItem::getId).max().orElse(0) + 1; }
    private int nextMemberId() { return members.stream().mapToInt(Member::getId).max().orElse(0) + 1; }
    private int nextLoanId() { return loans.stream().mapToInt(Loan::getId).max().orElse(0) + 1; }

    private static String clean(String s) { return s == null ? "" : s.trim(); }

    private static void requireText(String value, String field) throws ValidationException {
        if (value.isEmpty()) throw new ValidationException(field + " cannot be empty.");
    }

    private static void requireCopies(int copies) throws ValidationException {
        if (copies < 1 || copies > 100) throw new ValidationException("Copies must be between 1 and 100.");
    }
}
