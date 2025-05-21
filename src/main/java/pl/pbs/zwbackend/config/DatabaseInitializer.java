package pl.pbs.zwbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository; // Added ProjectRepository

    @Override
    public void run(String... args) {
        boolean usersWereInitialized = false;
        if (userRepository.count() == 0) {
            initUsers();
            usersWereInitialized = true;
        }

        if (projectRepository.count() == 0) {
            if (usersWereInitialized || userRepository.findByEmail("admin@example.com").isPresent()) {
                initProjects();
            } else {
                System.out.println("Admin user not found, skipping project initialization. Ensure users are created first or exist.");
            }
        }
    }

    private void initUsers() {
        List<User> users = Arrays.asList(
                User.builder()
                        .firstName("Admin")
                        .lastName("User")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build(),
                User.builder()
                        .firstName("Normal")
                        .lastName("User")
                        .email("user@example.com")
                        .password(passwordEncoder.encode("user123"))
                        .role(Role.USER)
                        .build(),
                User.builder()
                        .firstName("Dev")
                        .lastName("User")
                        .email("dev@example.com")
                        .password(passwordEncoder.encode("dev123"))
                        .role(Role.USER)
                        .build()
        );

        userRepository.saveAll(users);

        System.out.println("Baza danych została zainicjalizowana testowymi użytkownikami.");
    }

    private void initProjects() {
        User adminUser = userRepository.findByEmail("admin@example.com")
                .orElseThrow(() -> new RuntimeException("Admin user not found for project initialization. This should not happen if users are initialized."));

        Project project1 = Project.builder()
                .name("Projekt Innowacyjny System Zarządzania")
                .description("Rozwój nowego systemu do zarządzania zadaniami i zasobami w firmie, wykorzystujący najnowsze technologie.")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusMonths(8))
                .createdBy(adminUser)
                .build();

        Project project2 = Project.builder()
                .name("Aplikacja Mobilna dla Klientów")
                .description("Stworzenie aplikacji mobilnej na platformy iOS i Android, umożliwiającej klientom łatwy dostęp do usług firmy.")
                .startDate(LocalDate.now().plusMonths(1))
                .endDate(LocalDate.now().plusMonths(10))
                .createdBy(adminUser)
                .build();
        
        User normalUser = userRepository.findByEmail("user@example.com")
                .orElseThrow(() -> new RuntimeException("Normal user not found for project initialization."));

        Project project3 = Project.builder()
                .name("Organizacja Konferencji Branżowej")
                .description("Kompleksowe przygotowanie i przeprowadzenie konferencji dla specjalistów z branży IT.")
                .startDate(LocalDate.now().minusMonths(1)) // Project already started
                .endDate(LocalDate.now().plusMonths(2))
                .createdBy(normalUser)
                .build();

        projectRepository.saveAll(Arrays.asList(project1, project2, project3));
        System.out.println("Zainicjalizowano testowe projekty.");
    }
}