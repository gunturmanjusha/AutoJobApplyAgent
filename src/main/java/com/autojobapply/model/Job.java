package com.autojobapply.model;
public record Job(String source, String company, String title, String location, String description, String jobId, String listingUrl, String applicationUrl) { public String identity(){return (company+"|"+(jobId==null?applicationUrl:jobId)).toLowerCase().replaceAll("\\s+", " ");} }
