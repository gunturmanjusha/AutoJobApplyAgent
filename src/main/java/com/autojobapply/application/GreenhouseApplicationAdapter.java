package com.autojobapply.application;

import com.autojobapply.config.ExecutionMode;
import com.autojobapply.model.ApplicationStatus;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.ScoredJob;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Greenhouse direct-form adapter. Required fields are validated before any submit action is possible. */
public final class GreenhouseApplicationAdapter {
    private final ApplicationValidationGate validationGate = new ApplicationValidationGate();
    private final ApplicantAnswerResolver answers = new ApplicantAnswerResolver();

    public ApplicationStatus apply(ScoredJob scored, ApplicantProfile profile, Path resume, boolean submit) { return applyDetailed(scored, profile, resume, submit ? ExecutionMode.LIVE : ExecutionMode.DRY_RUN, submit, submit).status(); }

    public ApplicationAttempt applyDetailed(ScoredJob scored, ApplicantProfile profile, Path resume, ExecutionMode mode, boolean autoSubmit, boolean employerAuthorization) {
        if (mode == ExecutionMode.DISCOVER_ONLY) return attempt(ApplicationStatus.MATCHED, null, false, "discovery-only");
        if (!scored.job().applicationUrl().contains("greenhouse.io")) return attempt(ApplicationStatus.UNSUPPORTED, null, false, "not-greenhouse");
        if (profile.get("legal.name").isBlank() || profile.get("email").isBlank() || profile.get("phone").isBlank() || !Files.isRegularFile(resume)) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, null, false, "missing-verified-profile-or-resume");
        try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(mode != ExecutionMode.LIVE)); BrowserContext context = browser.newContext()) {
            Page page = context.newPage(); page.navigate(scored.job().applicationUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED)); page.waitForTimeout(750);
            if (page.locator("input[type=password]").count() > 0 || page.getByText("Sign in").count() > 0 || page.getByText("Log in").count() > 0) return attempt(ApplicationStatus.LOGIN_REQUIRED, null, false, "login-required");
            Locator form = page.locator("form").first(); if (form.count() == 0) return attempt(ApplicationStatus.UNSUPPORTED, null, false, "application-form-not-found");
            fill(form, "input[name*=first_name],input[id*=first_name]", first(profile.get("legal.name")));
            fill(form, "input[name*=last_name],input[id*=last_name]", last(profile.get("legal.name")));
            fill(form, "input[type=email],input[name*=email],input[id=email],input[autocomplete=email]", profile.get("email"));
            fill(form, "input[type=tel],input[name*=phone]", profile.get("phone"));
            Map<String, String> answered = applyProfileAnswers(page, form, profile, scored.job());
            Locator upload = form.locator("input[type=file][name*=resume],input[type=file][id*=resume]").first(); if (upload.count() == 0) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, null, false, "resume-control-not-found"); upload.setInputFiles(resume); page.waitForTimeout(500);
            boolean resumeAttached = Boolean.TRUE.equals(form.locator("input[type=file]").evaluateAll("inputs => inputs.some(input => input.files && input.files.length > 0)")) || form.innerText().contains(resume.getFileName().toString());
            FormValidationResult validation = validationGate.validate(profile, resume, inspectedFields(form, answered, resumeAttached), answered, visibleErrors(form));
            if (!validation.valid()) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "validation-failed");
            if (mode == ExecutionMode.DRY_RUN) return attempt(ApplicationStatus.APPLYING, validation, false, "READY_FOR_SUBMISSION_IN_DRY_RUN");
            Locator button = form.locator("button[type=submit],input[type=submit]").first(); if (button.count() == 0) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "submit-control-not-found");
            String before = page.url(); boolean clicked = new SubmissionSafetyGate().submitIfAllowed(mode, autoSubmit, employerAuthorization, validation, button::click);
            if (!clicked) return attempt(ApplicationStatus.NEEDS_MANUAL_ACTION, validation, false, "live-authorization-not-enabled");
            page.waitForTimeout(1500); boolean transitioned = !before.equals(page.url()); boolean success = page.locator("[data-qa*=success i],.application-confirmation,#application_confirmation,.thank-you").count() > 0;
            return transitioned && success ? attempt(ApplicationStatus.SUBMITTED, validation, true, "post-submit-transition-and-success-component") : attempt(ApplicationStatus.NEEDS_VERIFICATION, validation, true, "no-credible-employer-confirmation");
        } catch (Exception exception) { String detail = exception.getMessage() == null ? "no detail" : exception.getMessage().replaceAll("[\\r\\n]+", " ").trim(); return attempt(ApplicationStatus.FAILED, null, false, exception.getClass().getSimpleName() + ": " + detail.substring(0, Math.min(detail.length(), 500))); }
    }

    private Map<String, String> applyProfileAnswers(Page page, Locator form, ApplicantProfile profile, com.autojobapply.model.Job job) {
        Map<String, String> answered = new HashMap<>(); Locator controls = form.locator("input,textarea,select");
        for (int index = 0; index < controls.count(); index++) {
            Locator control = controls.nth(index); if ("file".equalsIgnoreCase(attribute(control, "type")) || "true".equalsIgnoreCase(attribute(control, "aria-hidden"))) continue;
            String name = firstNonBlank(attribute(control, "name"), attribute(control, "id"), "unnamed-field-" + index); String label = label(control, name);
            Optional<String> answer = answers.answer(profile, job, label, maxLength(control)); if (answer.isEmpty()) continue;
            if (choose(page, control, answer.get())) answered.put(name, answer.get());
        }
        return answered;
    }

    private boolean choose(Page page, Locator control, String answer) {
        String role = attribute(control, "role");
        if ("combobox".equalsIgnoreCase(role)) { control.click(); control.fill(answer); page.waitForTimeout(500); control.press("ArrowDown"); Locator options = page.locator("[role=option]:visible"); for (int i = 0; i < options.count(); i++) if (compatibleOption(control, answer, options.nth(i).textContent())) { options.nth(i).click(); return selectedComboboxValue(control, answer); } return false; }
        if ("select".equalsIgnoreCase((String) control.evaluate("e => e.tagName"))) { control.selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(answer)); return same(answer, control.inputValue()); }
        control.fill(answer); return same(answer, control.inputValue());
    }

    private List<FormField> inspectedFields(Locator form, Map<String, String> answered, boolean resumeAttached) {
        List<FormField> fields = new ArrayList<>(); Locator controls = form.locator("input,textarea,select");
        boolean sawResume = false;
        for (int index = 0; index < controls.count(); index++) { Locator control = controls.nth(index); if ("true".equalsIgnoreCase(attribute(control, "aria-hidden"))) continue; String type = attribute(control, "type"); String name = firstNonBlank(attribute(control, "name"), attribute(control, "id"), "unnamed-field-" + index); String label = label(control, name); boolean resume = "file".equalsIgnoreCase(type); boolean required = control.getAttribute("required") != null || "true".equalsIgnoreCase(attribute(control, "aria-required")); boolean screening = name.toLowerCase().contains("question") || name.toLowerCase().contains("custom"); boolean supported = resume || standard(name) || answered.containsKey(name); if (resume) sawResume = true; String value = resume ? (resumeAttached ? "attached" : "") : answered.getOrDefault(name, control.inputValue()); fields.add(new FormField(name, required, supported, screening, resume, value, label)); }
        if (!sawResume) fields.add(new FormField("resume", true, true, false, true, resumeAttached ? "attached" : "", "Resume/CV"));
        return fields;
    }
    private List<String> visibleErrors(Locator form) { Locator errors = form.locator("[aria-invalid='true'],.field-error,.error-message,[data-qa*=error i]"); List<String> result = new ArrayList<>(); for (int i = 0; i < errors.count(); i++) result.add("form-error-" + i); return result; }
    private boolean standard(String name) { String value = name.toLowerCase(); return value.contains("first") && value.contains("name") || value.contains("last") && value.contains("name") || value.contains("email") || value.contains("phone") || value.contains("location") || value.contains("country"); }
    private String label(Locator control, String fallback) { String aria = attribute(control, "aria-label"); if (!aria.isBlank()) return aria; Locator form = control.locator("xpath=ancestor::form[1]"); String labelledBy = attribute(control, "aria-labelledby"); if (!labelledBy.isBlank()) { Locator labelled = form.locator("[id='" + labelledBy.split("\\s+")[0] + "']"); if (labelled.count() > 0) return labelled.first().textContent(); } String id = attribute(control, "id"); if (!id.isBlank()) { Locator label = form.locator("label[for='" + id + "']"); if (label.count() > 0) return label.first().textContent(); } return fallback; }
    private String attribute(Locator locator, String name) { String value = locator.getAttribute(name); return value == null ? "" : value; }
    private int maxLength(Locator control) { try { String value = attribute(control, "maxlength"); return value.isBlank() ? 1000 : Integer.parseInt(value); } catch (NumberFormatException ignored) { return 1000; } }
    private boolean compatibleOption(Locator control, String answer, String option) { if (same(answer, option)) return true; String id = firstNonBlank(attribute(control, "id"), attribute(control, "name"), "").toLowerCase(Locale.ROOT); return id.contains("country") && option != null && option.trim().matches("(?i)" + java.util.regex.Pattern.quote(answer.trim()) + "\\s*\\+\\d+"); }
    private boolean selectedComboboxValue(Locator control, String answer) { Locator shell = control.locator("xpath=ancestor::*[contains(@class,'select-shell')][1]"); if (shell.count() == 0 || shell.locator("[class*='value-container--has-value']").count() == 0) return false; String id = firstNonBlank(attribute(control, "id"), attribute(control, "name"), "").toLowerCase(Locale.ROOT); return id.contains("country") || shell.innerText().toLowerCase(Locale.ROOT).contains(answer.toLowerCase(Locale.ROOT)); }
    private String firstNonBlank(String first, String second, String fallback) { return !first.isBlank() ? first : !second.isBlank() ? second : fallback; }
    private void fill(Locator root, String selector, String value) { if (value == null || value.isBlank()) return; Locator locator = root.locator(selector).first(); if (locator.count() > 0) locator.fill(value); }
    private boolean same(String expected, String actual) { return expected != null && actual != null && expected.trim().equalsIgnoreCase(actual.trim()); }
    private String first(String name) { return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0]; }
    private String last(String name) { String[] parts = name == null ? new String[0] : name.trim().split("\\s+"); return parts.length < 2 ? "" : parts[parts.length - 1]; }
    private ApplicationAttempt attempt(ApplicationStatus status, FormValidationResult validation, boolean submitted, String confirmation) { return new ApplicationAttempt(status, validation, submitted, confirmation); }
}
