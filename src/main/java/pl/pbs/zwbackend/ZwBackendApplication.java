package pl.pbs.zwbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class ZwBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(ZwBackendApplication.class, args);
    }
}
