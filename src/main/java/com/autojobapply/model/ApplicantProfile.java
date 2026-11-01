package com.autojobapply.model;
import java.io.*; import java.nio.file.*; import java.util.*;
public record ApplicantProfile(Properties values, Resume resume) {
 public static ApplicantProfile load(Path path, Resume resume) throws IOException { Properties p=new Properties(); if(Files.exists(path))try(InputStream in=Files.newInputStream(path)){p.load(in);} return new ApplicantProfile(p,resume); }
 public String get(String key){String v=values.getProperty(key,"").trim(); if(!v.isBlank()) return v; return switch(key){case "email"->resume.email(); case "phone"->resume.phone(); case "legal.name"->resume.name(); default->"";};}
}
