package com.autojobapply.discovery;
import com.google.gson.*; import java.net.*; import java.net.http.*; import java.time.*; import java.util.*;
/** Ingests the published public board directory; Greenhouse API remains the liveness authority. */
public final class GreenhouseCompanyDiscoveryService {
 private static final URI DIRECTORY=URI.create("https://kalebconfer-sys.github.io/job-board-directory/job-board-directory.json"); private final HttpTextClient http; public GreenhouseCompanyDiscoveryService(){this(new JavaHttpTextClient());} public GreenhouseCompanyDiscoveryService(HttpTextClient http){this.http=http;}
 public List<Board> discover() throws Exception {JsonArray rows=get(DIRECTORY).getAsJsonArray();Map<String,Board> unique=new LinkedHashMap<>();for(JsonElement e:rows){JsonObject o=e.getAsJsonObject();if(!"greenhouse".equalsIgnoreCase(s(o,"platform")))continue;String token=s(o,"token");if(!token.isBlank())unique.putIfAbsent(token.toLowerCase(),new Board(s(o,"company"),token,s(o,"board_url"),s(o,"api_url"),"job-board-directory"));}return List.copyOf(unique.values());}
 public boolean validate(Board b){try{return get(URI.create("https://boards-api.greenhouse.io/v1/boards/"+b.token()+"/jobs?content=false")).getAsJsonObject().has("jobs");}catch(Exception e){return false;}}
 private JsonElement get(URI u)throws Exception{return JsonParser.parseString(http.get(u));}private String s(JsonObject o,String k){return o.has(k)&&!o.get(k).isJsonNull()?o.get(k).getAsString():"";}
 public record Board(String company,String token,String boardUrl,String apiUrl,String source){}
}
