# Use a stable Java 17 image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy all project files to the container
COPY . .

# Compile the Java code
RUN mkdir -p out && javac -cp "lib/*" src/SchoolServer.java -d out

# Expose the port (Default 8080)
EXPOSE 8080

# Run the server
CMD ["java", "-cp", "out:lib/*", "SchoolServer"]
