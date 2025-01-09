package com.example.domain.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;


@Configuration
public class RabbitMQConfig {

    private static final String CREATE_TASK_QUEUE = "create-task-queue";
    private static final String UPDATE_TASK_QUEUE = "update-task-queue";
    private static final String DELETE_TASK_QUEUE = "delete-task-queue";
    private static final String EXCHANGE_NAME = "taskExchange";

    @Bean
    public Queue createTaskQueue() {
        return new Queue(CREATE_TASK_QUEUE, true);
    }

    @Bean
    public Queue updateTaskQueue() {
        return new Queue(UPDATE_TASK_QUEUE, true);
    }

    @Bean
    public Queue deleteTaskQueue() {
        return new Queue(DELETE_TASK_QUEUE, true);
    }

    @Bean
    public Exchange taskExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding bindingCreateTaskQueue(Queue createTaskQueue, Exchange taskExchange) {
        return BindingBuilder.bind(createTaskQueue).to(taskExchange).with("task.create").noargs();
    }

    @Bean
    public Binding bindingUpdateTaskQueue(Queue updateTaskQueue, Exchange taskExchange) {
        return BindingBuilder.bind(updateTaskQueue).to(taskExchange).with("task.update").noargs();
    }

    @Bean
    public Binding bindingDeleteTaskQueue(Queue deleteTaskQueue, Exchange taskExchange) {
        return BindingBuilder.bind(deleteTaskQueue).to(taskExchange).with("task.delete").noargs();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Optional: pretty printing
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // Ignore unknown properties

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}