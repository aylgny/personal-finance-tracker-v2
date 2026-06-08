# Build stage: use Maven with Java 17 to build the Spring Boot jar.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven files first to improve Docker layer caching.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

# Build the application without running tests during Docker image creation.
# Tests are already run separately in local/CI workflows.
RUN mvn clean package -DskipTests


# Runtime stage: use a smaller Java 17 image to run the jar.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built jar from the build stage.
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Render provides the PORT environment variable.
EXPOSE 8080

# Run the Spring Boot application.
ENTRYPOINT ["java", "-jar", "app.jar"]