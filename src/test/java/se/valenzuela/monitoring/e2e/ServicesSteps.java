package se.valenzuela.monitoring.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import se.valenzuela.monitoring.core.repository.MonitoredServiceRepository;
import se.valenzuela.monitoring.core.service.MonitoringService;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ServicesSteps {

    private final PlaywrightHooks browser;
    private final MonitoringService monitoringService;
    private final MonitoredServiceRepository repository;

    public ServicesSteps(PlaywrightHooks browser, MonitoringService monitoringService,
                          MonitoredServiceRepository repository) {
        this.browser = browser;
        this.monitoringService = monitoringService;
        this.repository = repository;
    }

    @Given("I am on the services page")
    public void iAmOnTheServicesPage() {
        browser.page.navigate(browser.baseUrl() + "/services");
    }

    @Given("a service registered with URL {string}")
    public void aServiceRegisteredWithUrl(String url) {
        monitoringService.addService(url);
    }

    @When("I add a service with URL {string}")
    public void iAddAServiceWithUrl(String url) {
        browser.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add service")).click();
        browser.page.getByLabel("Service URL").fill(url);
        browser.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click();
    }

    @When("I delete the service {string}")
    public void iDeleteTheService(String url) {
        Locator card = browser.page.locator(".service-card").filter(new Locator.FilterOptions().setHasText(url));
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete")).click();
        browser.page.locator("vaadin-confirm-dialog-overlay")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete").setExact(true))
                .click();
    }

    @Then("a service card for {string} should appear on the dashboard")
    public void aServiceCardForShouldAppear(String url) {
        assertThat(browser.page.locator(".service-card-url", new Page.LocatorOptions().setHasText(url))).isVisible();
    }

    @Then("no service card for {string} should appear on the dashboard")
    public void noServiceCardForShouldAppear(String url) {
        assertThat(browser.page.locator(".service-card-url", new Page.LocatorOptions().setHasText(url))).hasCount(0);
    }

    @After
    public void cleanUpServices() {
        repository.deleteAll();
    }
}
