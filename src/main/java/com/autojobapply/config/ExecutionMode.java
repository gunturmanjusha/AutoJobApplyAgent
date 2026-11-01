package com.autojobapply.config;

/** Controls whether the agent only queues work, validates forms, or may submit an authorized form. */
public enum ExecutionMode {
    DISCOVER_ONLY,
    DRY_RUN,
    LIVE;

    public static ExecutionMode from(String value) {
        try {
            return value == null ? DISCOVER_ONLY : valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DISCOVER_ONLY;
        }
    }
}
