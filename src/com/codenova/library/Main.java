package com.codenova.library;

import com.codenova.library.repository.ItemCsvRepository;
import com.codenova.library.repository.LoanCsvRepository;
import com.codenova.library.repository.MemberCsvRepository;
import com.codenova.library.service.LibraryService;
import com.codenova.library.ui.LibraryFrame;

import javax.swing.JOptionPane;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

/** Entry point. Usage: java -cp out com.codenova.library.Main [dataFolder]  (default: data) */
public class Main {
    public static void main(String[] args) {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        try {
            LibraryService service = new LibraryService(
                    new ItemCsvRepository(dir.resolve("items.csv")),
                    new MemberCsvRepository(dir.resolve("members.csv")),
                    new LoanCsvRepository(dir.resolve("loans.csv")),
                    Clock.systemDefaultZone());
            LibraryFrame.launch(service);
        } catch (RuntimeException e) {
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Fatal: " + e.getMessage());
            } else {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot start", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }
}
