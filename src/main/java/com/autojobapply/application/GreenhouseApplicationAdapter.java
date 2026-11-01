package com.autojobapply.application;

import com.autojobapply.config.ExecutionMode;
import com.autojobapply.model.ApplicationStatus;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.ScoredJob;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Greenhouse direct-form adapter. Required fields are validated before any submit action is possible. */
public final class GreenhouseApplicationAdapter {
    private final ApplicationValidationGate validationGate = new ApplicationValidationGate();

    /** Compatibility wrapper for existing callers. A true value is treated as an explicitly requested LIVE attempt. */
    public ApplicationStatus apply(ScoredJob scored, ApplicantProfile profile, Path resume, boolean submit) {
        return applyDetailed(scored, profile, resume, submit ? ExecutionMode.LIVE : ExecutionMode.DRY_RUN,
                submit, submit).status();
    }

    public ApplicationAttempt applyDetailed(ScoredJob scored, ApplicantProfile profile, Path resume, ExecutionMode mode,
                                            boolean autoSubmit, boolean employerAuthorization) {
        if (mode == ExecutionMode.DISCOVER_ONLY) return attempt(ApplicationStatus.MATCHED, null, false, "discovery-only");
        if (!scored.job().applicationUrl().contains("greenhouse.io")) return attempt(ApplicationStatus.UNSUPPORTED, null, false, "not-greenhouse");
        if (profile.get("legal.name").isBlank() || profile.get("email").isBlank() || profile.get("phone").isBlank()
                || !Files.isRegularFile(resume)) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, null, false, "missing-verified-profile-or-resume");

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(mode != ExecutionMode.LIVE));
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(scored.job().applicationUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            if (page.locator("input[type=password]").count() > 0 || page.getByText("Sign in").count() > 0 || page.getByText("Log in").count() > 0)
                return attempt(ApplicationStatus.LOGIN_REQUIRED, null, false, "login-required");
            Locator form = page.locator("form").first();
            if (form.count() == 0) return attempt(ApplicationStatus.UNSUPPORTED, null, false, "application-form-not-found");

            fill(form, "input[name*=first_name],input[id*=first_name]", first(profile.get("legal.name")));
            fill(form, "input[name*=last_name],input[id*=last_name]", last(profile.get("legal.name")));
            fill(form, "input[type=email],input[name*=email],input[id=email],input[autocomplete=email]", profile.get("email"));
            fill(form, "input[type=tel],input[name*=phone]", profile.get("phone"));
            fill(form, "input[name*=location],input[id*=location]", profile.get("location"));
            fill(form, "input[name*=country],input[id*=country]", profile.get("country"));
            select(form, "select[name*=country],select[id*=country]", profile.get("country"));
            Locator upload = form.locator("input[type=file]").first();
            if (upload.count() == 0) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, null, false, "resume-control-not-found");
            upload.setInputFiles(resume);

            FormValidationResult validation = validationGate.validate(profile, resume, inspectedFields(form), visibleErrors(form));
            if (!validation.valid()) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "validation-failed");
            if (mode == ExecutionMode.DRY_RUN) return attempt(ApplicationStatus.APPLYING, validation, false, "dry-run-validated-no-submit");

            Locator button = form.locator("button[type=submit],input[type=submit]").first();
            if (button.count() == 0) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "submit-control-not-found");
            String before = page.url();
            boolean clicked = new SubmissionSafetyGate().submitIfAllowed(mode, autoSubmit, employerAuthorization, validation, button::click);
            if (!clicked) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "live-authorization-not-enabled");
            page.waitForTimeout(1500);
            boolean transitioned = !before.equals(page.url());
            boolean success = page.locator("[data-qa*=success i],.application-confirmation,#application_confirmation,.thank-you").count() > 0;
            return transitioned && success
                    ? attempt(ApplicationStatus.SUBMITTED, validation, true, "post-submit-transition-and-success-component")
                    : attempt(ApplicationStatus.NEEDS_VERIFICATION, validation, true, "no-credible-employer-confirmation");
        } catch (Exception exception) {
            String detail = exception.getMessage() == null ? "no detail" : exception.getMessage().replaceAll("[\\r\\n]+", " ").trim();
            return attempt(ApplicationStatus.FAILED, null, false, exception.getClass().getSimpleName() + ": " + detail.substring(0, Math.min(detail.length(), 500)));
        }
    }

    private List<FormField> inspectedFields(Locator form) {
        List<FormField> fields = new ArrayList<>();
        Locator inputs = form.locator("input,textarea,select");
        for (int index = 0; index < inputs.count(); index++) {
            Locator input = inputs.nth(index);
            String type = attribute(input, "type");
            String name = firstNonBlank(attribute(input, "name"), attribute(input, "id"), "unnamed-field-" + index);
            boolean resume = "file".equalsIgnoreCase(type);
            boolean required = input.getAttribute("required") != null || "true".equalsIgnoreCase(attribute(input, "aria-required"));
            boolean screening = name.toLowerCase().contains("question") || name.toLowerCase().contains("custom");
            boolean supported = resume || supported(name);
            String value = input.inputValue();
            fields.add(new FormField(name, required, supported, screening, resume, value));
        }
        return fields;
    }

    private List<String> visibleErrors(Locator form) {
        Locator errors = form.locator("[aria-invalid='true'],.field-error,.error-message,[data-qa*=error i]");
        List<String> result = new ArrayList<>();
        for (int index = 0; index < errors.count(); index++) result.add("form-error-" + index);
        return result;
    }

    private boolean supported(String name) {
        String value = name.toLowerCase();
        return value.contains("first") && value.contains("name") || value.contains("last") && value.contains("name")
                || value.contains("email") || value.contains("phone") || value.contains("location") || value.contains("country");
    }

    private String attribute(Locator locator, String name) { String value = locator.getAttribute(name); return value == null ? "" : value; }
    private String firstNonBlank(String first, String second, String fallback) { return !first.isBlank() ? first : !second.isBlank() ? second : fallback; }
    private void fill(Locator root, String selector, String value) { if (value == null || value.isBlank()) return; Locator locator = root.locator(selector).first(); if (locator.count() > 0) locator.fill(value); }
    private void select(Locator root, String selector, String value) { if (value == null || value.isBlank()) return; Locator locator = root.locator(selector).first(); if (locator.count() > 0) locator.selectOption(new SelectOption().setLabel(value)); }
    private String first(String name) { return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0]; }
    private String last(String name) { String[] parts = name == null ? new String[0] : name.trim().split("\\s+"); return parts.length < 2 ? "" : parts[parts.length - 1]; }
    private ApplicationAttempt attempt(ApplicationStatus status, FormValidationResult validation, boolean submitted, String confirmation) { return new ApplicationAttempt(status, validation, submitted, confirmation); }
}
