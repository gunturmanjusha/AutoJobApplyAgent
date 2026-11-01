package com.autojobapply.application;

import com.autojobapply.model.ApplicationStatus;

/** Result of a form attempt; a status alone is never interpreted as a validated submission. */
public record ApplicationAttempt(ApplicationStatus status, FormValidationResult validation,
                                 boolean submitAttempted, String confirmation) { }
