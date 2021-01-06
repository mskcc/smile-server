package org.mskcc.cmo.metadb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.RequestService;
import org.mskcc.cmo.shared.neo4j.CmoRequestEntity;
import org.mskcc.cmo.shared.neo4j.SampleManifestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@EnableNeo4jRepositories(basePackages = "org.mskcc.cmo.metadb.persistence")
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging", "org.mskcc.cmo.metadb.service"})
public class MetadbApp implements CommandLineRunner {

    @Autowired
    private Gateway messagingGateway;

    @Autowired
    private MessageHandlingService messageHandlingService;
    
    @Autowired
    private RequestService requestService;

    private Thread shutdownHook;
    final CountDownLatch metadbAppClose = new CountDownLatch(1);

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting up MetaDB application...");
        try {
            installShutdownHook();
            messagingGateway.connect();
            messageHandlingService.initialize(messagingGateway);
            saveReq();
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
                        messageHandlingService.shutdown();
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
    
    public void saveReq() throws Exception {
        try {
           // LOG.info("*** Saving mock request ***\n");
            CmoRequestEntity request = mockRequestData();
            if (request != null) {
                requestService.saveRequest(request); 
            }
            else {
                throw new RuntimeException("request is null");
            }
            
           // LOG.info("*** Saved mock request ***\n");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private CmoRequestEntity mockRequestData() {
        CmoRequestEntity request = new CmoRequestEntity();
        request.setRequestId("12345");
        SampleManifestEntity s1 = new SampleManifestEntity();
        //s1.setUuid(UUID.randomUUID());
        s1.setIgoId("123");
        s1.setInvestigatorSampleId("999");
        SampleManifestEntity s2 = new SampleManifestEntity();
        //s2.setUuid(UUID.randomUUID());
        s2.setIgoId("456");
        s2.setInvestigatorSampleId("000");
        List<SampleManifestEntity> sampleManifestList = new ArrayList<SampleManifestEntity>();
        sampleManifestList.add(s1);
        sampleManifestList.add(s2);
        request.setSampleManifestList(sampleManifestList);
        return request;
    }
}
