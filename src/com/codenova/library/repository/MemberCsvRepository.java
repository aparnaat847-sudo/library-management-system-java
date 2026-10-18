package com.codenova.library.repository;

import com.codenova.library.exception.StorageException;
import com.codenova.library.model.Member;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** members.csv: id,cardNumber,name,email,phone,registeredOn */
public class MemberCsvRepository implements Repository<Member> {
    private static final String HEADER = "id,cardNumber,name,email,phone,registeredOn";
    private final Path file;

    public MemberCsvRepository(Path file) { this.file = file; }

    @Override
    public List<Member> loadAll() {
        List<Member> list = new ArrayList<>();
        for (List<String> r : CsvFile.read(file, 6)) {
            try {
                list.add(new Member(Integer.parseInt(r.get(0)), r.get(1), r.get(2), r.get(3), r.get(4),
                        LocalDate.parse(r.get(5))));
            } catch (NumberFormatException | DateTimeParseException e) {
                throw new StorageException("Corrupt value in " + file.getFileName() + ": " + r, e);
            }
        }
        return list;
    }

    @Override
    public void saveAll(List<Member> members) {
        List<List<String>> rows = new ArrayList<>();
        for (Member m : members) {
            rows.add(List.of(String.valueOf(m.getId()), m.getCardNumber(), m.getName(), m.getEmail(),
                    m.getPhone(), m.getRegisteredOn().toString()));
        }
        CsvFile.write(file, HEADER, rows);
    }
}
