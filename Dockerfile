# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Unpack the fat jar (Spring Boot's own jarmode tool): the JVM loads plain
# jars from disk faster than classes nested inside a single jar, which
# shortens every cold start after Render wakes the sleeping service.
RUN java -Djarmode=tools -jar target/*.jar extract --destination extracted --application-filename app.jar

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run the app as root inside the container - a compromised process
# gains nothing beyond this unprivileged user's own file access.
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build --chown=spring:spring /app/extracted ./
USER spring

# Render sets PORT at runtime (10000 by default); application.properties reads
# it via ${PORT:8080}. Configuration and secrets come from environment
# variables set in Render (see render.yaml) - never bake them into the image.
EXPOSE 8080

# Tuned for a small app that sleeps when idle, so startup time and memory
# matter more than peak throughput:
# - TieredStopAtLevel=1: JIT with the fast C1 compiler only - noticeably
#   quicker startup, negligible difference at this traffic level;
# - SerialGC: the lightest collector, ideal for a single small container;
# - Xmx320m: a fixed heap cap that leaves room for metaspace and threads
#   inside the 512 MB of Render's free instance.
# A JAVA_OPTS variable set in Render overrides all of these.
ENV JAVA_OPTS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xmx320m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
