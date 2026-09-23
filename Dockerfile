# --- Stage 1: build the WAR with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: run it on Tomcat 10.1 (Jakarta EE, matches jakarta.servlet-api 6.0.0) ---
FROM tomcat:10.1-jdk21
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/SkribblClone.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]