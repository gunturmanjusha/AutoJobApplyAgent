package com.autojobapply;

import com.microsoft.playwright.CLI;

/** Run once from IntelliJ to install the Playwright Chromium runtime locally. */
public final class InstallBrowser {
    private InstallBrowser() { }

    public static void main(String[] args) throws Exception {
        CLI.main(new String[]{"install", "chromium"});
    }
}
