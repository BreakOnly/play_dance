FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home dance
COPY --from=build /workspace/target/dance-studio-1.0.0.jar /app/app.jar
USER 10001
ENV SPRING_PROFILES_ACTIVE=prod TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
