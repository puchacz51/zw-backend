package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.TaskCreateRequest;
import pl.pbs.zwbackend.dto.TaskResponse;
import pl.pbs.zwbackend.dto.TaskUpdateRequest;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.Task;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.TaskRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(TaskCreateRequest taskRequest, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findById(taskRequest.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", taskRequest.getProjectId()));

        User assignedUser = null;
        if (taskRequest.getAssignedTo() != null && !taskRequest.getAssignedTo().isEmpty()) {
            assignedUser = userRepository.findByEmail(taskRequest.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", taskRequest.getAssignedTo()));
        }

        Task task = Task.builder()
                .name(taskRequest.getName())
                .description(taskRequest.getDescription())
                .status(taskRequest.getStatus())
                .dueDate(taskRequest.getDueDate())
                .project(project)
                .assignedTo(assignedUser)
                .build();

        Task savedTask = taskRepository.save(task);
        return convertToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        return convertToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksAssignedToUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return taskRepository.findByAssignedToId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest taskRequest, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Check if user has permission to update task (project owner or assigned user)
        boolean canUpdate = task.getProject().getCreatedBy().getId().equals(currentUser.getId()) ||
                (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUser.getId()));

        if (!canUpdate) {
            throw new UnauthorizedOperationException("User not authorized to update this task");
        }

        User assignedUser = null;
        if (taskRequest.getAssignedTo() != null && !taskRequest.getAssignedTo().isEmpty()) {
            assignedUser = userRepository.findByEmail(taskRequest.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", taskRequest.getAssignedTo()));
        }

        task.setName(taskRequest.getName());
        task.setDescription(taskRequest.getDescription());
        task.setStatus(taskRequest.getStatus());
        task.setDueDate(taskRequest.getDueDate());
        task.setAssignedTo(assignedUser);

        Task updatedTask = taskRepository.save(task);
        return convertToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long taskId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Check if user has permission to delete task (project owner)
        if (!task.getProject().getCreatedBy().getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException("User not authorized to delete this task");
        }

        taskRepository.delete(task);
    }

    private TaskResponse convertToResponse(Task task) {
        UserSummaryResponse assignedToSummary = null;
        if (task.getAssignedTo() != null) {
            User assignedUser = task.getAssignedTo();
            assignedToSummary = UserSummaryResponse.builder()
                    .id(assignedUser.getId())
                    .firstName(assignedUser.getFirstName())
                    .lastName(assignedUser.getLastName())
                    .email(assignedUser.getEmail())
                    .build();
        }

        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .status(task.getStatus())
                .projectId(task.getProject().getId())
                .assignedTo(assignedToSummary)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
