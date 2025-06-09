package pl.pbs.zwbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.Task;
import pl.pbs.zwbackend.model.enums.ProjectStatus;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.model.enums.TaskStatus;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.TaskRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @Override
    public void run(String... args) {
        boolean usersWereInitialized = false;
        if (userRepository.count() == 0) {
            initUsers();
            usersWereInitialized = true;
        }

        boolean projectsWereInitialized = false;
        if (projectRepository.count() == 0) {
            if (usersWereInitialized || userRepository.findByEmail("admin@example.com").isPresent()) {
                initProjects();
                projectsWereInitialized = true;
            } else {
                System.out.println("Admin user not found, skipping project initialization. Ensure users are created first or exist.");
            }
        }

        if (taskRepository.count() == 0) {
            if (projectsWereInitialized || projectRepository.count() > 0) {
                initTasks();
            } else {
                System.out.println("No projects found, skipping task initialization. Ensure projects are created first or exist.");
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
                .status(ProjectStatus.NOT_STARTED)
                .createdBy(adminUser)
                .build();

        Project project2 = Project.builder()
                .name("Aplikacja Mobilna dla Klientów")
                .description("Stworzenie aplikacji mobilnej na platformy iOS i Android, umożliwiającej klientom łatwy dostęp do usług firmy.")
                .startDate(LocalDate.now().plusMonths(1))
                .endDate(LocalDate.now().plusMonths(10))
                .status(ProjectStatus.UNDER_REVIEW)
                .createdBy(adminUser)
                .build();
        
        User normalUser = userRepository.findByEmail("user@example.com")
                .orElseThrow(() -> new RuntimeException("Normal user not found for project initialization."));

        Project project3 = Project.builder()
                .name("Organizacja Konferencji Branżowej")
                .description("Kompleksowe przygotowanie i przeprowadzenie konferencji dla specjalistów z branży IT.")
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(2))
                .status(ProjectStatus.IN_PROGRESS)
                .createdBy(normalUser)
                .build();

        projectRepository.saveAll(Arrays.asList(project1, project2, project3));
        System.out.println("Zainicjalizowano testowe projekty ze statusami.");
    }

    private void initTasks() {
        User johnDoe = userRepository.findByEmail("user@example.com")
                .orElseThrow(() -> new RuntimeException("User not found for task initialization."));
        User janeDoe = userRepository.findByEmail("dev@example.com")
                .orElseThrow(() -> new RuntimeException("Dev user not found for task initialization."));
        User adminUser = userRepository.findByEmail("admin@example.com")
                .orElseThrow(() -> new RuntimeException("Admin user not found for task initialization."));

        List<Project> projects = projectRepository.findAll();
        if (projects.size() < 2) {
            System.out.println("Not enough projects found for task initialization.");
            return;
        }

        Project project1 = projects.get(0);
        Project project2 = projects.get(1);

        List<Task> tasks = Arrays.asList(
                Task.builder()
                        .name("Design UI Mockups")
                        .description("Create UI mockups for the landing page and dashboard")
                        .status(TaskStatus.COMPLETED)
                        .project(project1)
                        .assignedTo(janeDoe)
                        .dueDate(LocalDate.now().plusDays(7))
                        .build(),
                Task.builder()
                        .name("Implement Authentication")
                        .description("Implement user authentication system with JWT")
                        .status(TaskStatus.IN_PROGRESS)
                        .project(project1)
                        .assignedTo(johnDoe)
                        .dueDate(LocalDate.now().plusDays(14))
                        .build(),
                Task.builder()
                        .name("API Integration")
                        .description("Integrate frontend with backend APIs")
                        .status(TaskStatus.TODO)
                        .project(project1)
                        .assignedTo(johnDoe)
                        .dueDate(LocalDate.now().plusDays(21))
                        .build(),
                Task.builder()
                        .name("Database Design")
                        .description("Design database schema for the project")
                        .status(TaskStatus.COMPLETED)
                        .project(project2)
                        .assignedTo(adminUser)
                        .dueDate(LocalDate.now().plusDays(3))
                        .build(),
                Task.builder()
                        .name("Unit Testing")
                        .description("Write unit tests for core functionality")
                        .status(TaskStatus.IN_PROGRESS)
                        .project(project2)
                        .assignedTo(adminUser)
                        .dueDate(LocalDate.now().plusDays(10))
                        .build()
        );

        taskRepository.saveAll(tasks);
        System.out.println("Zainicjalizowano testowe zadania.");
    }
}