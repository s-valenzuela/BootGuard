package se.valenzuela.monitoring.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.springframework.boot.test.web.server.LocalServerPort;

public class PlaywrightHooks {

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    Page page;

    private BrowserContext context;

    String baseUrl() {
        return "https://localhost:" + port;
    }

    @Before
    public void openPage() {
        if (playwright == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        }
        context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        page = context.newPage();
        page.setDefaultTimeout(60_000);
    }

    @After
    public void closePage() {
        context.close();
    }
}
