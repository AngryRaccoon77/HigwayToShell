package com.example.gateway;

import com.example.gateway.models.Task;
import com.example.gateway.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(TaskGatewayController.class);

    @GrpcClient("domain")
    private TaskServiceRudGrpc.TaskServiceRudBlockingStub taskServiceStub;

    @Cacheable(value = "tasks", key = "#id")
    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) {
        logger.info("Received request to get task with ID: {}", id);
        try {
            GetTaskRequest request = GetTaskRequest.newBuilder().setId(id).build();
            TaskResponse response = taskServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getTaskById(request);

            logger.info("Successfully retrieved task with ID: {}", id);

            Task task = new Task();
            task.setId(response.getTask().getId());
            task.setName(response.getTask().getName());
            task.setStatus(response.getTask().getStatus());
            task.setPriority(response.getTask().getPriority());
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            logger.error("Failed to retrieve task with ID: {}. Database or service might be down.", id, e);
            ErrorResponse errorResponse = new ErrorResponse("Failed to retrieve task with ID: {}. Database or service might be down.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Cacheable(value = "tasks", key = "'allTasks'")
    public List<Task> getAllTasksFromGrpc() {
        logger.info("Retrieving all tasks from gRPC");
        GetAllTasksRequest request = GetAllTasksRequest.newBuilder().build();
        GetAllTasksResponse response = taskServiceStub
                .withDeadlineAfter(3, TimeUnit.SECONDS)
                .getAllTasks(request);

        return response.getTasksList().stream().map(taskProto -> {
            Task task = new Task();
            task.setId(taskProto.getId());
            task.setName(taskProto.getName());
            task.setStatus(taskProto.getStatus());
            task.setPriority(taskProto.getPriority());
            return task;
        }).collect(Collectors.toList());
    }

    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        logger.info("Received request to get all tasks");
        try {
            List<Task> tasks = getAllTasksFromGrpc();
            logger.info("Successfully retrieved all tasks");
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("Failed to retrieve all tasks. Database or service might be down.", e);
            ErrorResponse errorResponse = new ErrorResponse(
                    "Failed to retrieve tasks. Please try again later."
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    @PostMapping
    @CacheEvict(value = "tasks", key = "'allTasks'", allEntries = true)
    public ResponseEntity<String> createTask(@RequestBody Task task) {
        logger.info("Received request to create a new task: {}", task);
        try {
            CreateTaskRequest request = CreateTaskRequest.newBuilder()
                    .setName(task.getName())
                    .setStatus(task.getStatus())
                    .setPriority(task.getPriority())
                    .build();
            CreateTaskResponse response = taskServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .createTask(request);

            if (response.getSuccess()) {
                logger.info("Task creation queued successfully");
                return ResponseEntity.accepted().body("Task creation queued");
            } else {
                logger.warn("Failed to queue task creation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to queue task creation");
            }
        } catch (Exception e) {
            logger.error("Failed to create task.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Service unavailable. Task creation failed.");
        }
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "tasks", key = "#id")
    public ResponseEntity<String> updateTask(@PathVariable String id, @RequestBody Task task) {
        logger.info("Received request to update task with ID: {}, new data: {}", id, task);
        try {
            UpdateTaskRequest request = UpdateTaskRequest.newBuilder()
                    .setId(id)
                    .setName(task.getName())
                    .setStatus(task.getStatus())
                    .setPriority(task.getPriority())
                    .build();
            UpdateTaskResponse response = taskServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .updateTask(request);

            if (response.getSuccess()) {
                logger.info("Task update queued successfully for ID: {}", id);
                return ResponseEntity.accepted().body("Task update queued");
            } else {
                logger.warn("Failed to queue task update for ID: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to queue task update");
            }
        } catch (Exception e) {
            logger.error("Failed to update task with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Service unavailable. Task update failed.");
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "tasks", key = "#id")
    public ResponseEntity<String> deleteTask(@PathVariable String id) {
        logger.info("Received request to delete task with ID: {}", id);
        try {
            DeleteTaskRequest request = DeleteTaskRequest.newBuilder().setId(id).build();
            DeleteTaskResponse response = taskServiceStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .deleteTask(request);

            if (response.getSuccess()) {
                logger.info("Task delete queued successfully for ID: {}", id);
                return ResponseEntity.accepted().body("Task delete queued");
            } else {
                logger.warn("Failed to queue task deletion for ID: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to queue task deletion");
            }
        } catch (Exception e) {
            logger.error("Failed to delete task with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Service unavailable. Task deletion failed.");
        }
    }
}