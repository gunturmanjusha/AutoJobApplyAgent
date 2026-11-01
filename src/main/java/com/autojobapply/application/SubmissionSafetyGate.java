package com.autojobapply.application;

import com.autojobapply.config.ExecutionMode;

/** The only gate that may invoke a submit action. It deliberately fails closed. */
public final class SubmissionSafetyGate {
    public boolean submitIfAllowed(ExecutionMode mode, boolean autoSubmit, boolean employerAuthorization,
                                   FormValidationResult validation, Runnable submitAction) {
        if (mode != ExecutionMode.LIVE || !autoSubmit || !employerAuthorization || !validation.valid()) return false;
        submitAction.run();
        return true;
    }
}
