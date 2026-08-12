package com.competition.hub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-secret-for-context-loads-at-least-32-bytes")
class CompetitionHubBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
