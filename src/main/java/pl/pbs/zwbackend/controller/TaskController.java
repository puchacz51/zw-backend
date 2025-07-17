package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.TaskCreateRequest;
import pl.pbs.zwbackend.dto.TaskResponse;
import pl.pbs.zwbackend.dto.TaskUpdateRequest;
import pl.pbs.zwbackend.service.TaskService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskCreateRequest taskRequest,
            @AuthenticationPrincipal UserDetails currentUser) {
        TaskResponse taskResponse = taskService.createTask(taskRequest, currentUser.getUsername());
        return new ResponseEntity<>(taskResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse taskResponse = taskService.getTaskById(id);
        return ResponseEntity.ok(taskResponse);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        List<TaskResponse> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable Long projectId) {
        List<TaskResponse> tasks = taskService.getTasksByProject(projectId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal UserDetails currentUser) {
        List<TaskResponse> tasks = taskService.getTasksAssignedToUser(currentUser.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest taskRequest,
            @AuthenticationPrincipal UserDetails currentUser) {
        TaskResponse taskResponse = taskService.updateTask(id, taskRequest, currentUser.getUsername());
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        taskService.deleteTask(id, currentUser.getUsername());
        return ResponseEntity.ok(Map.of("message", "Task successfully deleted"));
    }
}
