# AutoJobApplyAgent

Java 21 IntelliJ application for high-volume, legitimate job applications. Its future objective is hundreds of relevant confirmed applications from one Run action; this is a product goal, not a claim of current results.

## Workflow

`Main.java → ATS company discovery → job retrieval → filtering → resume matching → persistent queue → ATS adapter → verified submission → SQLite/CSV`

The system must not require manual company lists, board tokens, or job URLs. A discovered job is never a submitted application.

## Targeting

Principal, Staff, Senior Staff, Lead, Architect, and equivalent backend/platform roles; Java, Spring Boot, Kafka, cloud, and distributed systems; Bay Area and eligible remote-US jobs; configurable 60% minimum score; mandatory eligibility overrides score.

## ATS roadmap

1. Greenhouse: public board discovery, validation, job retrieval, matching, queueing, authorized form submission, confirmation.
2. Oracle Fusion Cloud Recruiting.
3. Workday.
4. Lever.

Only Greenhouse is in scope now.

## Current verified status

| Capability | Status |
| --- | --- |
| Resume parsing, applicant profile, matching, SQLite/CSV, duplicate checks | Implemented; compiled |
| Public Remotive and configured Greenhouse job retrieval | Implemented |
| Persistent browser sessions | Implemented; browser execution unverified |
| Greenhouse form adapter | Implemented but unverified against real forms; submission disabled |
| Automatic Greenhouse board-directory ingestion and persistent queue | In progress |
| Confirmed employer submissions | Not implemented or verified |
| Gmail, LinkedIn, Oracle Fusion, Workday, Lever | Not in this phase |

### Playwright status

Commit `f6ba356` established that Codex stalls in `Playwright.create()` before browser launch. Chromium is installed, but form validation must run in IntelliJ outside this environment. No real form, resume upload, or employer submission has been claimed as tested.

## Greenhouse Step 2

Greenhouse has no global board API. The next implementation ingests a legitimate public directory, validates tokens through `boards-api.greenhouse.io/v1/boards/{token}/jobs?content=true`, persists valid employers, refreshes open jobs, filters/matches them, and queues eligible work. Coverage is measured, never treated as universal.

## Run locally

1. Open `pom.xml` with Java 21 in IntelliJ.
2. Keep the resume at `private/resume.docx` or configure `job.resume.path`.
3. Copy `private/applicant.properties.example` to ignored `private/applicant.properties`; use verified answers only.
4. Run `com.autojobapply.Main`.

`data/applications.db` is authoritative; `data/applications.csv` is its report. Keep `authorized.submission.enabled=false` until a platform adapter is validated. `SUBMITTED` requires genuine employer confirmation.

## Reliability and safety

The scalability objective is thousands of boards and hundreds of applications per run. Use bounded concurrency, rate limits, retries, checkpoints, resume-safe recovery, and duplicate prevention. Never invent answers, bypass CAPTCHA/access controls, or commit resumes, profiles, sessions, tokens, or runtime data to public Git.

## Development rules

Read this README before architectural changes; preserve the one-click confirmed-application objective; reuse working code; do not reorder ATS phases without instruction; update status only with verified evidence; keep this README synchronized with architecture changes.
