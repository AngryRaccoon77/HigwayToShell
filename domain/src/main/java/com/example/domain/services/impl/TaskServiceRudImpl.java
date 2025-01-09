package com.example.domain.services.impl;

import com.example.domain.dtos.TaskCreateDTO;
import com.example.domain.dtos.TaskDTO;
import com.example.domain.proto.*;
import com.example.domain.services.TaskService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class TaskServiceRudImpl extends TaskServiceRudGrpc.TaskServiceRudImplBase {

    private final TaskService taskService;
    private ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceRudImpl.class);


    @Autowired
    public TaskServiceRudImpl(TaskService taskService) {
        this.taskService = taskService;
    }

    @Autowired
    public void setModelMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public void getTaskById(GetTaskRequest request, StreamObserver<TaskResponse> responseObserver) {
        try {
            UUID taskId = UUID.fromString(request.getId());
            Optional<TaskDTO> taskDTOOptional = taskService.getTaskById(taskId);
            if (taskDTOOptional.isPresent()) {
                TaskDTO taskDTO = taskDTOOptional.get();
                com.example.domain.models.Task domainTask = modelMapper.map(taskDTO, com.example.domain.models.Task.class);
                Task.Builder builder = Task.newBuilder();
                builder.setId(domainTask.getId().toString());
                builder.setName(domainTask.getName());
                builder.setPriority(domainTask.getPriority());
                builder.setStatus(domainTask.getStatus());
                Task taskProto = builder.build();
                TaskResponse response = TaskResponse.newBuilder()
                        .setTask(taskProto)
                        .build();
                responseObserver.onNext(response);
            } else {
                responseObserver.onNext(TaskResponse.newBuilder().build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAllTasks(GetAllTasksRequest request, StreamObserver<GetAllTasksResponse> responseObserver) {
        try {
            List<TaskDTO> taskDTOs = taskService.getAllTasks();
            List<Task> protoTasks = taskDTOs.stream()
                    .map(taskDTO -> {
                        com.example.domain.models.Task domainTask = modelMapper.map(taskDTO, com.example.domain.models.Task.class);
                        return Task.newBuilder()
                                .setId(domainTask.getId().toString())
                                .setName(domainTask.getName())
                                .setStatus(domainTask.getStatus())
                                .setPriority(domainTask.getPriority())
                                .build();
                    })
                    .collect(Collectors.toList());

            GetAllTasksResponse response = GetAllTasksResponse.newBuilder()
                    .addAllTasks(protoTasks)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }


    @Override
    public void createTask(CreateTaskRequest request, StreamObserver<CreateTaskResponse> responseObserver) {
        logger.info("Получен запрос на создание задачи: {}", request);
        try {
            // Создаем доменную модель из запроса
            TaskCreateDTO domainTask = new TaskCreateDTO();
            domainTask.setName(request.getName());
            domainTask.setStatus(request.getStatus());
            domainTask.setPriority(request.getPriority());

            taskService.createTask(domainTask);
            logger.info("Задача успешно создана " );

            CreateTaskResponse response = CreateTaskResponse.newBuilder()
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Ошибка при создании задачи: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateTask(UpdateTaskRequest request, StreamObserver<UpdateTaskResponse> responseObserver) {
        try {
            TaskCreateDTO domainTask = new TaskCreateDTO();
            UUID taskId = UUID.fromString(request.getId());
            domainTask.setName(request.getName());
            domainTask.setStatus(request.getStatus());
            domainTask.setPriority(request.getPriority());

            boolean updated = taskService.updateTask(taskId, domainTask);

            UpdateTaskResponse response = UpdateTaskResponse.newBuilder()
                    .setSuccess(updated)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteTask(DeleteTaskRequest request, StreamObserver<DeleteTaskResponse> responseObserver) {
        try {
            UUID taskId = UUID.fromString(request.getId());
            boolean success = taskService.deleteTask(taskId);
            DeleteTaskResponse response = DeleteTaskResponse.newBuilder()
                    .setSuccess(success)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
