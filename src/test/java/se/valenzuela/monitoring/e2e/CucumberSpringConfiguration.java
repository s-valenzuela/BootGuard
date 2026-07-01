package se.valenzuela.monitoring.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "vaadin.launch-browser=false")
@ContextConfiguration(initializers = CucumberSpringConfiguration.MariaDbInitializer.class)
public class CucumberSpringConfiguration {

    /*
     * Cucumber never runs this class through JUnit Jupiter, so the @Testcontainers/@Container
     * extension never fires. The container is started eagerly here instead, and its connection
     * details are pushed into the Spring environment via an ApplicationContextInitializer, which
     * Spring's TestContextManager (used by cucumber-spring) does invoke.
     */
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11")
            .withDatabaseName("bootguard")
            .withUsername("bootguard")
            .withPassword("bootguard");

    static {
        MARIADB.start();
    }

    static class MariaDbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + MARIADB.getJdbcUrl(),
                    "spring.datasource.username=" + MARIADB.getUsername(),
                    "spring.datasource.password=" + MARIADB.getPassword()
            ).applyTo(context.getEnvironment());
        }
    }
}
