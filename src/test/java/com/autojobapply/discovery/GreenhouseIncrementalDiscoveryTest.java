package com.autojobapply.discovery;

import com.autojobapply.config.Config;
import com.autojobapply.matching.JobMatcher;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Resume;
import com.autojobapply.tracking.ApplicationTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/** Proves bounded Greenhouse runs persist a cursor and cover deferred boards in order. */
public final class GreenhouseIncrementalDiscoveryTest {
    public static void main(String[] args) throws Exception {
        String directory = "[{\"platform\":\"greenhouse\",\"token\":\"a\",\"company\":\"A\",\"board_url\":\"u\",\"api_url\":\"u\"},{\"platform\":\"greenhouse\",\"token\":\"b\",\"company\":\"B\",\"board_url\":\"u\",\"api_url\":\"u\"},{\"platform\":\"greenhouse\",\"token\":\"c\",\"company\":\"C\",\"board_url\":\"u\",\"api_url\":\"u\"}]";
        HttpTextClient http = uri -> uri.toString().contains("directory") ? directory : uri.toString().contains("content=false") ? "{\"jobs\":[]}" : "{\"jobs\":[{\"id\":\"1\",\"title\":\"Staff Platform Engineer\",\"location\":{\"name\":\"Remote - US\"},\"content\":\"Java Spring Kafka distributed systems architecture experience cloud\",\"absolute_url\":\"https://example.invalid/job\"}]}";
        Properties properties = new Properties(); properties.setProperty("job.minimum.match.score", "60"); properties.setProperty("job.include.remote", "true"); properties.setProperty("greenhouse.max.boards.per.run", "1");
        Config config = new Config(properties);
        Resume resume = new Resume("staff engineer java spring kafka distributed systems architecture experience cloud", "Test", "", "", Set.of());
        Path database = Files.createTempFile("incremental", ".db");
        try (ApplicationTracker tracker = new ApplicationTracker(database)) {
            tracker.initialize(); GreenhousePipelineService pipeline = new GreenhousePipelineService(new GreenhouseCompanyDiscoveryService(http), new GreenhouseJobDiscoveryService(http), tracker, new JobMatcher(resume, new ApplicantProfile(new Properties(), resume), config), config);
            check("A".equals(pipeline.run().get(0).job().company()), "first bounded run");
            check("B".equals(pipeline.run().get(0).job().company()), "second bounded run");
            check("C".equals(pipeline.run().get(0).job().company()), "third bounded run");
            check("c".equals(tracker.discoveryCursor("greenhouse.directory.token.cursor")), "persisted cursor");
        } finally { Files.deleteIfExists(database); }
        System.out.println("GREENHOUSE_INCREMENTAL_DISCOVERY_TEST_PASSED");
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
