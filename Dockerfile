FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY src src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S whs && adduser -S whs -G whs

COPY --from=build /workspace/target/*.jar /app/app.jar

RUN mkdir -p /app/logs /app/temp/email-attachments && chown -R whs:whs /app

USER whs
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
