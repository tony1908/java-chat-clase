FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn -B test package

##########################################################

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/javachatclase-1.0-SNAPSHOT.jar /app/javachatclase-1.0-SNAPSHOT.jar

CMD ["java", "-jar", "javachatclase-1.0-SNAPSHOT.jar"]