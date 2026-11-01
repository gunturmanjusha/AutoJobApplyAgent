package com.autojobapply.application;

import com.autojobapply.config.Config;
import com.autojobapply.config.ExecutionMode;
import com.autojobapply.discovery.GreenhouseCompanyDiscoveryService;
import com.autojobapply.discovery.GreenhouseJobDiscoveryService;
import com.autojobapply.discovery.GreenhousePipelineService;
import com.autojobapply.discovery.HttpTextClient;
import com.autojobapply.matching.JobMatcher;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Resume;
import com.autojobapply.tracking.ApplicationTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Controlled end-to-end DRY_RUN evidence: match, queue, inspect required form fields, never submit. */
public final class DryRunQueueAndFormInspectionTest {
    public static void main(String[] args) throws Exception {
        String directory = "[{\"platform\":\"greenhouse\",\"token\":\"fictional\",\"company\":\"Fictional Platform\",\"board_url\":\"u\",\"api_url\":\"u\"}]";
        String jobs = "{\"jobs\":[{\"id\":\"1\",\"title\":\"Principal Java Platform Architect\",\"location\":{\"name\":\"San Francisco, CA\"},\"content\":\"Java 21 Spring Boot Kafka distributed systems microservices cloud SQL system design experience\",\"absolute_url\":\"https://job-boards.greenhouse.io/fictional/jobs/1\"}]}";
        HttpTextClient http = uri -> uri.toString().contains("directory") ? directory : uri.toString().contains("content=false") ? "{\"jobs\":[]}" : jobs;
        Properties properties = new Properties();
        properties.setProperty("job.minimum.match.score", "60"); properties.setProperty("job.include.remote", "true"); properties.setProperty("job.execution.mode", "DRY_RUN"); properties.setProperty("greenhouse.max.boards.per.run", "1");
        Resume resume = new Resume("principal staff java 17 java 21 spring boot kafka distributed systems microservices cloud sql system design experience", "Jordan Rivera", "jordan.rivera@example.invalid", "4155550147", Set.of());
        Properties profileValues = new Properties(); profileValues.setProperty("legal.name", "Jordan Rivera"); profileValues.setProperty("email", "jordan.rivera@example.invalid"); profileValues.setProperty("phone", "4155550147"); profileValues.setProperty("location", "San Francisco, CA"); profileValues.setProperty("work.authorization", "Synthetic test answer only");
        ApplicantProfile profile = new ApplicantProfile(profileValues, resume);
        Path database = Files.createTempFile("dry-run-queue", ".db"); Path attachment = Files.createTempFile("synthetic-resume", ".docx");
        try (ApplicationTracker tracker = new ApplicationTracker(database)) {
            tracker.initialize();
            GreenhousePipelineService pipeline = new GreenhousePipelineService(new GreenhouseCompanyDiscoveryService(http), new GreenhouseJobDiscoveryService(http), tracker, new JobMatcher(resume, profile, new Config(properties)), new Config(properties));
            if (pipeline.run().size() != 1) throw new AssertionError("relevant synthetic job did not reach queue");
            FormValidationResult inspection = new ApplicationValidationGate().validate(profile, attachment, List.of(
                    new FormField("first_name", true, true, false, false, "Jordan"), new FormField("last_name", true, true, false, false, "Rivera"),
                    new FormField("email", true, true, false, false, "jordan.rivera@example.invalid"), new FormField("phone", true, true, false, false, "4155550147"),
                    new FormField("location", true, true, false, false, "San Francisco, CA"), new FormField("resume", true, true, false, true, "attached")), List.of());
            if (!inspection.valid()) throw new AssertionError("adapter form inspection should validate fixture");
            AtomicInteger clicks = new AtomicInteger();
            if (new SubmissionSafetyGate().submitIfAllowed(ExecutionMode.DRY_RUN, true, true, inspection, clicks::incrementAndGet) || clicks.get() != 0)
                throw new AssertionError("DRY_RUN must never click submit");
        } finally { Files.deleteIfExists(database); Files.deleteIfExists(attachment); }
        System.out.println("DRY_RUN_QUEUE_AND_FORM_INSPECTION_TEST_PASSED");
    }
}
