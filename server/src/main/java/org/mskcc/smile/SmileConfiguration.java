package org.mskcc.smile;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 *
 * @author laptop
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "org.mskcc.smile.persistence.neo4j")
public class SmileConfiguration {
    @Value("${spring.neo4j.authentication.username}")
    private String username;

    @Value("${spring.neo4j.authentication.password}")
    private String password;

    @Value("${spring.neo4j.uri}")
    private String uri;

    @Bean
    public SessionFactory sessionFactory() {
        // with domain entity base package(s)
        return new SessionFactory(configuration(), "org.mskcc.smile.persistence");
    }

    /**
     * OGM db configuration.
     * @return
     */
    @Bean
    public org.neo4j.ogm.config.Configuration configuration() {
        return new org.neo4j.ogm.config.Configuration.Builder()
                .uri(uri)
                .credentials(username, password)
                .build();
    }

    /**
     * Added this as a means of silencing the neo4j ogm log cluttering.
     * There's probably a nicer way to do this but it gets the job done.
     */
    @Autowired
    public void logger() {
        Logger logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.drivers.bolt.response.BoltResponse.unrecognized");
        logger.setLevel(Level.OFF);
        logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.context.GraphEntityMapper");
        logger.setLevel(Level.OFF);
        logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.context.EntityGraphMapper");
        logger.setLevel(Level.OFF);
    }
}
