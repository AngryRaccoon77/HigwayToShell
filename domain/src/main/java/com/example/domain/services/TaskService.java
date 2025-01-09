package com.example.domain.services;


import com.example.domain.dtos.TaskCreateDTO;
import com.example.domain.dtos.TaskDTO;
import com.example.domain.models.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskService {
    Optional<TaskDTO> getTaskById(UUID id);
    TaskDTO createTask(TaskCreateDTO taskCreateDTO);
    boolean updateTask(UUID id, TaskCreateDTO taskCreateDTO);
    boolean deleteTask(UUID id);
     List<TaskDTO> getAllTasks();
}