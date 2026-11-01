package com.autojobapply.discovery;

import java.util.ArrayList;
import java.util.List;

/** Human-readable evidence for every public Greenhouse discovery stage. */
public final class GreenhousePipelineReport {
    private boolean directoryEntered;
    private boolean directoryCompleted;
    private boolean matchingEntered;
    private String executionMode = "unknown";
    private int employersDiscovered;
    private int employersSelected;
    private int boardsValidated;
    private int boardsValid;
    private int boardsInvalid;
    private int boardsRetrieved;
    private int retrievalFailures;
    private int jobsRetrieved;
    private int knownJobs;
    private int ineligibleJobs;
    private int matchedJobs;
    private int queuedJobs;
    private final List<String> reasons = new ArrayList<>();

    void directoryEntered() { directoryEntered = true; }
    void directoryCompleted(int count) { directoryCompleted = true; employersDiscovered = count; }
    void executionMode(String mode) { executionMode = mode; }
    void employersSelected(int count, int deferred) { employersSelected = count; if (deferred > 0) reason("employer processing: " + deferred + " boards deferred by the 50-board per-run limit"); }
    void validated(boolean valid, String reason) {
        boardsValidated++;
        if (valid) boardsValid++; else { boardsInvalid++; reason("board validation: " + reason); }
    }
    void retrieved(int count) { boardsRetrieved++; jobsRetrieved += count; }
    void retrievalFailed(String token, Exception error) { retrievalFailures++; reason("job retrieval for " + token + ": " + message(error)); }
    void known() { knownJobs++; }
    void matchingEntered() { matchingEntered = true; }
    void ineligible() { ineligibleJobs++; }
    void matched() { matchedJobs++; }
    void queued() { queuedJobs++; }
    void directoryFailed(Exception error) { reason("directory download: " + message(error)); }
    boolean noBoardCouldBeValidatedBecauseOfErrors() { return boardsValidated > 0 && boardsValid == 0 && boardsInvalid > 0; }
    boolean allRetrievalsFailed() { return boardsValid > 0 && boardsRetrieved == 0 && retrievalFailures > 0; }
    public int jobsRetrieved() { return jobsRetrieved; }
    public int matchedJobs() { return matchedJobs; }
    public int queuedJobs() { return queuedJobs; }

    public String render() {
        return "GREENHOUSE DISCOVERY DIAGNOSTICS\n"
                + "  execution mode: " + executionMode + " (does not skip public directory discovery or job retrieval)\n"
                + "  directory download: " + stage(directoryEntered, directoryCompleted) + " (boards=" + employersDiscovered + ")\n"
                + "  employer discovery: " + (directoryCompleted ? "COMPLETED" : "SKIPPED (directory unavailable)") + " (discovered=" + employersDiscovered + ", selected=" + employersSelected + ")\n"
                + "  board validation: " + stage(boardsValidated > 0, true) + " (attempted=" + boardsValidated + ", valid=" + boardsValid + ", invalid=" + boardsInvalid + ")\n"
                + "  job retrieval: " + stage(boardsValid > 0, boardsRetrieved > 0 || retrievalFailures == 0) + " (boards=" + boardsRetrieved + ", jobs=" + jobsRetrieved + ", failures=" + retrievalFailures + ")\n"
                + "  filtering: SKIPPED (no separate Greenhouse prefilter; every retrieved job is deterministically matched)\n"
                + "  matching: " + stage(matchingEntered, true) + " (eligible=" + matchedJobs + ", below-score-or-ineligible=" + ineligibleJobs + ", known=" + knownJobs + ")\n"
                + "  queuing: " + stage(matchingEntered, true) + " (queued=" + queuedJobs + ")"
                + (reasons.isEmpty() ? "" : "\n  skip/failure reasons:\n    - " + String.join("\n    - ", reasons));
    }

    private void reason(String reason) { if (reasons.size() < 10) reasons.add(reason); }
    private static String stage(boolean entered, boolean completed) { return !entered ? "SKIPPED" : completed ? "COMPLETED" : "FAILED"; }
    private static String message(Exception error) { return error.getClass().getSimpleName() + ": " + (error.getMessage() == null ? "no message" : error.getMessage()); }
}
