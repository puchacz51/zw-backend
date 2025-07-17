package pl.pbs.zwbackend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZwBackendApplication {

    public static void main(String[] args) {
        // Load .env file before starting Spring application
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        // You can optionally iterate and set them as system properties if needed,
        // but Spring Boot should pick them up if dotenv-java sets them correctly.
        // Forcing them into System properties can be more robust:
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(ZwBackendApplication.class, args);
    }

}
