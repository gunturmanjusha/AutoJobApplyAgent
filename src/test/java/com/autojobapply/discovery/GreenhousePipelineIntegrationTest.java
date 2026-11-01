package com.autojobapply.discovery;

import com.autojobapply.config.Config;
import com.autojobapply.matching.JobMatcher;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Resume;
import com.autojobapply.tracking.ApplicationTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

/** Exercises production discovery-to-queue orchestration with no network or browser. */
public final class GreenhousePipelineIntegrationTest {
    public static void main(String[] args) throws Exception {
        String directory = "[{\"platform\":\"greenhouse\",\"token\":\"valid\",\"company\":\"Valid Co\",\"board_url\":\"u\",\"api_url\":\"u\"}]";
        String jobs = "{\"jobs\":[{\"id\":\"1\",\"title\":\"Staff Software Engineer\",\"location\":{\"name\":\"San Francisco, CA\"},\"content\":\"Java Spring Kafka distributed systems architecture experience\",\"absolute_url\":\"https://job-boards.greenhouse.io/valid/jobs/1\"}]}";
        HttpTextClient http = uri -> {
            String url = uri.toString();
            return url.contains("directory") ? directory : url.contains("content=false") ? "{\"jobs\":[]}" : jobs;
        };
        Properties properties = new Properties();
        properties.setProperty("job.minimum.match.score", "60");
        properties.setProperty("job.max.discovered.per.run", "50");
        properties.setProperty("job.max.applications.per.run", "1");
        properties.setProperty("job.include.remote", "true");
        Config config = new Config(properties);
        Resume resume = new Resume("java spring kafka distributed systems architecture experience", "A B", "a@b.com", "1",
                Set.of("java", "spring", "kafka", "distributed", "systems", "architecture", "experience"));
        ApplicantProfile profile = new ApplicantProfile(new Properties(), resume);
        Path database = Files.createTempFile("pipeline", ".db");
        try (ApplicationTracker tracker = new ApplicationTracker(database)) {
            tracker.initialize();
            GreenhousePipelineService pipeline = new GreenhousePipelineService(
                    new GreenhouseCompanyDiscoveryService(http), new GreenhouseJobDiscoveryService(http), tracker,
                    new JobMatcher(resume, profile, config), config);

            if (pipeline.run().size() != 1) throw new AssertionError("first run should queue one eligible job");
            String diagnostics = pipeline.report().render();
            if (!diagnostics.contains("directory download: COMPLETED")
                    || !diagnostics.contains("job retrieval: COMPLETED")
                    || !diagnostics.contains("matching: COMPLETED")
                    || !diagnostics.contains("queuing: COMPLETED")) {
                throw new AssertionError("stage diagnostics did not report completed pipeline stages");
            }
            assertCount(database, "greenhouse_employers", 1);
            assertCount(database, "greenhouse_jobs", 1);
            assertCount(database, "application_queue", 1);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE application_queue SET state='NEEDS_MANUAL_ACTION'");
            }

            if (pipeline.run().size() != 0) throw new AssertionError("second run must not requeue a known job");
            assertCount(database, "greenhouse_employers", 1);
            assertCount(database, "greenhouse_jobs", 1);
            assertCount(database, "application_queue", 1);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT state,board_token,greenhouse_job_id FROM application_queue")) {
                if (!result.next() || !"NEEDS_MANUAL_ACTION".equals(result.getString(1))
                        || !"valid".equals(result.getString(2)) || !"1".equals(result.getString(3))) {
                    throw new AssertionError("queue state or stable Greenhouse identity was not preserved");
                }
            }
        } finally {
            Files.deleteIfExists(database);
        }
        System.out.println("GREENHOUSE_PIPELINE_INTEGRATION_TEST_PASSED");
    }

    private static void assertCount(Path database, String table, int expected) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            if (!result.next() || result.getInt(1) != expected) throw new AssertionError(table + " count");
        }
    }
}
