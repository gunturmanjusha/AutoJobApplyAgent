package com.autojobapply.application;

import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Job;
import java.util.*;

/** Maps only explicit, verified applicant-profile values to semantically recognized form labels. */
final class ApplicantAnswerResolver {
    Optional<String> answer(ApplicantProfile profile, Job job, String label, int maxLength) {
        Optional<String> factual = answer(profile, label);
        if (factual.isPresent()) return factual;
        return writtenAnswer(profile, job, normalize(label), maxLength);
    }

    Optional<String> answer(ApplicantProfile profile, String label) {
        String key = normalize(label);
        Optional<String> configured = value(profile.get("answer." + key.replace(' ', '.')));
        if (configured.isPresent()) return configured;
        if (key.contains("preferred") && key.contains("first") && key.contains("name")) return value(first(profile.get("legal.name")));
        if (key.contains("location")) return value(profile.get("location"));
        if (key.equals("country") || key.contains("country of residence")) return value(profile.get("country"));
        if (key.contains("state") && key.contains("residence")) return value(profile.get("state"));
        if (key.contains("city") && key.contains("residence")) return value(profile.get("city"));
        if (key.equals("address")) return value(profile.get("address"));
        if (key.contains("zip") || key.contains("postal code")) return value(profile.get("postal.code"));
        if (key.contains("authorized to work") || key.contains("work authorization")) return yesNo(profile.get("work.authorization.answer"));
        if ((key.contains("sponsorship") || key.contains("sponsor")) && (key.contains("require") || key.contains("need"))) return yesNo(profile.get("sponsorship.required.answer"));
        if (key.contains("i 140") || key.contains("immigration petition")) return yesNo(profile.get("immigration.i140.approved.answer"));
        if (key.contains("years") && key.contains("experience")) return value(profile.get("experience.years"));
        if (key.contains("current or last company")) return value(profile.get("employment.1.company"));
        if (key.contains("current or last job title")) return value(profile.get("employment.1.title"));
        if (key.contains("education") || key.contains("degree")) return value(profile.get("education.degree"));
        if (key.contains("certification")) return value(profile.get("certifications"));
        if (key.contains("relocat")) return yesNo(profile.get("relocation.answer"));
        if (key.contains("salary") || key.contains("compensation")) return value(profile.get("compensation.expectation"));
        if (key.contains("linkedin")) return value(profile.get("linkedin.url"));
        if (key.contains("referred") || key.contains("referral")) return yesNo(profile.get("referral.answer"));
        if (key.contains("text message") || key.contains("sms")) return value(profile.get("sms.consent.answer"));
        return Optional.empty();
    }

    private Optional<String> writtenAnswer(ApplicantProfile profile, Job job, String key, int maxLength) {
        if (job == null || maxLength < 40 || !(key.contains("good fit") || key.contains("why") || key.contains("relevant experience") || key.contains("background meet") || key.contains("java") || key.contains("spring") || key.contains("kafka") || key.contains("distributed") || key.contains("architecture"))) return Optional.empty();
        String years = profile.get("experience.years"), summary = profile.get("professional.summary"), skills = profile.get("skills.verified"), highlights = profile.get("employment.1.highlights");
        if (years.isBlank() || summary.isBlank() || skills.isBlank() || highlights.isBlank()) return Optional.empty();
        List<String> matched = Arrays.stream(skills.split(",")).map(String::trim).filter(skill -> !skill.isBlank() && job.description().toLowerCase(Locale.ROOT).contains(skill.toLowerCase(Locale.ROOT))).toList();
        if (matched.isEmpty()) return Optional.empty();
        String answer = "With " + years + " years of experience, " + summary + " My verified experience includes " + String.join(", ", matched) + ". " + highlights;
        return value(limit(answer, maxLength));
    }

    private Optional<String> yesNo(String value) { return "yes".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) ? Optional.of(value) : Optional.empty(); }
    private Optional<String> value(String value) { return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim()); }
    private String limit(String value, int maxLength) { if (value.length() <= maxLength) return value; int cut = value.lastIndexOf(' ', maxLength - 1); return (cut < 20 ? value.substring(0, maxLength) : value.substring(0, cut)).trim(); }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim(); }
    private String first(String name) { return name == null || name.isBlank() ? "" : name.trim().split("\\s+")[0]; }
}
