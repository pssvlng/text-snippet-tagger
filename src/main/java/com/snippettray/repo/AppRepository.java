package com.snippettray.repo;

import com.snippettray.model.SnippetResult;
import com.snippettray.model.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class AppRepository {
    private final Connection connection;

    public AppRepository(Connection connection) {
        this.connection = connection;
    }

    public synchronized int createTag(String tagName) throws SQLException {
        String normalized = tagName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Tag name must not be empty.");
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO tags(name) VALUES (?)"
        )) {
            insert.setString(1, normalized);
            insert.executeUpdate();
        }

        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id FROM tags WHERE name = ? COLLATE NOCASE"
        )) {
            select.setString(1, normalized);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        throw new SQLException("Tag could not be created.");
    }

    public synchronized List<Tag> getTags() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name FROM tags ORDER BY LOWER(name)"
        ); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("id"), rs.getString("name")));
            }
        }
        return tags;
    }

    public synchronized int createSnippet(String title, String content, List<Integer> tagIds) throws SQLException {
        String normalizedTitle = title == null ? null : title.trim();
        if (normalizedTitle != null && normalizedTitle.isEmpty()) {
            normalizedTitle = null;
        }

        String normalized = content.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Snippet text must not be empty.");
        }

        int snippetId;
        try (PreparedStatement insertSnippet = connection.prepareStatement(
                "INSERT INTO snippets(title, content) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            if (normalizedTitle == null) {
                insertSnippet.setNull(1, java.sql.Types.VARCHAR);
            } else {
                insertSnippet.setString(1, normalizedTitle);
            }
            insertSnippet.setString(2, normalized);
            insertSnippet.executeUpdate();

            try (ResultSet keys = insertSnippet.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Snippet insert succeeded but no ID returned.");
                }
                snippetId = keys.getInt(1);
            }
        }

        if (tagIds != null && !tagIds.isEmpty()) {
            try (PreparedStatement insertLink = connection.prepareStatement(
                    "INSERT OR IGNORE INTO snippet_tags(snippet_id, tag_id) VALUES (?, ?)"
            )) {
                for (Integer tagId : tagIds) {
                    insertLink.setInt(1, snippetId);
                    insertLink.setInt(2, tagId);
                    insertLink.addBatch();
                }
                insertLink.executeBatch();
            }
        }

        return snippetId;
    }

    public synchronized List<SnippetResult> searchSnippets(Integer tagId, String query) throws SQLException {
        String textQuery = query == null ? "" : query.trim();

        String sql = """
                SELECT
                    s.id,
                    COALESCE(s.title, '') AS title,
                    s.content,
                    s.created_at,
                    COALESCE(GROUP_CONCAT(t.name, ', '), '') AS tags_csv
                FROM snippets s
                LEFT JOIN snippet_tags st ON st.snippet_id = s.id
                LEFT JOIN tags t ON t.id = st.tag_id
                WHERE
                    (? IS NULL OR EXISTS (
                        SELECT 1
                        FROM snippet_tags st2
                        WHERE st2.snippet_id = s.id AND st2.tag_id = ?
                    ))
                    AND (
                        ? = ''
                        OR LOWER(s.content) LIKE '%' || LOWER(?) || '%'
                        OR LOWER(COALESCE(s.title, '')) LIKE '%' || LOWER(?) || '%'
                    )
                GROUP BY s.id
                ORDER BY s.created_at DESC, s.id DESC
                """;

        List<SnippetResult> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (tagId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, tagId);
                statement.setInt(2, tagId);
            }

            statement.setString(3, textQuery);
            statement.setString(4, textQuery);
            statement.setString(5, textQuery);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(new SnippetResult(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("created_at"),
                            rs.getString("tags_csv")
                    ));
                }
            }
        }

        return results;
    }

    public synchronized int countSnippetsUsingTag(int tagId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(DISTINCT snippet_id) AS usage_count FROM snippet_tags WHERE tag_id = ?"
        )) {
            statement.setInt(1, tagId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("usage_count");
                }
            }
        }

        return 0;
    }

    public synchronized void deleteTag(int tagId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM tags WHERE id = ?")) {
            statement.setInt(1, tagId);
            statement.executeUpdate();
        }
    }

    public synchronized void deleteSnippet(int snippetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM snippets WHERE id = ?")) {
            statement.setInt(1, snippetId);
            statement.executeUpdate();
        }
    }
}
