package com.autojobapply.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Proves a protected DRY_RUN cannot silently select a normal applicant path when its fixtures are absent. */
public final class SyntheticDryRunGuardTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("synthetic-dry-run");
        Path profile = root.resolve("synthetic.properties");
        Path resume = root.resolve("synthetic.docx");
        Properties values = new Properties();
        values.setProperty("job.execution.mode", "DRY_RUN");
        values.setProperty("job.synthetic.profile.required", "true");
        values.setProperty("job.applicant.profile.path", profile.toString());
        values.setProperty("job.resume.path", resume.toString());
        Config config = new Config(values);
        boolean rejected = false;
        try { config.validateExecutionInputs(); } catch (IllegalStateException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("missing synthetic assets must stop DRY_RUN");
        Files.createFile(profile);
        Files.createFile(resume);
        config.validateExecutionInputs();
        Files.deleteIfExists(profile); Files.deleteIfExists(resume); Files.deleteIfExists(root);
        System.out.println("SYNTHETIC_DRY_RUN_GUARD_TEST_PASSED");
    }
}
