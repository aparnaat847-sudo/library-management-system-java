package com.codenova.library.repository;

import com.codenova.library.exception.StorageException;
import com.codenova.library.model.DigitalMedia;
import com.codenova.library.model.LibraryItem;
import com.codenova.library.model.PhysicalBook;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** items.csv: kind,id,title,creator,totalCopies,extra1,extra2 (BOOK: isbn,shelf | DIGITAL: format,sizeMb). */
public class ItemCsvRepository implements Repository<LibraryItem> {
    private static final String HEADER = "kind,id,title,creator,totalCopies,extra1,extra2";
    private final Path file;

    public ItemCsvRepository(Path file) { this.file = file; }

    @Override
    public List<LibraryItem> loadAll() {
        List<LibraryItem> list = new ArrayList<>();
        for (List<String> r : CsvFile.read(file, 7)) {
            try {
                int id = Integer.parseInt(r.get(1));
                int copies = Integer.parseInt(r.get(4));
                switch (r.get(0)) {
                    case "BOOK" -> list.add(new PhysicalBook(id, r.get(2), r.get(3), copies, r.get(5), r.get(6)));
                    case "DIGITAL" -> list.add(new DigitalMedia(id, r.get(2), r.get(3), copies, r.get(5),
                            Integer.parseInt(r.get(6))));
                    default -> throw new StorageException("Unknown item kind '" + r.get(0) + "' in " + file.getFileName(), null);
                }
            } catch (NumberFormatException e) {
                throw new StorageException("Corrupt number in " + file.getFileName() + ": " + r, e);
            }
        }
        return list;
    }

    @Override
    public void saveAll(List<LibraryItem> items) {
        List<List<String>> rows = new ArrayList<>();
        for (LibraryItem i : items) {
            String[] x = i.getExtraFields();
            rows.add(List.of(i.getKind(), String.valueOf(i.getId()), i.getTitle(), i.getCreator(),
                    String.valueOf(i.getTotalCopies()), x[0], x[1]));
        }
        CsvFile.write(file, HEADER, rows);
    }
}
