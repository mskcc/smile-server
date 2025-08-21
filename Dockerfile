FROM maven:3.8.8
RUN mkdir /smile-server
ADD . /smile-server
WORKDIR /smile-server
RUN mvn clean install -DskipTests

FROM openjdk:21
COPY --from=0 /smile-server/server/target/smile_server.jar /smile-server/smile_server.jar

# Copy the entrypoint script and make it executable
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Set the SMILE_CONFIG_HOME environment variable (example, adjust as needed)
###might not need this .. tbd
###ENV SMILE_CONFIG_HOME="/smile-server/config" 
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
