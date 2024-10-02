FROM maven:3.8.8
RUN mkdir /smile-server
ADD . /smile-server
WORKDIR /smile-server
RUN mvn clean install -DskipTests

FROM openjdk:21
COPY --from=0 /smile-server/server/target/smile_server.jar /smile-server/smile_server.jar
ENTRYPOINT ["java"]
