FROM eclipse-temurin:17-jre-alpine

# Expose the port the application will run on
EXPOSE 8080

# Copy the JAR file into the container
COPY build-artifacts/*.jar /usr/local/app/app.jar

# Set the working directory to where the JAR is located
WORKDIR /usr/local/app

# Run the JAR file
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]