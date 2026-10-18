package com.codenova.library.repository;

import com.codenova.library.exception.StorageException;
import com.codenova.library.model.Loan;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** loans.csv: id,itemId,memberId,borrowDate,dueDate,returnDate,fine (returnDate empty while out). */
public class LoanCsvRepository implements Repository<Loan> {
    private static final String HEADER = "id,itemId,memberId,borrowDate,dueDate,returnDate,fine";
    private final Path file;

    public LoanCsvRepository(Path file) { this.file = file; }

    @Override
    public List<Loan> loadAll() {
        List<Loan> list = new ArrayList<>();
        for (List<String> r : CsvFile.read(file, 7)) {
            try {
                LocalDate returned = r.get(5).isEmpty() ? null : LocalDate.parse(r.get(5));
                list.add(new Loan(Integer.parseInt(r.get(0)), Integer.parseInt(r.get(1)), Integer.parseInt(r.get(2)),
                        LocalDate.parse(r.get(3)), LocalDate.parse(r.get(4)), returned, Integer.parseInt(r.get(6))));
            } catch (NumberFormatException | DateTimeParseException e) {
                throw new StorageException("Corrupt value in " + file.getFileName() + ": " + r, e);
            }
        }
        return list;
    }

    @Override
    public void saveAll(List<Loan> loans) {
        List<List<String>> rows = new ArrayList<>();
        for (Loan l : loans) {
            rows.add(List.of(String.valueOf(l.getId()), String.valueOf(l.getItemId()), String.valueOf(l.getMemberId()),
                    l.getBorrowDate().toString(), l.getDueDate().toString(),
                    l.getReturnDate() == null ? "" : l.getReturnDate().toString(), String.valueOf(l.getFine())));
        }
        CsvFile.write(file, HEADER, rows);
    }
}
