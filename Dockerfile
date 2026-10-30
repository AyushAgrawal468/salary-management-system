# One image serves both the API and the Angular UI from the same origin (no CORS needed).

# --- 1. Build the Angular UI ---
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- 2. Build the Spring Boot jar with the UI bundled as static resources ---
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/dist/frontend/browser src/main/resources/static
# Tests need Postgres and run in CI before this image is built.
RUN ./mvnw -B -q package -DskipTests

# --- 3. Minimal runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=backend /app/target/salary-management-*.jar app.jar
USER app
# Size the heap from the container's memory limit (small hosts have 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
