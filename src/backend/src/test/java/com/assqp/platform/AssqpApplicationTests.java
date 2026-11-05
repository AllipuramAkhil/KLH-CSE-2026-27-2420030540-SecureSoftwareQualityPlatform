package com.assqp.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AssqpApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context (security, JPA, AI fallback beans) wires up correctly.
    }
}
