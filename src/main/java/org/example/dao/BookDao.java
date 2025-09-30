package org.example.dao;

import org.example.db.Db;
import org.example.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDao {

    public static final String[] BOOK_COLS = {"id", "title", "genre", "isbn", "author_id"};

    private static final String SELECT_RANGE = """
        SELECT id, title, genre, isbn, author_id
        FROM books
        WHERE id BETWEEN ? AND ?
        ORDER BY id
        """;

    private static final String UPDATE_SQL = """
        UPDATE books SET title = ?, genre = ?, isbn = ?
        WHERE id = ?
        """;

    public long[] minMaxAndCount() throws SQLException {
        try (Connection c = Db.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MIN(id),0), COALESCE(MAX(id),-1), COUNT(*) FROM books")) {
            rs.next();
            return new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)};
        }
    }

    public List<Book> readRange(long fromId, long toId) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(SELECT_RANGE)) {
            ps.setLong(1, fromId);
            ps.setLong(2, toId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Book(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getLong(5)
                    ));
                }
                return out;
            }
        }
    }

    public void updateBatch(List<Book> books) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(UPDATE_SQL)) {
            c.setAutoCommit(false);
            for (Book b : books) {
                ps.setString(1, b.getTitle());
                ps.setString(2, b.getGenre());
                ps.setString(3, b.getIsbn());
                ps.setLong(4, b.getId());
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        }
    }

    public void ensureSchemaAndData() throws SQLException {
        try (Connection c = Db.get();
             Statement st = c.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS authors (
                  id BIGSERIAL PRIMARY KEY,
                  name TEXT NOT NULL,
                  email TEXT,
                  city  TEXT
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS books (
                  id BIGSERIAL PRIMARY KEY,
                  title TEXT NOT NULL,
                  genre TEXT,
                  isbn  TEXT NOT NULL,
                  author_id BIGINT NOT NULL REFERENCES authors(id) ON DELETE CASCADE
                )
            """);
        }


        try (Connection c = Db.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT (SELECT COUNT(*) FROM authors), (SELECT COUNT(*) FROM books)")) {
            rs.next();
            long ac = rs.getLong(1), bc = rs.getLong(2);
            if (ac == 0) insertAuthors(1000);
            if (bc == 0) insertBooks(2000);
        }}
    private void insertAuthors(int count) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO authors(name, email, city) VALUES (?,?,?)")) {
            c.setAutoCommit(false);
            for (int i = 1; i <= count; i++) {
                ps.setString(1, "Author " + i);
                ps.setString(2, "author" + i + "@example.com");
                ps.setString(3, "City " + (i % 100));
                ps.addBatch();
                if (i % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            c.commit();
        }
    }

    private void insertBooks(int count) throws SQLException {
        String[] genres = {"fantasy","crime","drama","sci-fi","poetry"};
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO books(title, genre, isbn, author_id) VALUES (?,?,?,?)")) {
            c.setAutoCommit(false);
            for (int i = 1; i <= count; i++) {
                ps.setString(1, "Book " + i);
                ps.setString(2, genres[i % genres.length]);
                ps.setString(3, String.format("%013d", i));
                ps.setLong(4, 1 + (i % 1000));
                ps.addBatch();
                if (i % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            c.commit();
        }
    }
}
