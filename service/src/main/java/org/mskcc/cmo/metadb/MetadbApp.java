package org.mskcc.cmo.metadb;

import java.util.concurrent.CountDownLatch;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.persistence.SampleMetadataRepository;
import org.mskcc.cmo.metadb.service.consumer.SampleIntakeMessageConsumer;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@EnableNeo4jRepositories(basePackages = "org.mskcc.cmo.metadb.persistence")
@SpringBootApplication(scanBasePackages = "org.mskcc.cmo.messaging")
public class MetadbApp implements CommandLineRunner {
    private final String NEW_SAMPLE_INTAKE = "igo.new-sample-intake";

    @Autowired
    private Gateway messagingGateway;
    
    @Autowired
    private SampleMetadataRepository sampleMetadataRepository;

    private Thread shutdownHook;
    final CountDownLatch metadbAppClose = new CountDownLatch(1);

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting up MetaDB application...");
        try {
            installShutdownHook();
            messagingGateway.connect();

            // maybe create subscriber interface and pass handlers to messageGateway.subscribe?
            messagingGateway.subscribe(NEW_SAMPLE_INTAKE, SampleMetadataEntity.class,
                    new SampleIntakeMessageConsumer(messagingGateway, sampleMetadataRepository));
            metadbAppClose.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    private void installShutdownHook() {
        shutdownHook =
            new Thread() {
                public void run() {
                    System.err.printf("\nCaught CTRL-C, shutting down gracefully...\n");
                    try {
                        messagingGateway.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
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
