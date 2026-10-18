package com.codenova.library.repository;

import java.util.List;

/** Storage abstraction: CSV today, JDBC (SQLite/H2) later, without touching the service. */
public interface Repository<T> {
    List<T> loadAll();
    void saveAll(List<T> items);
}
