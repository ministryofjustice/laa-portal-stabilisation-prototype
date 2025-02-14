FROM openjdk:21-slim

# Create a group and user to run the application
RUN addgroup --gid 1000 appgroup && \
    adduser --uid 1000 --ingroup appgroup --disabled-password --gecos "" appuser

# Define a volume to safely store temporary files across restarts
VOLUME /tmp
WORKDIR /app

# Copy the JAR from the build output to the container
COPY build/libs/laa-portal-stabilisation-prototype.jar /app/application.jar

# Set the user to run the application
RUN chown -R appuser:appgroup /app
USER 1000

# Expose the application port
EXPOSE 8080

# Set the default command to run the application
CMD ["java", "-jar", "application.jar"]
