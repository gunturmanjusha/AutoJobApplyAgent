package com.autojobapply.application;
import com.microsoft.playwright.*; import java.net.*; import java.nio.file.*;
/** Keeps employer cookies/local storage on this Mac only; the session directory is Git-ignored. */
public final class BrowserSessionManager implements AutoCloseable {
 private final Playwright playwright=Playwright.create(); private final Browser browser; private final Path root;
 public BrowserSessionManager(Path root,boolean headless)throws java.io.IOException{this.root=root;Files.createDirectories(root);browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));}
 public BrowserContext open(String applicationUrl){Path state=root.resolve(key(applicationUrl)+".json");Browser.NewContextOptions o=new Browser.NewContextOptions();if(Files.isRegularFile(state))o.setStorageStatePath(state);return browser.newContext(o);}
 public void save(String applicationUrl,BrowserContext context){context.storageState(new BrowserContext.StorageStateOptions().setPath(root.resolve(key(applicationUrl)+".json")));}
 private String key(String url){try{return new URI(url).getHost().replaceAll("[^a-zA-Z0-9.-]","_");}catch(Exception e){return "unknown";}}
 public void close(){browser.close();playwright.close();}
}
