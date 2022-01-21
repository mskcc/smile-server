package org.mskcc.cmo.metadb;

import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.service.AdminMessageHandlingService;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.RequestReplyHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.stereotype.Controller;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EntityScan(basePackages = "org.mskcc.cmo.metadb.model")
@EnableNeo4jRepositories(basePackages = "org.mskcc.cmo.metadb.persistence.neo4j")
@EnableJpaRepositories(basePackages = "org.mskcc.cmo.metadb.persistence.jpa")
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.cmo.common.*", "org.mskcc.cmo.metadb.*"})
@Controller
@EnableCaching
@EnableSwagger2
public class MetadbApp implements CommandLineRunner {
    private static final Log LOG = LogFactory.getLog(MetadbApp.class);

    @Autowired
    private Gateway messagingGateway;

    @Autowired
    private MessageHandlingService messageHandlingService;

    @Autowired
    private RequestReplyHandlingService requestReplyHandlingService;

    @Autowired
    private AdminMessageHandlingService adminMessageHandlingService;

    private Thread shutdownHook;
    final CountDownLatch metadbAppClose = new CountDownLatch(1);

    /**
     * Docket bean for Swagger configuration.
     * @return Docket
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(true)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("CMO MetaDB REST API")
                .build();
    }

    @Override
    public void run(String... args) throws Exception {
        LOG.info("Starting up MetaDB application...");
        try {
            installShutdownHook();
            messagingGateway.connect();
            requestReplyHandlingService.initialize(messagingGateway);
            messageHandlingService.initialize(messagingGateway);
            adminMessageHandlingService.initialize(messagingGateway);
            metadbAppClose.await();
        } catch (Exception e) {
            LOG.error("Encountered error during initialization", e);
        }
    }

    private void installShutdownHook() {
        shutdownHook =
            new Thread() {
                public void run() {
                    System.err.printf("\nCaught CTRL-C, shutting down gracefully...\n");
                    try {
                        requestReplyHandlingService.shutdown();
                        messageHandlingService.shutdown();
                        adminMessageHandlingService.shutdown();
                        messagingGateway.shutdown();
                    } catch (Exception e) {
                        LOG.error("Encountered error during shutdown process", e);
                    }
                    metadbAppClose.countDown();
                }
            };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static void main(String[] args) {
        SpringApplication.run(MetadbApp.class, args);
    }
}
