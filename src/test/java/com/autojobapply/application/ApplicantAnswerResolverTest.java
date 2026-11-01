package com.autojobapply.application;

import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Resume;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Offline Babylist-pattern regression: labeled answers validate only when explicit in the profile. */
public final class ApplicantAnswerResolverTest {
    public static void main(String[] args) throws Exception {
        Properties values = profile(); Resume resume = new Resume("", "Jordan Rivera", "jordan.rivera@example.invalid", "4155550147", Set.of()); ApplicantProfile profile = new ApplicantProfile(values, resume);
        ApplicantAnswerResolver resolver = new ApplicantAnswerResolver();
        check("California".equals(resolver.answer(profile, "State or Canadian Province/ Territory of Residence").orElseThrow()), "state label");
        check(resolver.answer(profile, "Were you referred to this role by a current employee?").isEmpty(), "missing referral must not be invented");
        Path attachment = Files.createTempFile("fictional", ".docx");
        List<FormField> fields = List.of(new FormField("preferred_name", true, true, false, false, "Jordan", "Preferred First Name"), new FormField("question_country", true, true, true, false, "United States", "Country of Residence"), new FormField("question_state", true, true, true, false, "California", "State or Canadian Province/ Territory of Residence"), new FormField("question_city", true, true, true, false, "San Francisco", "City of Residence"), new FormField("question_address", true, true, true, false, "100 Fictional Market Street", "Address"), new FormField("question_zip", true, true, true, false, "94105", "Zip Code (Postal Code)"), new FormField("question_auth", true, true, true, false, "Yes", "Are you currently authorized to work for any employer"), new FormField("question_company", true, true, true, false, "Northstar Platform Labs", "Current or Last Company Worked"), new FormField("question_title", true, true, true, false, "Principal Backend Engineer", "Current or Last Job Title"), new FormField("question_linkedin", true, true, true, false, "https://example.invalid/jordan-rivera", "LinkedIn Profile"), new FormField("resume", true, true, false, true, "attached", "Resume/CV"));
        if (!new ApplicationValidationGate().validate(profile, attachment, fields, List.of()).valid()) throw new AssertionError("explicit Babylist-pattern answers should validate");
        Files.deleteIfExists(attachment); System.out.println("APPLICANT_ANSWER_RESOLVER_TEST_PASSED");
    }
    private static Properties profile() { Properties p = new Properties(); p.setProperty("legal.name", "Jordan Rivera"); p.setProperty("email", "jordan.rivera@example.invalid"); p.setProperty("phone", "4155550147"); p.setProperty("country", "United States"); p.setProperty("state", "California"); p.setProperty("city", "San Francisco"); p.setProperty("address", "100 Fictional Market Street"); p.setProperty("postal.code", "94105"); p.setProperty("work.authorization.answer", "Yes"); p.setProperty("employment.1.company", "Northstar Platform Labs"); p.setProperty("employment.1.title", "Principal Backend Engineer"); p.setProperty("linkedin.url", "https://example.invalid/jordan-rivera"); return p; }
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); }
}
