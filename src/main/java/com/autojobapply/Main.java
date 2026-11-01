package com.autojobapply;

import com.autojobapply.application.SubmissionService;
import com.autojobapply.config.Config;
import com.autojobapply.config.ExecutionMode;
import com.autojobapply.discovery.JobDiscovery;
import com.autojobapply.discovery.GreenhouseCompanyDiscoveryService;
import com.autojobapply.discovery.GreenhouseJobDiscoveryService;
import com.autojobapply.discovery.GreenhousePipelineService;
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
            Config config = Config.load(); config.validateExecutionInputs(); Files.createDirectories(config.databasePath().getParent());
            Resume resume = new ResumeParser().parse(config.resumePath());
            ApplicantProfile profile = ApplicantProfile.load(config.profilePath(), resume);
            if (config.executionMode() == ExecutionMode.DRY_RUN) System.out.printf("DRY_RUN synthetic assets verified: resume=%s, parsed characters=%d, parsed terms=%d, profile=%s%n", config.resumePath(), resume.text().length(), resume.terms().size(), config.profilePath());
            try (ApplicationTracker tracker = new ApplicationTracker(config.databasePath())) {
                tracker.initialize(); List<ScoredJob> eligible = new ArrayList<>(); String queuedIdentity = System.getProperty("job.queue.identity", "").trim();
                if (!queuedIdentity.isBlank()) {
                    ScoredJob queued = tracker.queuedJob(queuedIdentity).orElseThrow(() -> new IllegalArgumentException("READY_TO_APPLY queue entry not found: " + queuedIdentity));
                    eligible.add(queued); summary.discovered = 1; System.out.println("Using existing queued job only: " + queued.job().identity());
                } else {
                    List<Job> jobs = new ArrayList<>(new JobDiscovery(config).discover()); summary.discovered = jobs.size();
                    JobMatcher matcher = new JobMatcher(resume, profile, config);
                    for (Job job : jobs) { if (tracker.isKnown(job)) { summary.duplicates++; continue; } ScoredJob scored = matcher.score(job); tracker.record(scored, scored.eligible() ? ApplicationStatus.MATCHED : ApplicationStatus.SKIPPED, null); if (scored.eligible()) { tracker.enqueue(scored); eligible.add(scored); } }
                    GreenhousePipelineService greenhouse = new GreenhousePipelineService(new GreenhouseCompanyDiscoveryService(),new GreenhouseJobDiscoveryService(),tracker,matcher,config);
                    try { eligible.addAll(greenhouse.run()); }
                    finally { System.out.println(greenhouse.report().render()); }
                    summary.discovered += greenhouse.report().jobsRetrieved();
                }
                eligible.sort(Comparator.comparingInt(ScoredJob::score).reversed()); summary.matched = eligible.size();
                if (config.executionMode() != ExecutionMode.DISCOVER_ONLY) {
                    SubmissionService submitter = new SubmissionService(config, profile, tracker);
                    int limit = config.executionMode() == ExecutionMode.LIVE ? config.maxSubmissions() : config.maxApplications();
                    int validationFailures = 0;
                    for (ScoredJob job : eligible.stream().limit(limit).toList()) {
                        ApplicationStatus result = submitter.process(job);
                        summary.count(result);
                        if (config.executionMode() == ExecutionMode.LIVE && (result == ApplicationStatus.NEEDS_MANUAL_ACTION
                                || result == ApplicationStatus.NEEDS_VERIFICATION || result == ApplicationStatus.FAILED)) {
                            validationFailures++;
                            if (validationFailures >= config.validationFailureStopThreshold()) break;
                        }
                    }
                }
                tracker.exportCsv(config.csvPath());
            }
        } catch (Exception e) { System.err.println("AUTO JOB APPLY AGENT FAILED: " + e.getMessage()); e.printStackTrace(System.err); log.error("Run stopped before completion: {}", e.getMessage(), e); summary.failed++; }
        summary.print();
    }
    private static final class RunSummary { int discovered, matched, duplicates, submitted, blocked, unsupported, failed, attempted;
        void count(ApplicationStatus s) { attempted++; switch (s) { case SUBMITTED -> submitted++; case BLOCKED -> blocked++; case UNSUPPORTED -> unsupported++; case FAILED, UNKNOWN, NEEDS_VERIFICATION -> failed++; default -> { } } }
        void print() { System.out.printf("%n========================================%n       AUTO JOB APPLY AGENT%n========================================%nJobs Discovered:             %d%nJobs Matching >= 60:         %d%nApplications Attempted:      %d%nConfirmed Submitted:         %d%nBlocked / Unsupported:       %d%nFailed / Unverified:         %d%nDuplicates Skipped:          %d%n%nApplication History: data/applications.csv%nDatabase:            data/applications.db%n========================================%n", discovered, matched, attempted, submitted, blocked + unsupported, failed, duplicates); }
    }
}
