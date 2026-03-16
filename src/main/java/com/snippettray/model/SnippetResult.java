package com.snippettray.model;

public record SnippetResult(int id, String title, String content, String createdAt, String tagsCsv) {
}
