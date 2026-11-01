package com.autojobapply.application;

import com.autojobapply.model.ApplicantProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fail-closed validation for required application inputs before a Submit control may be used. */
public final class ApplicationValidationGate {
    private final ApplicantAnswerResolver answers = new ApplicantAnswerResolver();

    public FormValidationResult validate(ApplicantProfile profile, Path resume, List<FormField> fields, List<String> visibleErrors) {
        List<String> validated = new ArrayList<>(), unresolved = new ArrayList<>();
        String legalName = profile.get("legal.name"), email = profile.get("email"), phone = profile.get("phone");
        if (legalName.isBlank()) unresolved.add("legal.name:missing-profile-value");
        if (email.isBlank()) unresolved.add("email:missing-profile-value");
        if (phone.isBlank()) unresolved.add("phone:missing-profile-value");
        if (!Files.isRegularFile(resume)) unresolved.add("resume:file-not-found");
        boolean resumeAttached = false;
        for (FormField field : fields) {
            String key = normalize(field.name());
            if (field.resumeAttachment()) { resumeAttached = !field.blank(); if (field.required() && !resumeAttached) unresolved.add(field.label() + ":resume-not-attached"); else if (resumeAttached) validated.add(field.label() + ":resume-attached"); continue; }
            if (!field.required()) continue;
            if (!field.supported()) { unresolved.add(field.label() + ":missing-profile-answer-or-unsupported-question"); continue; }
            if (field.blank()) { unresolved.add(field.label() + ":missing-required-value"); continue; }
            String expected = answers.answer(profile, field.label()).orElseGet(() -> expectedValue(key, legalName, email, phone, profile.get("location"), profile.get("country")));
            if (expected == null) unresolved.add(field.label() + ":missing-profile-answer-or-unsupported-question");
            else if (!same(key, expected, field.value())) unresolved.add(field.label() + ":does-not-match-verified-profile");
            else validated.add(field.label());
        }
        if (!visibleErrors.isEmpty()) unresolved.add("visible-form-errors");
        if (!resumeAttached) unresolved.add("resume:attachment-not-verified");
        return new FormValidationResult(unresolved.isEmpty() && visibleErrors.isEmpty() && resumeAttached, List.copyOf(validated), List.copyOf(unresolved), List.copyOf(visibleErrors), resumeAttached);
    }

    private String expectedValue(String key, String legalName, String email, String phone, String location, String country) { if (key.contains("first") && key.contains("name")) return first(legalName); if (key.contains("last") && key.contains("name")) return last(legalName); if (key.contains("email")) return email; if (key.contains("phone")) return phone; if (key.contains("location")) return location; if (key.contains("country")) return country; return null; }
    private boolean same(String key, String expected, String actual) { return key.contains("phone") ? digits(expected).equals(digits(actual)) : expected.trim().equalsIgnoreCase(actual.trim()); }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private String first(String name) { return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0]; }
    private String last(String name) { String[] parts = name == null ? new String[0] : name.trim().split("\\s+"); return parts.length < 2 ? "" : parts[parts.length - 1]; }
}
