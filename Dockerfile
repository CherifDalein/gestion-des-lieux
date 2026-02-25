FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/data/uploads/images \
    && chown -R spring:spring /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENV PORT=8080
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/lieux_db;MODE=MySQL;DB_CLOSE_DELAY=-1
ENV APP_UPLOAD_DIR=/app/data/uploads/images

EXPOSE 8080

USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
