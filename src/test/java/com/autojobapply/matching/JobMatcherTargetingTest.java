package com.autojobapply.matching;

import com.autojobapply.config.Config;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Job;
import com.autojobapply.model.Resume;
import java.util.Properties;
import java.util.Set;

/** Offline proof that targeting rejects the right reason without inflating a score. */
public final class JobMatcherTargetingTest {
    public static void main(String[] args) {
        Properties values = new Properties();
        values.setProperty("job.minimum.match.score", "60"); values.setProperty("job.include.remote", "true");
        Resume resume = new Resume("staff engineer java spring kafka distributed systems architecture experience cloud", "Test", "", "", Set.of());
        JobMatcher matcher = new JobMatcher(resume, new ApplicantProfile(new Properties(), resume), new Config(values));
        check(matcher.score(job("Principal Platform Architect", "Remote - US", "Java Spring Kafka distributed systems architecture experience cloud")).eligible(), "matching Principal remote role must be accepted");
        check(matcher.score(job("Staff Backend Engineer", "San Francisco, CA", "Java Spring Kafka distributed systems architecture experience cloud")).eligible(), "matching Staff Bay Area role must be accepted");
        check(matcher.score(job("Software Engineer", "San Francisco, CA", "Java Spring Kafka distributed systems architecture experience cloud")).rejectionReasons().contains("title/seniority not targeted"), "title rejection");
        check(matcher.score(job("Staff Engineer", "New York, NY", "Java Spring Kafka distributed systems architecture experience cloud")).rejectionReasons().contains("location is not Bay Area or eligible remote"), "location rejection");
        check(matcher.score(job("Staff Engineer", "San Francisco, CA", "Python mobile design")).rejectionReasons().stream().anyMatch(r -> r.startsWith("resume-match score")), "score rejection");
        System.out.println("JOB_MATCHER_TARGETING_TEST_PASSED");
    }
    private static Job job(String title, String location, String description) { return new Job("Greenhouse", "Example", title, location, description, "1", "u", "u"); }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
