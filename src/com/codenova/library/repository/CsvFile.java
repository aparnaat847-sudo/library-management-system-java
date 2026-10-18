package com.codenova.library.repository;

import com.codenova.library.exception.StorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared CSV helper. Writes go to a temp file first and are then moved over the real file,
 * so a crash mid-write can never leave a half-written (corrupt) data file.
 */
final class CsvFile {
    private CsvFile() { }

    static List<List<String>> read(Path file, int expectedColumns) {
        List<List<String>> rows = new ArrayList<>();
        if (!Files.exists(file)) return rows;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                List<String> cols = parseLine(line);
                if (cols.size() != expectedColumns) {
                    throw new StorageException("Corrupt data in " + file.getFileName() + " at line " + (i + 1)
                            + ": expected " + expectedColumns + " columns.", null);
                }
                rows.add(cols);
            }
        } catch (IOException e) {
            throw new StorageException("Could not read " + file, e);
        }
        return rows;
    }

    static void write(Path file, String header, List<List<String>> rows) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, "lib", ".tmp");
            List<String> lines = new ArrayList<>();
            lines.add(header);
            for (List<String> row : rows) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(esc(row.get(i)));
                }
                lines.add(sb.toString());
            }
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not save " + file, e);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(ch);
            } else if (ch == '"') inQuotes = true;
            else if (ch == ',') { out.add(sb.toString()); sb.setLength(0); }
            else sb.append(ch);
        }
        out.add(sb.toString());
        return out;
    }
}
