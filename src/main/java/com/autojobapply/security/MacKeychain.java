package com.autojobapply.security;
import java.io.*; import java.nio.charset.StandardCharsets;
/** macOS Keychain bridge. Refuses plaintext fallback. */
public final class MacKeychain {
 public void save(String service,String account,String secret)throws IOException,InterruptedException{Process p=new ProcessBuilder("security","add-generic-password","-U","-s",service,"-a",account,"-w",secret).start();if(p.waitFor()!=0)throw new IOException("macOS Keychain rejected credential storage");}
 public String load(String service,String account)throws IOException,InterruptedException{Process p=new ProcessBuilder("security","find-generic-password","-w","-s",service,"-a",account).start();String value=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();if(p.waitFor()!=0)throw new IOException("Credential not found in macOS Keychain");return value;}
}
