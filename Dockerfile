FROM maven:3.6.1-jdk-11-slim
RUN mkdir /smile-server
ADD . /smile-server
WORKDIR /smile-server
RUN mvn clean install -DskipTests

FROM openjdk:11-slim
COPY --from=0 /smile-server/server/target/smile_server.jar /smile-server/smile_server.jar
ENTRYPOINT ["java"]
