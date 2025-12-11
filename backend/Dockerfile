# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install netcat for healthcheck
RUN apk add --no-cache netcat-openbsd

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads/reports && \
    chown -R spring:spring /app

USER spring:spring

EXPOSE 9499

ENTRYPOINT ["java", "-jar", "app.jar"]

