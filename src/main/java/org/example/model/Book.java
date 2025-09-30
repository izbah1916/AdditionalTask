package org.example.model;

public class Book {
    private long id;
    private String title;
    private String genre;
    private String isbn;
    private long authorId;

    public Book(long id, String title, String genre, String isbn, long authorId) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.isbn = isbn;
        this.authorId = authorId;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getIsbn() { return isbn; }
    public long getAuthorId() { return authorId; }

    public void setTitle(String title) { this.title = title; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', genre='" + genre +
                "', isbn='" + isbn + "', authorId=" + authorId + "}";
    }
}
