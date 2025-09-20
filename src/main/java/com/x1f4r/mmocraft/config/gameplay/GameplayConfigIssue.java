package com.x1f4r.mmocraft.config.gameplay;

/**
 * Describes an issue encountered while loading a gameplay configuration file.
 */
public record GameplayConfigIssue(Severity severity, String message, String detail) {

    public GameplayConfigIssue {
        if (severity == null) {
            throw new IllegalArgumentException("severity");
        }
    }

    public static GameplayConfigIssue info(String message) {
        return new GameplayConfigIssue(Severity.INFO, message, null);
    }

    public static GameplayConfigIssue warn(String message, String detail) {
        return new GameplayConfigIssue(Severity.WARNING, message, detail);
    }

    public static GameplayConfigIssue error(String message, String detail) {
        return new GameplayConfigIssue(Severity.ERROR, message, detail);
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
