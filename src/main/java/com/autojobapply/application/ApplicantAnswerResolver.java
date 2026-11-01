package com.autojobapply.application;

import com.autojobapply.model.ApplicantProfile;
import java.util.Locale;
import java.util.Optional;

/** Maps only explicit, verified applicant-profile values to semantically recognized form labels. */
final class ApplicantAnswerResolver {
    Optional<String> answer(ApplicantProfile profile, String label) {
        String key = normalize(label);
        if (key.contains("preferred") && key.contains("first") && key.contains("name")) return value(first(profile.get("legal.name")));
        if (key.contains("location")) return value(profile.get("location"));
        if (key.equals("country") || key.contains("country of residence")) return value(profile.get("country"));
        if (key.contains("state") && key.contains("residence")) return value(profile.get("state"));
        if (key.contains("city") && key.contains("residence")) return value(profile.get("city"));
        if (key.equals("address")) return value(profile.get("address"));
        if (key.contains("zip") || key.contains("postal code")) return value(profile.get("postal.code"));
        if (key.contains("authorized to work") || key.contains("work authorization")) return yesNo(profile.get("work.authorization.answer"));
        if (key.contains("current or last company")) return value(profile.get("employment.1.company"));
        if (key.contains("current or last job title")) return value(profile.get("employment.1.title"));
        if (key.contains("linkedin")) return value(profile.get("linkedin.url"));
        if (key.contains("referred") || key.contains("referral")) return yesNo(profile.get("referral.answer"));
        if (key.contains("text message") || key.contains("sms")) return value(profile.get("sms.consent.answer"));
        return Optional.empty();
    }

    private Optional<String> yesNo(String value) { return "yes".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) ? Optional.of(value) : Optional.empty(); }
    private Optional<String> value(String value) { return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim()); }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim(); }
    private String first(String name) { return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0]; }
}
