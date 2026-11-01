package com.autojobapply.application;

/** A local, value-free representation of a field encountered on an application form. */
public record FormField(String name, boolean required, boolean supported, boolean screeningQuestion,
                        boolean resumeAttachment, String value) {
    public boolean blank() { return value == null || value.isBlank(); }
}
