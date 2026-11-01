# AutoJobApplyAgent

AutoJobApplyAgent is a personal job-search helper for Staff, Lead, Principal, and architecture software roles in the San Francisco Bay Area and remote US roles.

You give it your resume and verified applicant details once. When you run `Main.java` in IntelliJ, it looks through permitted public job feeds, finds relevant openings, compares each job with your resume, and keeps a record of the best matches.

## What it does today

- Reads a Word (`.docx` or `.doc`) or PDF resume.
- Looks for Staff, Lead, Principal, Architect, Distinguished Engineer, Fellow, and Chief Architect-level software and backend roles; it excludes titles containing “Senior.”
- Focuses on Bay Area roles and remote US opportunities.
- Scores each opening from 0 to 100 based on your resume.
- Selects opportunities scoring 60 or higher.
- Saves job links, match scores, and statuses so the same role is not processed twice.
- Creates a spreadsheet-friendly report at `data/applications.csv` after each run.

## What it does not do yet

The project does not currently submit applications on company websites. It records those roles as unsupported unless an employer provides an authorized submission integration. It does not scrape LinkedIn, automate LinkedIn Easy Apply, bypass CAPTCHA or MFA, or guess answers to screening questions.

## Run it

1. Open this project in IntelliJ and let Maven finish loading.
2. Use Java 23 as the Project SDK.
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
