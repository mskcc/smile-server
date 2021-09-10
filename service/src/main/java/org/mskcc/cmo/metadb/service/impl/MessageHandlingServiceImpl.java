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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.MessageConsumer;
import org.mskcc.cmo.metadb.model.MetaDbRequest;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlingServiceImpl implements MessageHandlingService {

    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${consistency_check.new_request_topic}")
    private String CONSISTENCY_CHECK_NEW_REQUEST;

    @Value("${metadb.igo_request_update_topic}")
    private String IGO_REQUEST_UPDATE_TOPIC;

    @Value("${metadb.igo_sample_update_topic}")
    private String IGO_SAMPLE_UPDATE_TOPIC;

    @Value("${metadb.cmo_request_update_topic}")
    private String CMO_REQUEST_UPDATE_TOPIC;

    @Value("${metadb.cmo_sample_update_topic}")
    private String CMO_SAMPLE_UPDATE_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private SampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetaDbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetaDbRequest>();
    private static final BlockingQueue<RequestMetadata> requestUpdateQueue =
            new LinkedBlockingQueue<RequestMetadata>();
    private static final BlockingQueue<SampleMetadata> sampleUpdateQueue =
            new LinkedBlockingQueue<SampleMetadata>();

    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static CountDownLatch requestUpdateHandlerShutdownLatch;
    private static CountDownLatch sampleUpdateHandlerShutdownLatch;
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
                        MetaDbRequest existingRequest =
                                requestService.getMetadbRequestById(request.getRequestId());

                        // persist new request to database
                        if (existingRequest == null) {
                            requestService.saveRequest(request);
                            messagingGateway.publish(request.getRequestId(),
                                    CONSISTENCY_CHECK_NEW_REQUEST,
                                    mapper.writeValueAsString(
                                            requestService.getPublishedMetadbRequestById(
                                                    request.getRequestId())));
                        } else if (requestService.requestHasUpdates(existingRequest, request)) {
                            // PROPOSED FLOW:
                            // 1. Persist the request json received in database
                            //   - requestService.updateRequestJsonForRequest(request.getRequestJson())
                            // 2. Pass request to RequestUpdateMessageHandler
                            //   - message handler will persist updates to request metadata to database
                            //     and will handle the request metadata versioning
                            //   - message handler will also publish request metadata history to
                            //     CMO_REQUEST_METADATA_UPDATE topic
                            // 3. Pass SampleMetadata to SampleUpdateMessageHandler
                            //   - message handler will identify which samples in the request
                            //     contain updates that haven't been persisted to the db yet
                            //     and will handle the sample metadata versioning
                            //   - message handler will also publish sample metadata history
                            //     for each sample to CMO_SAMPLE_METADATA_UPDATE topic

                            // make call to update the requestJson member of existingRequest in the
                            // database to reflect the latest version of the raw json string that we got
                            // directly from IGO LIMS

                            // message handlers will check if there are updates to persist or not
                            requestUpdateQueue.add(request.getLatestRequestMetadata());
                            for (MetaDbSample sample : request.getMetaDbSampleList()) {
                                sampleUpdateQueue.add(sample.getLatestSampleMetadata());
                            }
                        } else {
                            LOG.warn("Request already in database - it will not be saved: "
                                    + request.getRequestId());
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

    private class RequestMetadataUpdateHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        RequestMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    RequestMetadata requestMetadata = requestUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (requestMetadata != null) {
                        MetaDbRequest existingRequest =
                                requestService.getMetadbRequestById(requestMetadata.getRequestId());

                        if (requestService.requestHasMetadataUpdates(
                                existingRequest.getLatestRequestMetadata(), requestMetadata)) {
                            // persist request-level metadata updates to database
                            // existingRequest.updateRequestMetadata(request);
                            // requestService.updateRequestMetadata(existingRequest);

                            //LOG.info("Publishing Request-level Metadata updates "
                            //        + "to CMO_REQUEST_METADATA_UPDATE");
                            // publish request-level metadata history to CMO_REQUEST_UPDATE_TOPIC ..
                            //messagingGateway.publish(existingRequest.getRequestId(),
                            //        CMO_REQUEST_UPDATE_TOPIC,
                            //        mapper.writeValueAsString(
                            //              existingRequest.getRequestMetadataList()));
                        } else {
                            LOG.warn("There are no request-level metadata updates to persist: "
                                    + requestMetadata.getRequestId());
                        }
                    }
                    if (interrupted && requestUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of request metadata update", e);
                }
            }
            requestUpdateHandlerShutdownLatch.countDown();
        }
    }

    private class SampleMetadataUpdateHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        SampleMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SampleMetadata sampleMetadata = sampleUpdateQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (sampleMetadata != null) {
                        MetaDbSample existingSample = sampleService.getMetaDbSampleByRequestAndIgoId(
                                sampleMetadata.getRequestId(), sampleMetadata.getIgoId());
                        if (existingSample == null) {
                            // handle and persist new sample received
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(), sampleMetadata)) {
                            // persist sample level updates to database and publish
                            // sample metadata history to CMO_SAMPLE_METADATA_UPDATE
                            // existingSample.updatesampleMetadata(sampleMetadata)
                            // sampleService.updateSampleMetadata(sample)
                            // LOG.info("Publishing sample-level metadata history for sample: "
                            // + sample.getIgoId());
                            //messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                            //        mapper.writeValueAsString(sample.getSampleMetadataList()));
                        }
                    } else {
                        LOG.warn("There are no updates to persist for current sample: "
                                + sampleMetadata.getIgoId());
                    }
                    if (interrupted && sampleUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of sample metadata update", e);
                }
            }
            sampleUpdateHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupIgoNewRequestHandler(messagingGateway, this);
            setupRequestUpdateHandler(messagingGateway, this);
            setupSampleUpdateHandler(messagingGateway, this);
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
    public void requestUpdateHandler(RequestMetadata requestMetadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            requestUpdateQueue.put(requestMetadata);
        } else {
            LOG.error("Shutdown initiated, not accepting request update: " + requestMetadata);
            throw new IllegalStateException("Shutdown initiated, not handling any more requests");
        }
    }

    @Override
    public void sampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            sampleUpdateQueue.put(sampleMetadata);
        } else {
            LOG.error("Shutdown initiated, not accepting sample update: " + sampleMetadata);
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
        requestUpdateHandlerShutdownLatch.await();
        sampleUpdateHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeNewRequestHandlers() throws Exception {
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newRequestPhaser = new Phaser();
        newRequestPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newRequestPhaser.register();
            exec.execute(new NewIgoRequestHandler(newRequestPhaser));
        }
        newRequestPhaser.arriveAndAwaitAdvance();

        // request update handler
        requestUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser requestUpdatePhaser = new Phaser();
        requestUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            requestUpdatePhaser.register();
            exec.execute(new RequestMetadataUpdateHandler(requestUpdatePhaser));
        }
        requestUpdatePhaser.arriveAndAwaitAdvance();

        // sample update handler
        sampleUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser sampleUpdatePhaser = new Phaser();
        sampleUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            sampleUpdatePhaser.register();
            exec.execute(new SampleMetadataUpdateHandler(sampleUpdatePhaser));
        }
        sampleUpdatePhaser.arriveAndAwaitAdvance();
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

    private void setupRequestUpdateHandler(Gateway gateway, MessageHandlingService messageHandlingService)
            throws Exception {
        gateway.subscribe(IGO_REQUEST_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String requestMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    Map<String, String> requestMetadataMap =
                            mapper.readValue(requestMetadataJson, Map.class);
                    RequestMetadata requestMetadata = new RequestMetadata(
                            requestMetadataMap.get("requestId"),
                            requestMetadataJson,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    messageHandlingService.requestUpdateHandler(requestMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
                }
            }
        });
    }

    private void setupSampleUpdateHandler(Gateway gateway, MessageHandlingService messageHandlingService)
            throws Exception {
        gateway.subscribe(IGO_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String sampleMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    SampleMetadata sampleMetadata =
                            mapper.readValue(sampleMetadataJson, SampleMetadata.class);
                    sampleMetadata.setImportDate(
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    messageHandlingService.sampleUpdateHandler(sampleMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
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
