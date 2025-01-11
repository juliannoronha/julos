FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the JAR file from your target directory
COPY target/*.jar app.jar

# Expose the port that matches your docker-compose.yml
EXPOSE 8443

# Optional: Add any environment variables needed
ENV SPRING_PROFILES_ACTIVE=test

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]