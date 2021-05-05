# CMO MetaDB 🔍

CMO MetaDB is a distributed microservices system. It receives messages from LIMS when a request is marked delivered. This message is processed and persisted to NEO4J. Any new message is then published to downstream subscribers.

## Run

### Custom properties

Make an `application.properties` based on [application.properties.EXAMPLE](src/main/resources/application.properties.EXAMPLE). 

All properties are required with the exception of some NATS connection-specific properties. The following are only required if `nats.tls_channel` is set to `true`:

- `nats.keystore_path` : path to client keystore
- `nats.truststore_path` : path to client truststore
- `nats.key_password` : keystore password
- `nats.store_password` : truststore password

Configure your `log4j.properties` based on [log4j.properties.EXAMPLE](src/main/resources/log4j.properties.EXAMPLE). The default configuration writes to std.out which is standard for container environments.

### Locally

**Requirements:**
- maven 3.6.1
- java 8

Add `application.properties` and `log4j.properties` to the local application resources: `src/main/resources`

Build with 

```
mvn clean install
```

Run with 

```
java -jar server/target/cmo_metadb_server.jar
```

### With Docker

**Requirements**
- docker

Build image with Docker

```
docker build -t <repo>/<tag>:<version> .
```

Push image to DockerHub 

```
docker push <repo>/<tag>:<version>
```

If the Docker image is built with the properties baked in then simply run with:


```
docker run --name cmo-metadb <repo>/<tag>:<version> \
	-jar /cmo-metadb/cmo_metadb_server.jar
```

Otherwise use a bind mount to make the local files available to the Docker image and add  `--spring.config.location` to the java arg

```
docker run --mount type=bind,source=<local path to properties files>,target=/cmo-metadb/src/main/resources \
	--name cmo-metadb <repo>/<tag>:<version> \
	-jar /cmo-metadb/cmo_metadb_server.jar \
	--spring.config.location=/cmo-metadb/src/main/resources/application.properties
```
