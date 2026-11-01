package com.autojobapply.config;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/** Replaces only ignored fictional DRY_RUN data. It never reads or copies a real applicant file. */
public final class CreateSyntheticDryRunAssets {
    private static final Path DIRECTORY = Path.of("private", "dry-run");

    public static void main(String[] args) throws Exception { create(); }

    public static void create() throws Exception {
        Files.createDirectories(DIRECTORY);
        Path profile = DIRECTORY.resolve("applicant.properties");
        Path resume = DIRECTORY.resolve("resume.docx");
        Properties values = new Properties();
        values.setProperty("legal.name", "Jordan Rivera");
        values.setProperty("email", "jordan.rivera@example.invalid");
        values.setProperty("phone", "4155550147");
        values.setProperty("location", "San Francisco, CA");
        values.setProperty("linkedin.url", "https://example.invalid/jordan-rivera");
        values.setProperty("github.url", "https://example.invalid/jordan-rivera");
        values.setProperty("work.authorization", "Synthetic test answer only");
        values.setProperty("visa.sponsorship", "Synthetic test answer only");
        values.setProperty("salary.expectation", "Synthetic test answer only");
        values.setProperty("employment.1.company", "Northstar Platform Labs");
        values.setProperty("employment.1.title", "Principal Backend Engineer");
        try (OutputStream output = Files.newOutputStream(profile)) { values.store(output, "Fictional local DRY_RUN profile. Never use for a real application."); }
        try (XWPFDocument document = new XWPFDocument(); OutputStream output = Files.newOutputStream(resume)) {
            line(document, "Jordan Rivera | jordan.rivera@example.invalid | 415-555-0147 | San Francisco, CA");
            line(document, "SYNTHETIC DRY_RUN RESUME — FICTIONAL DATA — NOT FOR REAL APPLICATIONS");
            line(document, "Principal/Staff Java backend engineer with 12 years building high-scale distributed systems.");
            line(document, "Technical experience: Java 17 and Java 21, Spring Boot, Kafka, microservices, REST APIs, SQL, PostgreSQL, AWS, Kubernetes, cloud architecture, and system design.");
            line(document, "Northstar Platform Labs — Principal Backend Engineer (fictional), 2021-present: designed event-driven Java 21 services, Kafka data pipelines, distributed workflow systems, and cloud-native platform APIs.");
            line(document, "Harbor Signal Systems — Staff Software Engineer (fictional), 2016-2021: led Spring Boot microservices, SQL data modeling, reliability engineering, and system design reviews for multi-region services.");
            line(document, "Impact: improved service reliability, scaled asynchronous processing, mentored engineers, and delivered secure backend platforms. All employers and achievements in this file are fictional test data.");
            document.write(output);
        }
    }

    private static void line(XWPFDocument document, String text) { document.createParagraph().createRun().setText(text); }
}
