package com.autojobapply.application;
import com.autojobapply.model.*; import com.microsoft.playwright.*; import java.nio.file.*;
/** Concrete adapter for direct Greenhouse application forms; never treats a filled form as submitted. */
public final class GreenhouseApplicationAdapter {
 public ApplicationStatus apply(ScoredJob scored, ApplicantProfile profile, Path resume, boolean submit) {
  if(!scored.job().applicationUrl().contains("greenhouse.io")) return ApplicationStatus.UNSUPPORTED;
  try(Playwright pw=Playwright.create(); Browser browser=pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!submit)); BrowserContext context=browser.newContext()){
   Page p=context.newPage();p.navigate(scored.job().applicationUrl());
   if(p.locator("input[type=password], text=Sign in, text=Log in").count()>0)return ApplicationStatus.LOGIN_REQUIRED;
   Locator form=p.locator("form").first(); if(form.count()==0)return ApplicationStatus.UNSUPPORTED;
   fill(form,"input[name*=first_name],input[id*=first_name]",first(profile.get("legal.name"))); fill(form,"input[name*=last_name],input[id*=last_name]",last(profile.get("legal.name")));
   fill(form,"input[type=email],input[name*=email]",profile.get("email"));fill(form,"input[type=tel],input[name*=phone]",profile.get("phone"));fill(form,"input[name*=location]",profile.get("location"));
   Locator upload=form.locator("input[type=file]").first();if(upload.count()>0)upload.setInputFiles(resume);
   if(!submit)return ApplicationStatus.APPLYING;
   Locator button=form.locator("button[type=submit],input[type=submit]").first();if(button.count()==0)return ApplicationStatus.BLOCKED;String before=p.url();button.click();p.waitForTimeout(1500);
   boolean transitioned=!before.equals(p.url());boolean success=p.locator("[data-qa*=success i],.application-confirmation,#application_confirmation,.thank-you").count()>0;return transitioned&&success?ApplicationStatus.SUBMITTED:ApplicationStatus.NEEDS_VERIFICATION;
  }catch(Exception e){return ApplicationStatus.FAILED;}
 }
 private void fill(Locator root,String selector,String value){if(value==null||value.isBlank())return;Locator l=root.locator(selector).first();if(l.count()>0)l.fill(value);}private String first(String n){return n==null?"":n.split(" ")[0];}private String last(String n){String[]x=n==null?new String[0]:n.split(" ");return x.length>1?x[x.length-1]:"";}
}
