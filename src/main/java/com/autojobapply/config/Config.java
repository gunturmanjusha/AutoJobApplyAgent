package com.autojobapply.config;
import java.io.*; import java.nio.file.*; import java.util.*;
public record Config(Properties values) {
  public static Config load() throws IOException { Properties p=new Properties(); try(InputStream in=Config.class.getResourceAsStream("/application.properties")){p.load(in);} Path override=Path.of("application.properties"); if(Files.exists(override))try(InputStream in=Files.newInputStream(override)){p.load(in);} for(String key:List.of("job.execution.mode","job.auto.submit","authorized.submission.enabled","job.max.submissions.per.run","job.max.applications.per.run")){String value=System.getProperty(key);if(value!=null)p.setProperty(key,value);} return new Config(p); }
  private String get(String k){return values.getProperty(k);}
  public Path resumePath(){return Path.of(get("job.resume.path"));} public Path profilePath(){return Path.of(get("job.applicant.profile.path"));} public Path databasePath(){return Path.of(get("job.database.path"));} public Path csvPath(){return Path.of(get("job.report.csv"));}
  public int minimumScore(){return Integer.parseInt(get("job.minimum.match.score"));} public int maxDiscovered(){return Integer.parseInt(get("job.max.discovered.per.run"));} public int maxApplications(){return Integer.parseInt(get("job.max.applications.per.run"));}
  public int maxSubmissions(){return Integer.parseInt(get("job.max.submissions.per.run","1"));}
  public int validationFailureStopThreshold(){return Integer.parseInt(get("job.validation.failure.stop.threshold","1"));}
  public int maximumPostingAgeDays(){return Integer.parseInt(get("job.maximum.posting.age.days","30"));}
  public boolean includeRemote(){return Boolean.parseBoolean(get("job.include.remote"));} public boolean autoSubmit(){return Boolean.parseBoolean(get("job.auto.submit"));} public boolean authorizedSubmission(){return Boolean.parseBoolean(get("authorized.submission.enabled"));} public ExecutionMode executionMode(){return ExecutionMode.from(get("job.execution.mode","DISCOVER_ONLY"));}
  public List<String> greenhouseBoards(){String v=get("greenhouse.board.tokens"); return v==null||v.isBlank()?List.of():Arrays.stream(v.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();}
  private String get(String k,String fallback){return values.getProperty(k,fallback);}
}
