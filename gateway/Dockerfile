# Этап сборки
FROM eclipse-temurin:17-jdk-alpine AS builder

# Устанавливаем необходимые зависимости только для сборки
RUN apk add --no-cache \
    protoc \
    libc6-compat \
    bash

WORKDIR /app

# Копируем только файлы, необходимые для разрешения зависимостей
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Скачиваем зависимости (будут закешированы Docker)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN ./gradlew build --no-daemon

# Финальный этап
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем только JAR файл из этапа сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# Создаем непривилегированного пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

# Переключаемся на непривилегированного пользователя
USER appuser

# Настраиваем метаданные контейнера
LABEL maintainer="AngryRaccoon77" \
      version="1.0" \
      description="Java application with gRPC"

# Объявляем порты (замените на ваши реальные порты)
EXPOSE 8080

# Настраиваем параметры JVM для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Точка входа с настройками JVM
ENTRYPOINT ["sh", "-c", "sleep 10 && java $JAVA_OPTS -jar app.jar"]
