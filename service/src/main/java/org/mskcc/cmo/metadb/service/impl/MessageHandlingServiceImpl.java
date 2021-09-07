package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.mskcc.cmo.metadb.service.MetadbRequestService;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${consistency_check.new_request_topic}")
    private String CONSISTENCY_CHECK_NEW_REQUEST;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private MetadbRequestService requestService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetaDbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetaDbRequest>();
    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static Gateway messagingGateway;

    private static final Log LOG = LogFactory.getLog(MessageHandlingServiceImpl.class);

    private class NewIgoRequestHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        NewIgoRequestHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    MetaDbRequest request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        MetaDbRequest existingRequest = requestService.getMetadbRequestById(request.getRequestId());

                        // persist new request to database
                        if (existingRequest == null) {
                            requestService.saveRequest(request);
                            messagingGateway.publish(request.getRequestId(),
                                    CONSISTENCY_CHECK_NEW_REQUEST,
                                    mapper.writeValueAsString(
                                            requestService.getPublishedMetadbRequestById(request.getRequestId())));
                        } else {
                            // check if there are updates to persist to db

                            //TODO: ADD MECHANISM FOR DETERMINING IF REQUEST HAS SAMPLE-LEVEL METADATA UPDATES
                            // Consider adding a data util tool that tells us at if the updates are 1. request-level,
                            // sample-level, or both?
                            // we need to be able to know which samples have updates so that they can be published
                            // to the CMO_SAMPLE_METADATA_UPDATE topic
                            // if there are request-level updates then they must be published to the
                            // CMO_REQUEST_METADATA_UPDATE topic as well
                            // regardless of which level the updates occur, the updates still need to be persisted
                            // to the database

                            // add method that returns the samples that contain updates. if the returned list is empty the
                            // there are only request-level updates if hasUpdates() method returns true
                            // however this wouldnt necessarily tell us that are specifically request-level metadata updates
                            // to publish to CMO_REQUEST_METADATA_UPDATE
                            if (requestService.requestHasUpdates(existingRequest, request)) {
                                existingRequest.updateRequestMetadata(request);
                                requestService.saveRequest(request);
                            } else {
                                LOG.warn("Request already in database - it will not be saved: "
                                        + request.getRequestId());
                            }
                        }
                    }
                    if (interrupted && newRequestQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            newRequestHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupIgoNewRequestHandler(messagingGateway, this);
            initializeNewRequestHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void newRequestHandler(MetaDbRequest request) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            newRequestQueue.put(request);
        } else {
            LOG.error("Shutdown initiated, not accepting request: " + request);
            throw new IllegalStateException("Shutdown initiated, not handling any more requests");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newRequestHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeNewRequestHandlers() throws Exception {
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newSamplePhaser = new Phaser();
        newSamplePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newSamplePhaser.register();
            exec.execute(new NewIgoRequestHandler(newSamplePhaser));
        }
        newSamplePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + IGO_NEW_REQUEST_TOPIC);
                try {
                    String requestJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    MetaDbRequest metaDbRequest = mapper.readValue(requestJson,
                            MetaDbRequest.class);
                    metaDbRequest.setRequestJson(requestJson);
                    metaDbRequest.setMetaDbSampleList(extractMetaDbSamplesFromIgoResponse(requestJson));
                    metaDbRequest.setNamespace("igo");
                    messageHandlingService.newRequestHandler(metaDbRequest);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request on topic: " + IGO_NEW_REQUEST_TOPIC, e);
                }
            }
        });
    }

    private List<MetaDbSample> extractMetaDbSamplesFromIgoResponse(Object message)
            throws JsonProcessingException, IOException {
        Map<String, Object> map = mapper.readValue(message.toString(), Map.class);
        SampleMetadata[] sampleList = mapper.convertValue(map.get("samples"),
                SampleMetadata[].class);

        List<MetaDbSample> metaDbSampleList = new ArrayList<>();
        for (SampleMetadata sample: sampleList) {
            // update import date here since we are parsing from json
            sample.setImportDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            sample.setRequestId((String) map.get("requestId"));
            MetaDbSample metaDbSample = new MetaDbSample();
            metaDbSample.addSampleMetadata(sample);
            metaDbSampleList.add(metaDbSample);
        }
        return metaDbSampleList;
    }
}
