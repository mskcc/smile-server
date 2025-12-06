FROM maven:3.8.8
RUN mkdir /smile-server
ADD . /smile-server
WORKDIR /smile-server
RUN mvn clean install -DskipTests

FROM ibm-semeru-runtimes:open-21.0.9_10-jdk-jammy
COPY --from=0 /smile-server/server/target/smile_server.jar /smile-server/smile_server.jar

# copy the entrypoint script and make it executable
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
