package com.snippettray.service;

import com.snippettray.db.DatabaseManager;
import com.snippettray.model.SnippetResult;
import com.snippettray.model.Tag;
import com.snippettray.repo.AppRepository;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public final class SnippetService {
    private final AppRepository repository;
    private final DatabaseManager databaseManager;

    public SnippetService(AppRepository repository, DatabaseManager databaseManager) {
        this.repository = repository;
        this.databaseManager = databaseManager;
    }

    public int addTag(String name) throws SQLException {
        return repository.createTag(name);
    }

    public List<Tag> getTags() throws SQLException {
        return repository.getTags();
    }

    public int addSnippet(String title, String content, List<Integer> tagIds) throws SQLException {
        return repository.createSnippet(title, content, tagIds);
    }

    public List<SnippetResult> search(Integer tagId, String textQuery) throws SQLException {
        return repository.searchSnippets(tagId, textQuery);
    }

    public int countSnippetsUsingTag(int tagId) throws SQLException {
        return repository.countSnippetsUsingTag(tagId);
    }

    public void deleteTag(int tagId) throws SQLException {
        repository.deleteTag(tagId);
    }

    public void deleteSnippet(int snippetId) throws SQLException {
        repository.deleteSnippet(snippetId);
    }

    public List<Integer> getSnippetTagIds(int snippetId) throws SQLException {
        return repository.getSnippetTagIds(snippetId);
    }

    public void updateSnippet(int snippetId, String title, String content, List<Integer> tagIds) throws SQLException {
        repository.updateSnippet(snippetId, title, content, tagIds);
    }

    public Path getDatabasePath() {
        return databaseManager.getDatabasePath();
    }

    public void shutdown() {
        databaseManager.closeQuietly();
    }
}
