FROM maven:3.9.6-amazoncorretto-21 AS builder
WORKDIR /app

COPY AI_Incident_Assistant/pom.xml ./AI_Incident_Assistant/pom.xml
COPY AI_Incident_Assistant/src ./AI_Incident_Assistant/src

RUN mvn -f AI_Incident_Assistant/pom.xml clean package -DskipTests

FROM ghcr.io/graalvm/jdk-community:21
WORKDIR /app

COPY --from=builder /app/AI_Incident_Assistant/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
