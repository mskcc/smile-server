package org.mskcc.cmo.metadb.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.service.LimsRestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.mskcc.cmo.shared.neo4j.SampleMetadataEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LimsRestServiceImpl implements LimsRestService {

    @Value("${cmo.new.request.topic}")
    private String CMO_NEW_REQUEST;

    @Value("${limsrest.poll.interval:300000}")
    private int LIMSREST_POLL_INTERVAL;

    @Value("${path.to.getdelivery.endpoint.date}")
    private String PATH_TO_GETDELIVERY_ENDPOINT_DATE;

    @Autowired
    private SampleService sampleService;

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;

    private static final ExecutorService exec = Executors.newSingleThreadExecutor();
    private static final CountDownLatch limsRestHandlerShutdownLatch = new CountDownLatch(1);

    private static LocalDateTime getDeliveryEndpointDate;

    private class LimsRestHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        LimsRestHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    if (interrupted) {
                        break;
                    }
                    getDeliveryEndpointDate = LocalDateTime.now();
                    // poll limsrest.get-deliveries (clarify what is int64 timestamp - unix epoch?)
                    // if epoch required, use getDeliveryEndpointDate.toEpochSecond(ZoneOffset.ofHours(-5)));
                    // for (each-new-request-to-process) {
                    //     // persist request using autowired request service
                    //     for (each-sample-in-request) {
                    //         // get sample manifest
                    //         // persist sample manifest using autowired sample service
                    //     }
                    //     // combine request & sample manifests into single json
                    //     // publish request to cmo.new.request.topic
                    //     // update latest getDeliveries timestamp to file in case of shutdown
                    //     writeLimsRestGetDeliveryDate(getDeliveryEndpointDate);
                    // }
                    Thread.sleep(LIMSREST_POLL_INTERVAL);
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    System.err.printf("Error during LimsRest processing: %s\n", e.getMessage());
                }
            }
            limsRestHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {

        if (!initialized) {
            getDeliveryEndpointDate = readLimsRestGetDeliveryDate();
            final Phaser limsRestPhaser = new Phaser();
            limsRestPhaser.register(); // register self
            limsRestPhaser.register(); // register handler thread
            exec.execute(new LimsRestHandler(limsRestPhaser));
            limsRestPhaser.arriveAndAwaitAdvance();
            initialized = true;
        } else {
            System.err.printf("LimsRest Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("LimsRest Service has not been initialized");
        }
        exec.shutdownNow();
        limsRestHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private LocalDateTime readLimsRestGetDeliveryDate() throws Exception {
        Path path = Paths.get(PATH_TO_GETDELIVERY_ENDPOINT_DATE);
        return LocalDateTime.parse(Files.readAllLines(path).get(0));
    }

    private void writeLimsRestGetDeliveryDate(LocalDateTime getDeliveryEndpointDate) throws Exception {
        Path path = Paths.get(PATH_TO_GETDELIVERY_ENDPOINT_DATE);
        String date = getDeliveryEndpointDate.toString() + "\n";
        Files.write(path, date.getBytes());
    }
}
