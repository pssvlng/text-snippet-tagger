package com.snippettray.db;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DATABASE_FILE_NAME = "snippet_tray.db";

    private final Connection connection;
    private final Path databasePath;

    private DatabaseManager(Connection connection, Path databasePath) {
        this.connection = connection;
        this.databasePath = databasePath;
    }

    public static DatabaseManager initialize() throws SQLException, IOException, URISyntaxException {
        Path executionDirectory = resolveExecutionDirectory();
        ensureWritableDirectory(executionDirectory);
        Path dbPath = executionDirectory.resolve(DATABASE_FILE_NAME);

        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS snippets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    )
                    """);

            try {
                statement.execute("ALTER TABLE snippets ADD COLUMN title TEXT");
            } catch (SQLException ignored) {
            }

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS snippet_tags (
                        snippet_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        PRIMARY KEY (snippet_id, tag_id),
                        FOREIGN KEY (snippet_id) REFERENCES snippets(id) ON DELETE CASCADE,
                        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                    )
                    """);

            statement.execute("CREATE INDEX IF NOT EXISTS idx_snippet_tags_tag_id ON snippet_tags(tag_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_snippets_created_at ON snippets(created_at DESC)");
        }

        return new DatabaseManager(connection, dbPath);
    }

    public Connection getConnection() {
        return connection;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public void closeQuietly() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private static Path resolveExecutionDirectory() throws URISyntaxException {
        Path codeSourcePath = Paths.get(DatabaseManager.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toAbsolutePath();

        if (Files.isRegularFile(codeSourcePath) && codeSourcePath.toString().toLowerCase().endsWith(".jar")) {
            return codeSourcePath.getParent();
        }

        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static void ensureWritableDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            throw new IOException("Execution directory does not exist: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IOException("Execution path is not a directory: " + directory);
        }
        if (!Files.isWritable(directory)) {
            throw new IOException("Execution directory is not writable: " + directory);
        }

        Path probe = directory.resolve(".snippet-tray-write-test");
        try {
            Files.deleteIfExists(probe);
            Files.createFile(probe);
        } finally {
            Files.deleteIfExists(probe);
        }
    }
}
