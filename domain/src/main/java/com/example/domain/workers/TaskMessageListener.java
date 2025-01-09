package com.example.domain.workers;



import com.example.domain.models.Task;
import com.example.domain.repositories.TaskRepository;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaskMessageListener {

    private  TaskRepository taskRepository;

    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @RabbitListener(queues = "create-task-queue")
    public void createTask(Task task) {
        taskRepository.save(task);
    }

    @RabbitListener(queues = "update-task-queue")
    public void updateTask(Task task) {
        if (task.getId() != null) {
            taskRepository.save(task);
        }
    }

    @RabbitListener(queues = "delete-task-queue")
    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }
}