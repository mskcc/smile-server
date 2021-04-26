FROM maven:3.6.1-jdk-8-slim
RUN mkdir -p /cmo-metadb

WORKDIR /cmo-metadb
RUN mvn clean install

FROM openjdk:8-slim
WORKDIR /cmo-metadb
COPY server/target/cmo_metadb_server.jar /cmo-metadb/cmo_metadb_server.jar
CMD ["java", "-jar", "/cmo-metadb/cmo_metadb_server.jar"]
