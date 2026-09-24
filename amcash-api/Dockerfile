FROM maven:3.9.11-eclipse-temurin-25-alpine AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring

COPY --from=build --chown=spring:spring \
    /workspace/target/amcash-api-0.0.1-SNAPSHOT.jar \
    /app/amcash-api.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/amcash-api.jar"]
