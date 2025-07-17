package pl.pbs.zwbackend.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest
@ActiveProfiles("test")
class ApplicationIntegrationTest {
    @Test
    void contextLoads() {
        // Test that the application context loads successfully
        // This is a smoke test to ensure all beans are properly configured
    }
}
