package org.example.config;

public final class Config {

    public static final String DB_URL  = System.getenv().getOrDefault("DB_URL",  "jdbc:postgresql://localhost:5432/minilab");
    public static final String DB_USER = System.getenv().getOrDefault("DB_USER", "postgres");
    public static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "1234");

    public static final int READ_CHUNK_SIZE   = 500;
    public static final int UPDATE_BATCH_SIZE = 500;
    public static final int MODIFY_PART_SIZE  = 500;

    private Config() {

    }
}
