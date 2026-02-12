package se.valenzuela.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "integration")
class MonitoringApplicationTests {

	@Test
	void contextLoads() {
	}

}
