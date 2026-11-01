package com.autojobapply.application;

import java.util.List;

/** Validation evidence stores field identifiers and outcomes, never applicant values or resume contents. */
public record FormValidationResult(boolean valid, List<String> validatedFields, List<String> unresolvedFields,
                                   List<String> visibleErrors, boolean resumeAttached) {
    public String evidence() {
        return "validated=" + String.join(";", validatedFields) + "|unresolved=" + String.join(";", unresolvedFields)
                + "|errors=" + String.join(";", visibleErrors) + "|resumeAttached=" + resumeAttached;
    }
}
