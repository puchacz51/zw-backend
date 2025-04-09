package pl.pbs.zwbackend;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZwBackendApplication {

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUser;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    public static void main(String[] args) {
        SpringApplication.run(ZwBackendApplication.class, args);
    }
}
