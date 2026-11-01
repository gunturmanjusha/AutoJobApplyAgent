package com.autojobapply;

import com.autojobapply.application.SubmissionService;
import com.autojobapply.config.Config;
import com.autojobapply.discovery.JobDiscovery;
import com.autojobapply.matching.JobMatcher;
import com.autojobapply.model.*;
import com.autojobapply.resume.ResumeParser;
import com.autojobapply.tracking.ApplicationTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.util.*;

/** IntelliJ entry point. It only submits through an explicitly configured, authorized adapter. */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        RunSummary summary = new RunSummary();
        try {
            Config config = Config.load(); Files.createDirectories(config.databasePath().getParent());
            Resume resume = new ResumeParser().parse(config.resumePath());
            ApplicantProfile profile = ApplicantProfile.load(config.profilePath(), resume);
            try (ApplicationTracker tracker = new ApplicationTracker(config.databasePath())) {
                tracker.initialize(); List<Job> jobs = new JobDiscovery(config).discover(); summary.discovered = jobs.size();
                JobMatcher matcher = new JobMatcher(resume, profile, config); List<ScoredJob> eligible = new ArrayList<>();
                for (Job job : jobs) { if (tracker.isKnown(job)) { summary.duplicates++; continue; } ScoredJob scored = matcher.score(job); tracker.record(scored, scored.eligible() ? ApplicationStatus.MATCHED : ApplicationStatus.SKIPPED, null); if (scored.eligible()) eligible.add(scored); }
                eligible.sort(Comparator.comparingInt(ScoredJob::score).reversed()); summary.matched = eligible.size();
                SubmissionService submitter = new SubmissionService(config, profile, tracker);
                for (ScoredJob job : eligible.stream().limit(config.maxApplications()).toList()) summary.count(submitter.process(job));
                tracker.exportCsv(config.csvPath());
            }
        } catch (Exception e) { log.error("Run stopped before completion: {}", e.getMessage(), e); summary.failed++; }
        summary.print();
    }
    private static final class RunSummary { int discovered, matched, duplicates, submitted, blocked, unsupported, failed, attempted;
        void count(ApplicationStatus s) { attempted++; switch (s) { case SUBMITTED -> submitted++; case BLOCKED -> blocked++; case UNSUPPORTED -> unsupported++; case FAILED, UNKNOWN, NEEDS_VERIFICATION -> failed++; default -> { } } }
        void print() { System.out.printf("%n========================================%n       AUTO JOB APPLY AGENT%n========================================%nJobs Discovered:             %d%nJobs Matching >= 60:         %d%nApplications Attempted:      %d%nConfirmed Submitted:         %d%nBlocked / Unsupported:       %d%nFailed / Unverified:         %d%nDuplicates Skipped:          %d%n%nApplication History: data/applications.csv%nDatabase:            data/applications.db%n========================================%n", discovered, matched, attempted, submitted, blocked + unsupported, failed, duplicates); }
    }
}
