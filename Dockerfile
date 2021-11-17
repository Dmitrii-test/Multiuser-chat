FROM openjdk:11-jdk-slim
COPY /server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/app/server.jar
ENTRYPOINT ["java","-jar","/usr/app/server.jar", "123", "bot"]