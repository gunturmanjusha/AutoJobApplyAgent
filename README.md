# AutoJobApplyAgent

Java 23 Maven application for permitted job discovery, local resume matching, and durable application tracking. Run `com.autojobapply.Main` from IntelliJ.

## First run

1. Put a `.docx`, `.doc`, or `.pdf` resume at `private/resume.docx` (or change `job.resume.path`).
2. Copy `private/applicant.properties.example` to `private/applicant.properties`; provide only verified values.
3. Run `Main.java`. Results are in `data/applications.db` and `data/applications.csv`.

Discovery uses Remotive's public software jobs feed and optionally public Greenhouse board feeds configured in `application.properties`. It filters for senior backend/architecture roles in the Bay Area or remote jobs, scores each job deterministically, and retains the exact source/application URL.

## Submission boundary

Public listings and a normal browser login do **not** confer permission to automate applications. Therefore this project intentionally marks a role `UNSUPPORTED` unless a specific employer-authorized submission API/adapter is configured. It neither scrapes LinkedIn nor automates LinkedIn Easy Apply, and it never bypasses CAPTCHA, MFA, or screening questions. `SUBMITTED` is reserved for a future authorized adapter that obtains verifiable confirmation.

Private profiles, resumes, databases, sessions, and secrets are ignored by Git.
