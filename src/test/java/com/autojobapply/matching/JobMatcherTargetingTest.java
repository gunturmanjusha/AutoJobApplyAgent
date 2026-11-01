package com.autojobapply.matching;

import com.autojobapply.config.Config;
import com.autojobapply.model.ApplicantProfile;
import com.autojobapply.model.Job;
import com.autojobapply.model.Resume;
import java.util.Properties;
import java.util.Set;

/** Offline proof that targeting rejects the right reason without inflating a score. */
public final class JobMatcherTargetingTest {
    public static void main(String[] args) {
        Properties values = new Properties();
        values.setProperty("job.minimum.match.score", "60"); values.setProperty("job.include.remote", "true");
        Resume resume = new Resume("staff engineer java spring kafka distributed systems architecture experience cloud", "Test", "", "", Set.of());
        JobMatcher matcher = new JobMatcher(resume, new ApplicantProfile(new Properties(), resume), new Config(values));
        check(matcher.score(job("Principal Platform Architect", "Remote - US", "Java Spring Kafka distributed systems architecture experience cloud")).eligible(), "matching Principal remote role must be accepted");
        check(matcher.score(job("Staff Backend Engineer", "San Francisco, CA", "Java Spring Kafka distributed systems architecture experience cloud")).eligible(), "matching Staff Bay Area role must be accepted");
        check(matcher.score(job("Software Engineer", "San Francisco, CA", "Java Spring Kafka distributed systems architecture experience cloud")).rejectionReasons().contains("title/seniority not targeted"), "title rejection");
        check(matcher.score(job("Staff Engineer", "New York, NY", "Java Spring Kafka distributed systems architecture experience cloud")).rejectionReasons().contains("location is not Bay Area or eligible remote"), "location rejection");
        check(matcher.score(job("Staff Engineer", "Montréal, Quebec, Canada", "Java Spring Kafka distributed systems architecture experience cloud")).rejectionReasons().contains("location is not Bay Area or eligible remote"), "Canada must not match California abbreviation");
        check(matcher.score(job("Staff Engineer", "San Francisco, CA", "Python mobile design")).rejectionReasons().stream().anyMatch(r -> r.startsWith("resume-match score")), "score rejection");
        var salesforceAdmin = matcher.score(job("Salesforce Solution Architect", "San Francisco, CA", "5 years experience as a Salesforce administrator manages Sales Cloud, Service Cloud, Apex, Visualforce, Lightning, SOQL, certified platform administrator, Process Builder, Data Loader, and Salesforce environments"));
        check(salesforceAdmin.score() == 100, "generic score inflation is reproduced");
        check(salesforceAdmin.rejectionReasons().contains("incompatible specialized platform requirements: salesforce not evidenced by resume/profile"), "Salesforce administration must not match Java profile");
        check(matcher.score(job("Salesforce Integration Architect", "Remote - US", "Salesforce integration team needs Java, Spring Boot, Kafka, distributed systems, microservices, cloud architecture, REST APIs, SQL, and system design. Salesforce is one integration endpoint.")).eligible(), "platform title alone must not reject a substantive Java backend role");
        check(matcher.score(job("SAP Architect", "San Francisco, CA", "SAP S/4HANA ABAP SAP Basis SuccessFactors SAP FICO SAP MM SAP SD implementation and certification experience")).rejectionReasons().contains("incompatible specialized platform requirements: sap not evidenced by resume/profile"), "SAP specialization rejection");
        check(matcher.score(job("ServiceNow Architect", "Remote - US", "ServiceNow administrator ServiceNow developer ServiceNow CMDB ServiceNow ITSM workflows and platform implementation")).rejectionReasons().contains("incompatible specialized platform requirements: servicenow not evidenced by resume/profile"), "ServiceNow specialization rejection");
        System.out.println("JOB_MATCHER_TARGETING_TEST_PASSED");
    }
    private static Job job(String title, String location, String description) { return new Job("Greenhouse", "Example", title, location, description, "1", "u", "u"); }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
