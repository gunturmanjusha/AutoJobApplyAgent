package com.autojobapply.email;
import com.autojobapply.security.MacKeychain; import java.nio.file.*;
/** OAuth setup guard: credentials are loaded only from an ignored local client file and tokens belong in Keychain. */
public final class GmailIntegrationService {
 private final Path clientFile; private final MacKeychain keychain=new MacKeychain(); public GmailIntegrationService(Path clientFile){this.clientFile=clientFile;}
 public boolean ready(){return Files.isRegularFile(clientFile);} public void requireClientConfiguration(){if(!ready())throw new IllegalStateException("Gmail OAuth client JSON is missing: "+clientFile+". Configure it locally; no token fallback is used.");}
 public void storeRefreshToken(String dedicatedEmail,String token)throws Exception{keychain.save("AutoJobApplyAgent.Gmail",dedicatedEmail,token);}
 public String loadRefreshToken(String dedicatedEmail)throws Exception{return keychain.load("AutoJobApplyAgent.Gmail",dedicatedEmail);}
}
