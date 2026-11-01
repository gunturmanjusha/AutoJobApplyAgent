package com.autojobapply.application;

/** A local, value-free representation of a field encountered on an application form. */
public record FormField(String name, boolean required, boolean supported, boolean screeningQuestion,
                        boolean resumeAttachment, String value, String label) {
    public FormField(String name, boolean required, boolean supported, boolean screeningQuestion, boolean resumeAttachment, String value) { this(name, required, supported, screeningQuestion, resumeAttachment, value, name); }
    public boolean blank() { return value == null || value.isBlank(); }
}
