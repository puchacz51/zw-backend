package pl.pbs.zwbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            initUsers();
        }
    }

    private void initUsers() {
        List<User> users = Arrays.asList(
                User.builder()
                        .username("admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build(),
                User.builder()
                        .username("user")
                        .email("user@example.com")
                        .password(passwordEncoder.encode("user123"))
                        .role(Role.USER)
                        .build(),
                User.builder()
                        .username("developer")
                        .email("dev@example.com")
                        .password(passwordEncoder.encode("dev123"))
                        .role(Role.USER)
                        .build()
        );

        userRepository.saveAll(users);

        System.out.println("Baza danych została zainicjalizowana testowymi użytkownikami.");
    }
}