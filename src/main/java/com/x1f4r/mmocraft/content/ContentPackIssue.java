package com.x1f4r.mmocraft.content;

import java.util.Objects;

/**
 * Represents an issue discovered while loading content packs.
 */
public record ContentPackIssue(Severity severity, String message, String detail) {

    public ContentPackIssue {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    public static ContentPackIssue info(String message, String detail) {
        return new ContentPackIssue(Severity.INFO, message, detail);
    }

    public static ContentPackIssue warning(String message, String detail) {
        return new ContentPackIssue(Severity.WARNING, message, detail);
    }

    public static ContentPackIssue error(String message, String detail) {
        return new ContentPackIssue(Severity.ERROR, message, detail);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
