package com.example.gateway;

import com.example.gateway.models.Task;
import com.example.gateway.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(TaskGatewayController.class);

    @GrpcClient("domain")
    private TaskServiceRudGrpc.TaskServiceRudBlockingStub taskServiceStub;

    @Cacheable(value = "tasks", key = "#id")
    @GetMapping("/{id}")
    public com.example.gateway.models.Task getTask(@PathVariable String id) {
        logger.info("Received request to get task with ID: {}", id);
        try {
            GetTaskRequest request = GetTaskRequest.newBuilder().setId(id).build();
            TaskResponse response = taskServiceStub.getTaskById(request);
            logger.info("Successfully retrieved task with ID: {}", id);

            com.example.gateway.models.Task task = new com.example.gateway.models.Task();
            task.setId(response.getTask().getId());
            task.setName(response.getTask().getName());
            task.setStatus(response.getTask().getStatus());
            task.setPriority(response.getTask().getPriority());
            return task;
        } catch (Exception e) {
            logger.error("Failed to retrieve task with ID: {}. Database or service might be down.", id, e);
            throw new RuntimeException("Task not found and database is currently unavailable.");
        }
    }


    @Cacheable(value = "tasks", key = "'allTasks'")
    @GetMapping
    @ResponseBody
    public List<Task> getAllTasks() {
        logger.info("Received request to get all tasks");
        try {
            GetAllTasksRequest request = GetAllTasksRequest.newBuilder().build();
            GetAllTasksResponse response = taskServiceStub.getAllTasks(request);
            logger.info("Successfully retrieved all tasks");

            return response.getTasksList().stream().map(taskProto -> {
                com.example.gateway.models.Task task = new com.example.gateway.models.Task();
                task.setId(taskProto.getId());
                task.setName(taskProto.getName());
                task.setStatus(taskProto.getStatus());
                task.setPriority(taskProto.getPriority());
                return task;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to retrieve all tasks. Database or service might be down.", e);
            throw new RuntimeException("Tasks are not available and the database is currently unavailable.");
        }
    }


    @PostMapping
    public String createTask(@RequestBody com.example.gateway.models.Task task) {
        logger.info("Received request to create a new task: {}", task);
        CreateTaskRequest request = CreateTaskRequest.newBuilder()
                .setName(task.getName())
                .setStatus(task.getStatus())
                .setPriority(task.getPriority())
                .build();
        CreateTaskResponse response = taskServiceStub.createTask(request);
        String message = response.getSuccess() ? "Task creation queued" : "Failed to queue creation";
        logger.info("Task creation result: {}", message);
        return message;
    }

    @PutMapping("/{id}")
    public String updateTask(@PathVariable String id, @RequestBody com.example.gateway.models.Task task) {
        logger.info("Received request to update task with ID: {}, new data: {}", id, task);
        UpdateTaskRequest request = UpdateTaskRequest.newBuilder()
                .setId(id)
                .setName(task.getName())
                .setStatus(task.getStatus())
                .setPriority(task.getPriority())
                .build();
        UpdateTaskResponse response = taskServiceStub.updateTask(request);
        String message = response.getSuccess() ? "Task update queued" : "Failed to queue update";
        logger.info("Task update result for ID {}: {}", id, message);
        return message;
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable String id) {
        logger.info("Received request to delete task with ID: {}", id);
        DeleteTaskRequest request = DeleteTaskRequest.newBuilder().setId(id).build();
        DeleteTaskResponse response = taskServiceStub.deleteTask(request);
        String message = response.getSuccess() ? "Task delete queued" : "Failed to queue deletion";
        logger.info("Task deletion result for ID {}: {}", id, message);
        return message;
    }
}
