package com.autojobapply.discovery;

import com.autojobapply.model.Job;
import com.autojobapply.model.ScoredJob;
import java.util.ArrayList;
import java.util.List;

/** Human-readable, sanitized evidence for every public Greenhouse discovery stage. */
public final class GreenhousePipelineReport {
    private boolean directoryEntered, directoryCompleted, matchingEntered;
    private String executionMode = "unknown", cursorBefore = "start", cursorAfter = "start";
    private int minimumScore;
    private int employersDiscovered, employersSelected, boardsValidated, boardsValid, boardsInvalid, boardsRetrieved, retrievalFailures, jobsRetrieved, blankDescriptions, shortestDescription = Integer.MAX_VALUE, longestDescription, knownJobs, ineligibleJobs, matchedJobs, queuedJobs, titleRejected, locationRejected, scoreRejected, profileRejected;
    private final List<String> reasons = new ArrayList<>();
    private final List<String> samples = new ArrayList<>();
    private final List<String> nextBoards = new ArrayList<>();

    void directoryEntered() { directoryEntered = true; }
    void directoryCompleted(int count) { directoryCompleted = true; employersDiscovered = count; }
    void executionMode(String mode) { executionMode = mode; }
    void minimumScore(int score) { minimumScore = score; }
    void cursor(String before, String after) { cursorBefore = before.isBlank() ? "start" : before; cursorAfter = after.isBlank() ? "start" : after; }
    void employersSelected(int count, int deferred) { employersSelected = count; if (deferred > 0) reason("employer processing: " + deferred + " boards deferred by the configured per-run limit"); }
    void nextBoards(List<GreenhouseCompanyDiscoveryService.Board> boards) { boards.stream().limit(5).forEach(b -> nextBoards.add(b.token())); }
    void validated(boolean valid, String reason) { boardsValidated++; if (valid) boardsValid++; else { boardsInvalid++; reason("board validation: " + reason); } }
    void retrieved(List<Job> jobs) { boardsRetrieved++; jobsRetrieved += jobs.size(); for (Job job : jobs) { int length = job.description().trim().length(); if (length == 0) blankDescriptions++; else { shortestDescription = Math.min(shortestDescription, length); longestDescription = Math.max(longestDescription, length); } } }
    void retrievalFailed(String token, Exception error) { retrievalFailures++; reason("job retrieval for " + token + ": " + message(error)); }
    void known() { knownJobs++; }
    void scored(ScoredJob scored) { matchingEntered = true; if (!scored.eligible()) { ineligibleJobs++; for (String rejection : scored.rejectionReasons()) { if (rejection.startsWith("title/")) titleRejected++; else if (rejection.startsWith("location")) locationRejected++; else if (rejection.startsWith("resume-match")) scoreRejected++; else if (rejection.startsWith("mandatory")) profileRejected++; } } if (samples.size() < 20) samples.add(sample(scored)); }
    void matched() { matchedJobs++; }
    void queued() { queuedJobs++; }
    void directoryFailed(Exception error) { reason("directory download: " + message(error)); }
    boolean noBoardCouldBeValidatedBecauseOfErrors() { return boardsValidated > 0 && boardsValid == 0 && boardsInvalid > 0; }
    boolean allRetrievalsFailed() { return boardsValid > 0 && boardsRetrieved == 0 && retrievalFailures > 0; }
    public int jobsRetrieved() { return jobsRetrieved; }

    public String render() {
        return "GREENHOUSE DISCOVERY DIAGNOSTICS\n"
                + "  execution mode: " + executionMode + " (does not skip public directory discovery or job retrieval)\n"
                + "  directory download: " + stage(directoryEntered, directoryCompleted) + " (boards=" + employersDiscovered + ")\n"
                + "  employer discovery: " + (directoryCompleted ? "COMPLETED" : "SKIPPED (directory unavailable)") + " (cursor=" + cursorBefore + " -> " + cursorAfter + ", selected=" + employersSelected + ", next=" + (nextBoards.isEmpty() ? "none" : String.join(",", nextBoards)) + ")\n"
                + "  board validation: " + stage(boardsValidated > 0, true) + " (attempted=" + boardsValidated + ", valid=" + boardsValid + ", invalid=" + boardsInvalid + ")\n"
                + "  job retrieval: " + stage(boardsValid > 0, boardsRetrieved > 0 || retrievalFailures == 0) + " (boards=" + boardsRetrieved + ", jobs=" + jobsRetrieved + ", blank descriptions=" + blankDescriptions + ", description chars=" + descriptionRange() + ", failures=" + retrievalFailures + ")\n"
                + "  matching: " + stage(matchingEntered, true) + " (threshold=" + minimumScore + ", eligible=" + matchedJobs + ", rejected title/seniority=" + titleRejected + ", location/remote=" + locationRejected + ", resume score=" + scoreRejected + ", profile=" + profileRejected + ", known=" + knownJobs + ")\n"
                + "  queuing: " + stage(matchingEntered, true) + " (queued=" + queuedJobs + ")"
                + (samples.isEmpty() ? "" : "\n  sanitized job samples (title | location | score | outcome):\n    - " + String.join("\n    - ", samples))
                + (reasons.isEmpty() ? "" : "\n  skip/failure reasons:\n    - " + String.join("\n    - ", reasons));
    }

    private static String sample(ScoredJob scored) { return clean(scored.job().title()) + " | " + clean(scored.job().location()) + " | " + scored.score() + " | " + (scored.eligible() ? "eligible" : String.join("; ", scored.rejectionReasons())); }
    private String descriptionRange() { return shortestDescription == Integer.MAX_VALUE ? "n/a" : shortestDescription + "-" + longestDescription; }
    private static String clean(String value) { return value.replaceAll("\\s+", " ").trim(); }
    private void reason(String reason) { if (reasons.size() < 10) reasons.add(reason); }
    private static String stage(boolean entered, boolean completed) { return !entered ? "SKIPPED" : completed ? "COMPLETED" : "FAILED"; }
    private static String message(Exception error) { return error.getClass().getSimpleName() + ": " + (error.getMessage() == null ? "no message" : error.getMessage()); }
}
