# AutoJobApplyAgent

A Java 23 Maven application for permitted job discovery, local resume matching, and durable application tracking. The IntelliJ entry point is `com.autojobapply.Main`.

## What it does

- Reads `.docx`, legacy `.doc`, and PDF resumes.
- Loads verified applicant information from `private/applicant.properties`.
- Discovers relevant senior software, backend, and architecture jobs from permitted public feeds.
- Filters for Bay Area roles and remote jobs, then assigns a transparent 0–100 resume-match score.
- Selects jobs at or above the configured threshold (default: 60).
- Persists job URLs, scores, statuses, and duplicate-prevention information in SQLite and exports a CSV report.

## Requirements

- Java 23
- Maven (IntelliJ's bundled Maven is sufficient)
- Internet access for permitted public job-feed discovery

## Run from IntelliJ

1. Open `pom.xml` as a Maven project and set both the Project SDK and Maven importer JDK to Java 23.
2. Reload the Maven project so dependencies are downloaded.
3. Review `src/main/resources/application.properties`.
4. Ensure `job.resume.path` points to your `.docx`, `.doc`, or `.pdf` resume.
5. Review `private/applicant.properties` and complete only facts you have verified.
6. Open `src/main/java/com/autojobapply/Main.java` and click **Run**.

The default resume setting is currently:

```properties
job.resume.path=private/resume.docx
```

## Configuration

`application.properties` controls the minimum score, target location, remote inclusion, discovery/application limits, database and CSV locations, optional Greenhouse board tokens, and browser settings.

`private/applicant.properties.example` is a reusable fictional profile that shows the expected format. The application reads only `private/applicant.properties`. Replace every sample value with verified information, and leave unknown work authorization, sponsorship, compensation, availability, or voluntary-disclosure fields blank.

## Output

After a run, the application creates:

- `data/applications.db` — authoritative SQLite application/job history.
- `data/applications.csv` — CSV report generated from the database.

These files are intentionally ignored by Git because they change across runs and may contain application history.

## Submission boundary

Public job listings and a standard browser login do **not** grant permission to automate applications. The current implementation therefore records a role as `UNSUPPORTED` unless an employer-authorized submission integration has been configured. It does not scrape LinkedIn, automate LinkedIn Easy Apply, bypass CAPTCHA or MFA, or fabricate screening answers.

`SUBMITTED` is reserved for a future employer-authorized adapter that receives reliable submission confirmation. This avoids incorrectly claiming that an application was submitted.

## Privacy

Resume and applicant-profile files are local-only and ignored by Git. Copy the fictional profile template to `private/applicant.properties`, and put your actual resume at `private/resume.docx` (or change the configured path locally). Do not add credentials, API keys, browser sessions, or generated application databases to Git.
