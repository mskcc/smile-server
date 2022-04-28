package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
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
import org.mskcc.smile.model.RequestMetadata;
import org.mskcc.smile.model.SampleMetadata;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.SmileSample;
import org.mskcc.smile.service.ResearchMessageHandlingService;
import org.mskcc.smile.service.SmileRequestService;
import org.mskcc.smile.service.SmileSampleService;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.mskcc.smile.service.util.SampleDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResearchMessageHandlingServiceImpl implements ResearchMessageHandlingService {
    @Value("${igo.new_request_topic}")
    private String IGO_NEW_REQUEST_TOPIC;

    @Value("${igo.promoted_request_topic}")
    private String IGO_PROMOTED_REQUEST_TOPIC;

    @Value("${consistency_check.new_request_topic}")
    private String CONSISTENCY_CHECK_NEW_REQUEST;

    @Value("${consumers.promoted_request_topic}")
    private String CONSUMERS_PROMOTED_REQUEST_TOPIC;

    @Value("${smile.igo_request_update_topic}")
    private String IGO_REQUEST_UPDATE_TOPIC;

    @Value("${smile.igo_sample_update_topic}")
    private String IGO_SAMPLE_UPDATE_TOPIC;

    @Value("${smile.cmo_request_update_topic}")
    private String CMO_REQUEST_UPDATE_TOPIC;

    @Value("${smile.cmo_sample_update_topic}")
    private String CMO_SAMPLE_UPDATE_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Value("${num.promoted_request_handler_threads}")
    private int NUM_PROMOTED_REQUEST_HANDLERS;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    private static Gateway messagingGateway;
    private static final Log LOG = LogFactory.getLog(ResearchMessageHandlingServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();

    private static final BlockingQueue<SmileRequest> newRequestQueue =
        new LinkedBlockingQueue<SmileRequest>();
    private static final BlockingQueue<SmileRequest> promotedRequestQueue =
        new LinkedBlockingQueue<SmileRequest>();
    private static final BlockingQueue<RequestMetadata> requestUpdateQueue =
            new LinkedBlockingQueue<RequestMetadata>();
    private static final BlockingQueue<SampleMetadata> researchSampleUpdateQueue =
            new LinkedBlockingQueue<SampleMetadata>();

    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static CountDownLatch promotedRequestHandlerShutdownLatch;
    private static CountDownLatch requestUpdateHandlerShutdownLatch;
    private static CountDownLatch researchSampleUpdateHandlerShutdownLatch;

    public static enum SmileRequestDest {
        NEW_REQUEST_DEST,
        PROMOTED_REQUEST_DEST
    }

    private class IgoRequestHandler implements Runnable {

        final Phaser phaser;
        final SmileRequestDest smileRequestDest;
        final BlockingQueue<SmileRequest> requestQueue;
        final CountDownLatch shutdownLatch;
        boolean interrupted = false;

        IgoRequestHandler(Phaser phaser, SmileRequestDest smileRequestDest,
                BlockingQueue<SmileRequest> requestQueue, CountDownLatch shutdownLatch) {
            this.phaser = phaser;
            this.smileRequestDest = smileRequestDest;
            this.requestQueue = requestQueue;
            this.shutdownLatch = shutdownLatch;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SmileRequest request = requestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        SmileRequest existingRequest =
                                requestService.getSmileRequestById(request.getIgoRequestId());
                        if (existingRequest != null
                                && !requestService.requestHasUpdates(existingRequest, request)) {
                            LOG.warn("Request already exists in database and no updates were detected - "
                                    + "it will not be saved: " + request.getIgoRequestId());
                            return;
                        }

                        // save new request to database
                        if (existingRequest == null) {
                            LOG.info("Persisting new request: " + request.getIgoRequestId());
                            requestService.saveRequest(request);
                        } else {
                            LOG.info("Updating existing request: " + existingRequest.getIgoRequestId());
                            existingRequest.updateRequestMetadataByRequest(request);
                            requestService.saveRequest(existingRequest);
                        }
                        // publish updated/saved request to consistency checker or promoted request topic
                        String requestJson = mapper.writeValueAsString(
                                requestService.getPublishedSmileRequestById(request.getIgoRequestId()));
                        switch (smileRequestDest) {
                            case NEW_REQUEST_DEST:
                                LOG.info("Publishing request to: " + CONSISTENCY_CHECK_NEW_REQUEST);
                                messagingGateway.publish(request.getIgoRequestId(),
                                        CONSISTENCY_CHECK_NEW_REQUEST, requestJson);
                                break;
                            case PROMOTED_REQUEST_DEST:
                                LOG.info("Publishing request to: " + CONSUMERS_PROMOTED_REQUEST_TOPIC);
                                messagingGateway.publish(request.getIgoRequestId(),
                                        CONSUMERS_PROMOTED_REQUEST_TOPIC, requestJson);
                                break;
                            default:
                                break;
                        }
                    }
                    if (interrupted && requestQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            shutdownLatch.countDown();
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
                        SmileRequest existingRequest =
                                requestService.getSmileRequestById(requestMetadata.getIgoRequestId());
                        if (existingRequest == null) {
                            LOG.warn("Request does not already exist in the database: "
                                    + requestMetadata.getIgoRequestId()
                                    + " - will not be persisting updates.");
                        } else if (requestService.requestHasMetadataUpdates(
                                existingRequest.getLatestRequestMetadata(), requestMetadata)) {
                            // persist request-level metadata updates to database
                            LOG.info("Found updates in request metadata: " + requestMetadata.getIgoRequestId()
                                    + " - persisting to database");
                            existingRequest.updateRequestMetadataByMetadata(requestMetadata);
                            if (requestService.saveRequestMetadata(existingRequest)) {
                                LOG.info("Publishing Request-level Metadata updates "
                                        + "to " + CMO_REQUEST_UPDATE_TOPIC);
                                // publish request-level metadata history to CMO_REQUEST_UPDATE_TOPIC
                                messagingGateway.publish(existingRequest.getIgoRequestId(),
                                        CMO_REQUEST_UPDATE_TOPIC,
                                        mapper.writeValueAsString(
                                              existingRequest.getRequestMetadataList()));
                            } else {
                                LOG.error("Failed to update the request metadata for request: "
                                        + existingRequest.getIgoRequestId());
                            }
                        } else {
                            LOG.warn("There are no request-level metadata updates to persist: "
                                    + requestMetadata.getIgoRequestId());
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

    private class ResearchSampleMetadataUpdateHandler implements Runnable {

        final Phaser phaser;
        boolean interrupted = false;

        ResearchSampleMetadataUpdateHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    SampleMetadata sampleMetadata = researchSampleUpdateQueue.poll(
                            100, TimeUnit.MILLISECONDS);
                    if (sampleMetadata != null) {
                        SmileSample existingSample = sampleService.getResearchSampleByRequestAndIgoId(
                                sampleMetadata.getIgoRequestId(), sampleMetadata.getPrimaryId());
                        if (existingSample == null) {
                            LOG.info("research Sample metadata does not already exist - persisting to db: "
                                    + sampleMetadata.getPrimaryId());
                            // handle and persist new sample received
                            SmileSample sample = SampleDataFactory
                                    .buildNewResearchSampleFromMetadata(sampleMetadata.getIgoRequestId(),
                                            sampleMetadata);
                            sampleService.saveSmileSample(sample);
                            LOG.info("Publishing metadata history for new research sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(sample.getSampleMetadataList()));
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(), sampleMetadata)
                                || (!sampleService.sampleHasMetadataUpdates(
                                        existingSample.getLatestSampleMetadata(), sampleMetadata))
                                && !existingSample.getLatestSampleMetadata().getCmoSampleName()
                                        .equals(sampleMetadata.getCmoSampleName())) {
                            // logic checks if 1. comparator detects changes or 2. comparator does not
                            // detect changes because 'cmoSampleName' is ignored but the 'cmoSampleName'
                            // for existing and current sample metadata clearly differ then proceed with
                            // persisting updates for sample
                            LOG.info("Found updates for research sample - persisting to database: "
                                    + sampleMetadata.getPrimaryId());
                            // persist sample level updates to database and publish
                            // sample metadata history to CMO_SAMPLE_METADATA_UPDATE
                            existingSample.updateSampleMetadata(sampleMetadata);
                            sampleService.saveSmileSample(existingSample);
                            LOG.info("Publishing sample-level metadata history for research sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(existingSample.getSampleMetadataList()));
                        } else {
                            LOG.info("There are no updates to persist for research sample: "
                                    + sampleMetadata.getPrimaryId());
                        }
                    }
                    if (interrupted && researchSampleUpdateQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during handling of research sample metadata update", e);
                }
            }
            researchSampleUpdateHandlerShutdownLatch.countDown();
        }
    }

    @Override
    public void initialize(Gateway gateway) throws Exception {
        if (!initialized) {
            messagingGateway = gateway;
            setupIgoNewRequestHandler(messagingGateway, this);
            setupIgoPromotedRequestHandler(messagingGateway, this);
            setupRequestUpdateHandler(messagingGateway, this);
            setupResearchSampleUpdateHandler(messagingGateway, this);
            initializeMessageHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void newRequestHandler(SmileRequest request) throws Exception {
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
    public void promotedRequestHandler(SmileRequest request) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            promotedRequestQueue.put(request);
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
    public void researchSampleUpdateHandler(SampleMetadata sampleMetadata) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            researchSampleUpdateQueue.put(sampleMetadata);
        } else {
            LOG.error("Shutdown initiated, not accepting research sample update: " + sampleMetadata);
            throw new IllegalStateException("Shutdown initiated, not handling any more samples");
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        exec.shutdownNow();
        newRequestHandlerShutdownLatch.await();
        promotedRequestHandlerShutdownLatch.await();
        requestUpdateHandlerShutdownLatch.await();
        researchSampleUpdateHandlerShutdownLatch.await();
        shutdownInitiated = true;
    }

    private void initializeMessageHandlers() throws Exception {
        // new request handler
        newRequestHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser newRequestPhaser = new Phaser();
        newRequestPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            newRequestPhaser.register();
            exec.execute(new IgoRequestHandler(newRequestPhaser, SmileRequestDest.NEW_REQUEST_DEST,
                    newRequestQueue, newRequestHandlerShutdownLatch));
        }
        newRequestPhaser.arriveAndAwaitAdvance();

        // promoted request handler
        promotedRequestHandlerShutdownLatch = new CountDownLatch(NUM_PROMOTED_REQUEST_HANDLERS);
        final Phaser promotedRequestPhaser = new Phaser();
        promotedRequestPhaser.register();
        for (int lc = 0; lc < NUM_PROMOTED_REQUEST_HANDLERS; lc++) {
            promotedRequestPhaser.register();
            exec.execute(new IgoRequestHandler(promotedRequestPhaser, SmileRequestDest.PROMOTED_REQUEST_DEST,
                    promotedRequestQueue, promotedRequestHandlerShutdownLatch));
        }
        promotedRequestPhaser.arriveAndAwaitAdvance();

        // request update handler
        requestUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser requestUpdatePhaser = new Phaser();
        requestUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            requestUpdatePhaser.register();
            exec.execute(new RequestMetadataUpdateHandler(requestUpdatePhaser));
        }
        requestUpdatePhaser.arriveAndAwaitAdvance();

        // research sample update handler
        researchSampleUpdateHandlerShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser researchSampleUpdatePhaser = new Phaser();
        researchSampleUpdatePhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            researchSampleUpdatePhaser.register();
            exec.execute(new ResearchSampleMetadataUpdateHandler(researchSampleUpdatePhaser));
        }
        researchSampleUpdatePhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, ResearchMessageHandlingService
            researchMessageHandlingService) throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                try {
                    String requestJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    SmileRequest request = RequestDataFactory.buildNewLimsRequestFromJson(requestJson);
                    LOG.info("Received message on topic: " + IGO_NEW_REQUEST_TOPIC + " and request id: "
                            + request.getIgoRequestId());
                    researchMessageHandlingService.newRequestHandler(request);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request on topic: " + IGO_NEW_REQUEST_TOPIC, e);
                }
            }
        });
    }

    private void setupIgoPromotedRequestHandler(Gateway gateway, ResearchMessageHandlingService
            researchMessageHandlingService) throws Exception {
        gateway.subscribe(IGO_PROMOTED_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                try {
                    String requestJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    SmileRequest request = RequestDataFactory.buildNewLimsRequestFromJson(requestJson);
                    LOG.info("Received message on topic: " + IGO_PROMOTED_REQUEST_TOPIC + " and request id: "
                            + request.getIgoRequestId());
                    researchMessageHandlingService.promotedRequestHandler(request);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request on topic: "
                            + IGO_PROMOTED_REQUEST_TOPIC, e);
                }
            }
        });
    }

    private void setupRequestUpdateHandler(Gateway gateway, ResearchMessageHandlingService
            researchMessageHandlingService) throws Exception {
        gateway.subscribe(IGO_REQUEST_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                try {
                    String requestMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    RequestMetadata requestMetadata =
                            RequestDataFactory.buildNewRequestMetadataFromMetadata(requestMetadataJson);
                    LOG.info("Received message on topic: "  + IGO_REQUEST_UPDATE_TOPIC + " and request id: "
                            + requestMetadata.getIgoRequestId());
                    researchMessageHandlingService.requestUpdateHandler(requestMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
                }
            }
        });
    }

    private void setupResearchSampleUpdateHandler(Gateway gateway,
            ResearchMessageHandlingService researchMessageHandlingService)
            throws Exception {
        gateway.subscribe(IGO_SAMPLE_UPDATE_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + IGO_SAMPLE_UPDATE_TOPIC);
                try {
                    String sampleMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    SampleMetadata sampleMetadata =
                            SampleDataFactory.buildNewSampleMetadatafromJson(sampleMetadataJson);
                    researchMessageHandlingService.researchSampleUpdateHandler(sampleMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of research sample update on topic: "
                            + IGO_SAMPLE_UPDATE_TOPIC, e);
                }
            }
        });
    }
}
