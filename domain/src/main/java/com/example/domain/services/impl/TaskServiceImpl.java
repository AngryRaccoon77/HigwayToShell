package com.example.domain.services.impl;

import com.example.domain.dtos.TaskCreateDTO;
import com.example.domain.dtos.TaskDTO;
import com.example.domain.models.Task;
import com.example.domain.repositories.TaskRepository;
import com.example.domain.services.TaskService;
import io.grpc.stub.StreamObserver;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private TaskRepository taskRepository;
    private RabbitTemplate rabbitTemplate;
    private ModelMapper modelMapper;

    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    public void setModelMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    private static final String CREATE_TASK_QUEUE = "create-task-queue";
    private static final String UPDATE_TASK_QUEUE = "update-task-queue";
    private static final String DELETE_TASK_QUEUE = "delete-task-queue";

    @Override
    public Optional<TaskDTO> getTaskById(UUID id) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            TaskDTO taskDTO = modelMapper.map(task.get(), TaskDTO.class);
            return Optional.of(taskDTO);
        } else {
            System.out.println("Task not found for ID: " + id);
            return Optional.empty();
        }
    }

    @Override
    public List<TaskDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {
        Task tempTask = modelMapper.map(taskCreateDTO, Task.class);
        tempTask.setId(null);
        rabbitTemplate.convertAndSend(CREATE_TASK_QUEUE, tempTask); // Uses Jackson2JsonMessageConverter
        return modelMapper.map(tempTask, TaskDTO.class);
    }

    @Override
    public boolean updateTask(UUID id, TaskCreateDTO taskCreateDTO) {
        Task taskData = modelMapper.map(taskCreateDTO, Task.class);
        taskData.setId(id);
        rabbitTemplate.convertAndSend(UPDATE_TASK_QUEUE, taskData);
        return true;
    }

    @Override
    public boolean deleteTask(UUID id) {
        rabbitTemplate.convertAndSend(DELETE_TASK_QUEUE, id);
        return true;
    }
}