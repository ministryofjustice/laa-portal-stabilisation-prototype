FROM openjdk:17

# Define a volume to safely store temporary files across restarts
VOLUME /tmp

# Copy the JAR from the build output to the container
COPY build/libs/laa-portal-stabilisation-prototype.jar application.jar

# Expose the application port
EXPOSE 8080

# Set the default command to run the application
CMD java -jar application.jar
