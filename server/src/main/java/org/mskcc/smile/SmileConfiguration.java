/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.mskcc.smile;

import org.neo4j.ogm.session.SessionFactory;

//import org.neo4j.driver.internal.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
////import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
//import org.springframework.data.neo4j.core.DatabaseSelection;
//import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
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
    
//    private String database = "neo4j";
    
    @Bean
    public SessionFactory sessionFactory() {
        // with domain entity base package(s)
        return new SessionFactory(configuration(), "org.mskcc.smile.persistence");
    }

    @Bean
    public org.neo4j.ogm.config.Configuration configuration() {
        return new org.neo4j.ogm.config.Configuration.Builder()
                .uri(uri)
                .credentials(username, password)
                .build();
    }
//        
//    @Bean
//    DatabaseSelectionProvider databaseSelectionProvider() {
//        return () -> {
//            return DatabaseSelection.byName(database);
//        };
//    }

//    @Bean
//    public Neo4jTransactionManager transactionManager() {
//        return new Neo4jTransactionManager(sessionFactory());
//    }
    
}
