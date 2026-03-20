# Stage 1: Extract layers from the Spring Boot fat jar
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /application
ARG JAR_FILE
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# Stage 2: Runtime image using JRE (smaller than JDK)
FROM eclipse-temurin:25-jre
WORKDIR /application
VOLUME /tmp

# Copy layers in order from least-changed to most-changed
# Docker caches each COPY as a separate layer
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
