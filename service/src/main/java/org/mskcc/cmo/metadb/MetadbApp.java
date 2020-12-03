package org.mskcc.cmo.messaging_application;

import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication(scanBasePackages = "org.mskcc.cmo.messaging")
public class MetadbApp implements CommandLineRunner {

    @Autowired
    private Gateway messagingGateway;

    private Thread shutdownHook;
    final CountDownLatch metadbAppClose = new CountDownLatch(1);

    @Override
    public void run(String... args) {

        try {
            installShutdownHook();
            messagingGateway.initialize();
            // maybe create subscriber interface and pass these to messageGateway.subscribe?
            setupTestSubjectOneSubscription();
            setupTestSubjectTwoSubscription();
            metadbAppClose.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
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

    private void setupTestSubjectOneSubscription() throws Exception {
        messagingGateway.subscribe("test-subject-1", String.class, new MessageConsumer() {
            public void onMessage(Object message) {
                System.out.printf("*** Message received on test-subject-1 topic ***\n");
                System.out.printf("%s\n", message);
                System.out.printf("Publishing on topic: test-subject-2\n");
                try {
                    messagingGateway.publish("test-subject-2", "\"message from test-subject-1 handler to test-subject-2\"");
                } catch (Exception ignored) {}
                System.out.printf("*** End message received ***\n");
            }
        });
    }

    private void setupTestSubjectTwoSubscription() throws Exception {
        messagingGateway.subscribe("test-subject-2", String.class, new MessageConsumer() {
            public void onMessage(Object message) {
                System.out.printf("*** Message received on test-subject-2 topic ***\n");
                System.out.printf("%s\n", message);
                System.out.printf("*** End message received ***\n");
            }
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(MetadbApp.class, args);
    }
}
