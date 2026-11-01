package com.autojobapply.application;

import com.autojobapply.config.ExecutionMode;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Resume;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fixture tests prove that a failed validation result cannot invoke a submit action. */
public final class ApplicationValidationSafetyTest {
    private static final Pattern INPUT = Pattern.compile("<input\\s+([^>]*)>");
    private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)=(?:\"([^\"]*)\")");

    public static void main(String[] args) throws Exception {
        Path resume = Files.createTempFile("resume", ".docx");
        Properties values = new Properties();
        values.setProperty("legal.name", "Jane Doe");
        values.setProperty("email", "jane@example.com");
        values.setProperty("phone", "4155551212");
        values.setProperty("location", "San Francisco, CA");
        ApplicantProfile profile = new ApplicantProfile(values, new Resume("", "Jane Doe", "jane@example.com", "4155551212", Set.of()));
        ApplicationValidationGate gate = new ApplicationValidationGate();

        assertOutcome(gate, profile, resume, "complete.html", true);
        assertOutcome(gate, profile, resume, "missing-required.html", false);
        assertOutcome(gate, profile, resume, "missing-resume.html", false);
        assertOutcome(gate, profile, resume, "unanswered-screening.html", false);
        assertOutcome(gate, profile, resume, "invalid-applicant.html", false);
        assertOutcome(gate, profile, resume, "visible-error.html", false);
        assertOutcome(gate, profile, resume, "unsupported-required.html", false);
        Files.deleteIfExists(resume);
        System.out.println("APPLICATION_VALIDATION_SAFETY_TEST_PASSED");
    }

    private static void assertOutcome(ApplicationValidationGate gate, ApplicantProfile profile, Path resume,
                                      String fixture, boolean expectedValid) throws Exception {
        String html = new String(ApplicationValidationSafetyTest.class.getResourceAsStream("/greenhouse-forms/" + fixture).readAllBytes());
        FormValidationResult result = gate.validate(profile, resume, fields(html), html.contains("field-error") ? List.of("visible-form-error") : List.of());
        if (result.valid() != expectedValid) throw new AssertionError(fixture + " validation result");
        AtomicInteger submits = new AtomicInteger();
        boolean submitted = new SubmissionSafetyGate().submitIfAllowed(ExecutionMode.LIVE, true, true, result, submits::incrementAndGet);
        if (submitted != expectedValid || submits.get() != (expectedValid ? 1 : 0)) throw new AssertionError(fixture + " submit safety");
        if (new SubmissionSafetyGate().submitIfAllowed(ExecutionMode.DRY_RUN, true, true, result, submits::incrementAndGet))
            throw new AssertionError(fixture + " dry run submitted");
    }

    private static List<FormField> fields(String html) {
        List<FormField> fields = new ArrayList<>();
        Matcher inputs = INPUT.matcher(html);
        while (inputs.find()) {
            String attributes = inputs.group(1);
            String name = attribute(attributes, "name");
            String type = attribute(attributes, "type");
            boolean resume = "file".equals(type);
            boolean required = attributes.contains("required");
            boolean screening = name.contains("question") || name.contains("custom");
            boolean supported = resume || name.contains("first_name") || name.contains("last_name") || name.contains("email") || name.contains("phone") || name.contains("location");
            fields.add(new FormField(name, required, supported, screening, resume, attribute(attributes, "value")));
        }
        return fields;
    }

    private static String attribute(String attributes, String name) {
        Matcher matcher = ATTRIBUTE.matcher(attributes);
        while (matcher.find()) if (name.equals(matcher.group(1))) return matcher.group(2);
        return "";
    }
}
