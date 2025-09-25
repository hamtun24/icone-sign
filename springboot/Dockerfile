# Use an official lightweight JDK base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy JAR file into container
COPY /IconeSign-api-v1.0.0.jar app.jar

# Expose port (Spring Boot default is 8080)
EXPOSE 8080

# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
