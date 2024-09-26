package org.mskcc.smile;

import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.smile.service.ClinicalMessageHandlingService;
import org.mskcc.smile.service.CorrectCmoPatientHandlingService;
import org.mskcc.smile.service.RequestReplyHandlingService;
import org.mskcc.smile.service.ResearchMessageHandlingService;
import org.mskcc.smile.service.TempoMessageHandlingService;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.stereotype.Controller;
//import springfox.documentation.builders.ApiInfoBuilder;
//import springfox.documentation.builders.PathSelectors;
//import springfox.documentation.builders.RequestHandlerSelectors;
//import springfox.documentation.service.ApiInfo;
//import springfox.documentation.spi.DocumentationType;
//import springfox.documentation.spring.web.plugins.Docket;
//import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EntityScan(basePackages = "org.mskcc.smile.model")
@EnableNeo4jRepositories(basePackages = "org.mskcc.smile.persistence.neo4j")
@EnableJpaRepositories(basePackages = "org.mskcc.smile.persistence.jpa")
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.smile.commons.*", "org.mskcc.smile.*"})
@Controller
@EnableCaching
//@EnableSwagger2
//@Import(SmileConfiguration.class)
public class SmileApp implements CommandLineRunner {
    private static final Log LOG = LogFactory.getLog(SmileApp.class);

    @Autowired
    private Gateway messagingGateway;

    @Autowired
    private ResearchMessageHandlingService researchMessageHandlingService;
    
    @Autowired
    private ClinicalMessageHandlingService clinicalMessageHandlingService;
    
    @Autowired
    private CorrectCmoPatientHandlingService correctCmoPatientHandlingService;

    @Autowired
    private TempoMessageHandlingService tempoMessageHandlingService;

    @Autowired
    private RequestReplyHandlingService requestReplyHandlingService;

    private Thread shutdownHook;
    final CountDownLatch smileAppClose = new CountDownLatch(1);

    /**
     * Docket bean for Swagger configuration.
     * @return Docket
     */
//    @Bean
//    public Docket api() {
//        return new Docket(DocumentationType.SWAGGER_2)
//                .useDefaultResponseMessages(true)
//                .apiInfo(apiInfo())
//                .select()
//                .apis(RequestHandlerSelectors.any())
//                .paths(PathSelectors.any())
//                .build();
//    }
//
//    private ApiInfo apiInfo() {
//        return new ApiInfoBuilder()
//                .title("CMO SMILE REST API")
//                .build();
//    }

  @Bean
  public GroupedOpenApi api() {
      return GroupedOpenApi.builder()
              .group("smile rest api")
              .packagesToScan("org.mskcc.smile.web")
              .pathsToMatch("/**")
//              .pathsToMatch("/public/**")
              .build();
  }
//  @Bean
//  public GroupedOpenApi adminApi() {
//      return GroupedOpenApi.builder()
//              .group("springshop-admin")
//              .pathsToMatch("/admin/**")
//              .addOpenApiMethodFilter(method -> method.isAnnotationPresent(Admin.class))
//              .build();
//  }    
    
    
    @Override
    public void run(String... args) throws Exception {
        LOG.info("Starting up SMILE Server application...");
        try {
            installShutdownHook();
            messagingGateway.connect();
            requestReplyHandlingService.initialize(messagingGateway);
            researchMessageHandlingService.initialize(messagingGateway);
            clinicalMessageHandlingService.initialize(messagingGateway);
            correctCmoPatientHandlingService.initialize(messagingGateway);
            tempoMessageHandlingService.intialize(messagingGateway);
            smileAppClose.await();
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
                        researchMessageHandlingService.shutdown();
                        clinicalMessageHandlingService.shutdown();
                        correctCmoPatientHandlingService.shutdown();
                        tempoMessageHandlingService.shutdown();
                        messagingGateway.shutdown();
                    } catch (Exception e) {
                        LOG.error("Encountered error during shutdown process", e);
                    }
                    smileAppClose.countDown();
                }
            };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static void main(String[] args) {
        SpringApplication.run(SmileApp.class, args);
    }
}
