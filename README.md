# AutoJobApplyAgent

AutoJobApplyAgent is a personal job-search helper for Staff, Lead, Principal, and architecture software roles in the San Francisco Bay Area and remote US roles.

You give it your resume and verified applicant details once. When you run `Main.java` in IntelliJ, it looks through permitted public job feeds, finds relevant openings, compares each job with your resume, and keeps a record of the best matches.

## What you can do today

- Reads a Word (`.docx` or `.doc`) or PDF resume.
- Looks for Staff, Lead, Principal, Architect, Distinguished Engineer, Fellow, and Chief Architect-level software and backend roles. This includes titles such as “Senior Principal Engineer” but excludes Senior-only titles such as “Senior Software Engineer.”
- Focuses on Bay Area roles and remote US opportunities.
- Scores each opening from 0 to 100 based on your resume.
- Selects opportunities scoring 60 or higher.
- Saves job links, match scores, and statuses so the same role is not processed twice.
- Creates a spreadsheet-friendly report at `data/applications.csv` after each run.
- Keeps duplicate-prevention history in `data/applications.db`.
- Supports local persistent employer browser sessions. After you sign in to an employer site in a visible browser, its saved session can be reused while it remains valid.

## What happens when you run `Main.java`

1. It reads your resume and applicant profile.
2. It searches permitted public job feeds for recent Bay Area and remote roles.
3. It filters and scores each job against your resume.
4. It records eligible matches and skips duplicates.
5. It writes the CSV report and SQLite history to the local `data/` folder.

## What is still being built

The project does not yet contain complete employer-specific form adapters, so it does not currently fill or submit company applications. It records these as unsupported until a documented, permitted application path is implemented.

It does not scrape LinkedIn, automate LinkedIn Easy Apply, bypass CAPTCHA or MFA, or guess screening answers.

## Run it

1. Open this project in IntelliJ and let Maven finish loading.
2. Use Java 21 as the Project SDK.
3. Put your resume at `private/resume.docx`, or update `job.resume.path` in `src/main/resources/application.properties`.
4. Copy `private/applicant.properties.example` to `private/applicant.properties` and replace the fictional sample details with your own verified information.
5. Open `src/main/java/com/autojobapply/Main.java` and click the green **Run** button.

At the end of the run, look at:

- `data/applications.csv` for an easy-to-read job report.
- `data/applications.db` for the complete local history and duplicate prevention.

## Files you may want to change

- `src/main/resources/application.properties` — score threshold, location, remote preference, and run limits.
- `private/applicant.properties` — your contact details and verified application answers.
- `private/applicant.properties.example` — a fictional example showing the expected format.

## Keep personal files private

Your real resume and `private/applicant.properties` are meant to stay on your computer. Do not put them, passwords, API keys, browser sessions, or generated application reports in a public repository.

## Current status

| Capability | Status |
| --- | --- |
| Resume reading, job discovery, matching, SQLite, and CSV reports | Working |
| Persistent local employer browser sessions | Working |
| Gmail OAuth security foundation | Needs your local Google OAuth client configuration |
| Employer form filling and confirmed submission | Not yet implemented per ATS platform |
| LinkedIn automation | Not part of Phase 1 |

### Greenhouse validation status

The first Greenhouse adapter is compiled but has **not** been validated against a live employer form in this environment. The local Playwright Chromium runtime is not installed, so no real employer pages were opened, no profile fields were filled, and no resume was attached. Production submission remains disabled. The adapter now requires both a post-submit page transition and a specific application-success component before it can record `SUBMITTED`; generic page text alone is not sufficient.
