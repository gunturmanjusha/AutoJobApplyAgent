package com.autojobapply.config;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/** Creates ignored fictional DRY_RUN data. It never reads or copies a real applicant file. */
public final class CreateSyntheticDryRunAssets {
    private static final Path DIRECTORY = Path.of("private", "dry-run");

    public static void main(String[] args) throws Exception { create(); }

    public static void create() throws Exception {
        Files.createDirectories(DIRECTORY);
        Path profile = DIRECTORY.resolve("applicant.properties");
        Path resume = DIRECTORY.resolve("resume.docx");
        if (!Files.exists(profile)) {
            Properties values = new Properties();
            values.setProperty("legal.name", "Avery Testperson");
            values.setProperty("email", "avery.testperson@example.invalid");
            values.setProperty("phone", "4155550100");
            values.setProperty("location", "San Francisco, CA");
            values.setProperty("linkedin.url", "https://example.invalid/avery-testperson");
            values.setProperty("github.url", "https://example.invalid/avery-testperson");
            values.setProperty("work.authorization", "Synthetic test answer only");
            values.setProperty("visa.sponsorship", "Synthetic test answer only");
            values.setProperty("salary.expectation", "Synthetic test answer only");
            values.setProperty("employment.1.company", "Example Systems LLC");
            values.setProperty("employment.1.title", "Staff Test Engineer");
            try (OutputStream output = Files.newOutputStream(profile)) { values.store(output, "Fictional local DRY_RUN profile. Never use for a real application."); }
        }
        if (!Files.exists(resume)) {
            try (XWPFDocument document = new XWPFDocument(); OutputStream output = Files.newOutputStream(resume)) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText("Avery Testperson | avery.testperson@example.invalid | 415-555-0100");
                document.createParagraph().createRun().setText("SYNTHETIC DRY_RUN RESUME — NOT FOR REAL APPLICATIONS");
                document.createParagraph().createRun().setText("Example Systems LLC — Staff Test Engineer — fictional employment history.");
                document.write(output);
            }
        }
    }
}
