# syntax = docker/dockerfile:experimental

# ------------------------------------------------------------------------------
# BUILD STAGE
# ------------------------------------------------------------------------------

FROM  maven:3.8.5-eclipse-temurin-17 as build


WORKDIR /workspace/

COPY pom.xml .
COPY src src

COPY .git .git

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -e -DskipTests=true package

# ------------------------------------------------------------------------------
# RUNTIME STAGE (deployment)
# ------------------------------------------------------------------------------
FROM openjdk:17.0.2-slim-buster


ENV app_name="rendezvous-client-jar-with-dependencies"

COPY --from=build /workspace/target/${app_name}.jar /opt/software/${app_name}/${app_name}.jar

WORKDIR /opt/software/${app_name}

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/urandom -jar ${app_name}.jar"]
