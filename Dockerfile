FROM openjdk:21-slim

RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Define a volume to safely store temporary files across restarts
VOLUME /tmp
WORKDIR /app

# Copy the JAR from the build output to the container
COPY build/libs/laa-portal-stabilisation-prototype.jar application.jar /app/

# Set the user to run the application
RUN chown -R appuser:appgroup /app
USER 1000

# Expose the application port
EXPOSE 8080

# Set the default command to run the application
CMD java -jar application.jar