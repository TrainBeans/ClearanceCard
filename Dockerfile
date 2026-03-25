# ── Stage 1: build the fat JAR ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Copy wrapper + pom first so the dependency layer is cached separately
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw clean package -DskipTests --batch-mode --no-transfer-progress \
    && mv target/ClearanceCard-*.jar target/app.jar

# ── Stage 2: minimal JRE runtime image ─────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/target/app.jar app.jar

EXPOSE 8080


ENTRYPOINT ["java", "-jar", "app.jar"]

