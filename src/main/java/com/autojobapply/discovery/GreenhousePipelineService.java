package com.autojobapply.discovery;

import com.autojobapply.config.Config;
import com.autojobapply.matching.JobMatcher;
import com.autojobapply.model.ApplicationStatus;
import com.autojobapply.model.Job;
import com.autojobapply.model.ScoredJob;
import com.autojobapply.tracking.ApplicationTracker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GreenhousePipelineService {
    private static final String CURSOR_KEY = "greenhouse.directory.token.cursor";
    private final GreenhouseCompanyDiscoveryService companies;
    private final GreenhouseJobDiscoveryService jobs;
    private final ApplicationTracker tracker;
    private final JobMatcher matcher;
    private final Config config;
    private GreenhousePipelineReport report = new GreenhousePipelineReport();

    public GreenhousePipelineService(GreenhouseCompanyDiscoveryService companies, GreenhouseJobDiscoveryService jobs,
                                    ApplicationTracker tracker, JobMatcher matcher, Config config) {
        this.companies = companies; this.jobs = jobs; this.tracker = tracker; this.matcher = matcher; this.config = config;
    }
    public GreenhousePipelineReport report() { return report; }

    public List<ScoredJob> run() throws Exception {
        report = new GreenhousePipelineReport();
        report.executionMode(config.executionMode().name());
        report.minimumScore(config.minimumScore());
        report.directoryEntered();
        List<GreenhouseCompanyDiscoveryService.Board> directory;
        try { directory = companies.discover().stream().sorted(Comparator.comparing(b -> b.token().toLowerCase())).toList(); }
        catch (Exception error) { report.directoryFailed(error); throw new IllegalStateException("Greenhouse directory download failed; discovery was not skipped", error); }
        report.directoryCompleted(directory.size());

        String cursor = tracker.discoveryCursor(CURSOR_KEY);
        List<GreenhouseCompanyDiscoveryService.Board> remaining = directory.stream().filter(b -> b.token().compareToIgnoreCase(cursor) > 0).toList();
        boolean wrapped = remaining.isEmpty() && !directory.isEmpty();
        if (wrapped) remaining = directory;
        int limit = config.greenhouseMaxBoards();
        List<GreenhouseCompanyDiscoveryService.Board> selected = remaining.stream().limit(limit).toList();
        List<GreenhouseCompanyDiscoveryService.Board> deferred = remaining.stream().skip(selected.size()).toList();
        String cursorAfter = selected.isEmpty() ? cursor : selected.get(selected.size() - 1).token();
        report.cursor(cursor, cursorAfter);
        report.employersSelected(selected.size(), deferred.size());
        report.nextBoards(deferred);

        List<ScoredJob> eligible = new ArrayList<>();
        for (GreenhouseCompanyDiscoveryService.Board board : selected) {
            GreenhouseCompanyDiscoveryService.Validation validation = companies.validateDetailed(board);
            report.validated(validation.valid(), validation.reason());
            tracker.saveEmployer(board.token(), board.company(), board.boardUrl(), board.apiUrl(), board.source(), validation.valid());
            if (!validation.valid()) continue;
            try {
                List<Job> retrieved = jobs.retrieve(board);
                report.retrieved(retrieved);
                for (Job job : retrieved) {
                    tracker.saveGreenhouseJob(board.token(), job);
                    if (tracker.isKnown(job)) { report.known(); continue; }
                    ScoredJob scored = matcher.score(job);
                    report.scored(scored);
                    tracker.record(scored, scored.eligible() ? ApplicationStatus.MATCHED : ApplicationStatus.SKIPPED, null);
                    if (!scored.eligible()) continue;
                    report.matched(); tracker.enqueue(board.token(), scored); report.queued(); eligible.add(scored);
                }
            } catch (Exception error) { report.retrievalFailed(board.token(), error); }
        }
        if (!selected.isEmpty()) tracker.saveDiscoveryCursor(CURSOR_KEY, cursorAfter);
        if (report.noBoardCouldBeValidatedBecauseOfErrors()) throw new IllegalStateException("Greenhouse board validation failed for every selected board; see diagnostics for HTTP errors");
        if (report.allRetrievalsFailed()) throw new IllegalStateException("Greenhouse job retrieval failed for every validated board; see diagnostics for HTTP errors");
        return eligible;
    }
}
