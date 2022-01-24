package org.mskcc.cmo.metadb.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import org.mskcc.cmo.metadb.model.MetadbPatient;
import org.mskcc.cmo.metadb.model.MetadbRequest;
import org.mskcc.cmo.metadb.model.MetadbSample;
import org.mskcc.cmo.metadb.model.RequestMetadata;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.mskcc.cmo.metadb.service.CrdbMappingService;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.mskcc.cmo.metadb.service.MetadbPatientService;
import org.mskcc.cmo.metadb.service.MetadbRequestService;
import org.mskcc.cmo.metadb.service.MetadbSampleService;
import org.mskcc.cmo.metadb.service.util.RequestDataFactory;
import org.mskcc.cmo.metadb.service.util.SampleDataFactory;
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

    @Value("${metadb.correct_cmoptid_topic}")
    private String CORRECT_CMOPTID_TOPIC;

    @Value("${num.new_request_handler_threads}")
    private int NUM_NEW_REQUEST_HANDLERS;

    @Autowired
    private CrdbMappingService crdbMappingService;

    @Autowired
    private MetadbPatientService patientService;

    @Autowired
    private MetadbRequestService requestService;

    @Autowired
    private MetadbSampleService sampleService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;
    private static volatile boolean shutdownInitiated;
    private static final ExecutorService exec = Executors.newCachedThreadPool();
    private static final BlockingQueue<MetadbRequest> newRequestQueue =
        new LinkedBlockingQueue<MetadbRequest>();
    private static final BlockingQueue<RequestMetadata> requestUpdateQueue =
            new LinkedBlockingQueue<RequestMetadata>();
    private static final BlockingQueue<SampleMetadata> sampleUpdateQueue =
            new LinkedBlockingQueue<SampleMetadata>();
    private static final BlockingQueue<Map<String, String>> correctCmoPatientIdQueue =
            new LinkedBlockingQueue<Map<String, String>>();

    private static CountDownLatch newRequestHandlerShutdownLatch;
    private static CountDownLatch requestUpdateHandlerShutdownLatch;
    private static CountDownLatch sampleUpdateHandlerShutdownLatch;
    private static CountDownLatch correctCmoPatientIdShutdownLatch;
    private static Gateway messagingGateway;

    private static final Log LOG = LogFactory.getLog(MessageHandlingServiceImpl.class);

    private class CorrectCmoPatientIdReqReplyHandler implements Runnable {
        final Phaser phaser;
        boolean interrupted = false;

        CorrectCmoPatientIdReqReplyHandler(Phaser phaser) {
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arrive();
            while (true) {
                try {
                    Map<String, String> idCorrectionMap =
                            correctCmoPatientIdQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (idCorrectionMap != null) {
                        String oldCmoPatientId = idCorrectionMap.get("oldId");
                        String newCmoPatientId = idCorrectionMap.get("newId");

                        // get samples by old cmo patient id before updating the
                        // cmo patient id for the given patient alias/patient node
                        List<MetadbSample> samples =
                                sampleService.getMetadbSampleListByCmoPatientId(oldCmoPatientId);
                        MetadbPatient updatedPatient = patientService.updateCmoPatientId(
                                oldCmoPatientId, newCmoPatientId);

                        for (MetadbSample sample: samples) {
                            // TODO: add support for clinical sample updates
                            // publish research samples only to label generator
                            if (sample.getSampleCategory().equals("research")) {
                                SampleMetadata latestMetadata = sample.getLatestSampleMetadata();
                                latestMetadata.setCmoPatientId(newCmoPatientId);
                                LOG.info("Replacing patient ID prefix for sample: "
                                        + latestMetadata.getPrimaryId() + " to new CMO patient ID prefix");
                                String newCmoSampleLabel = latestMetadata.getCmoSampleName()
                                        .replaceAll(oldCmoPatientId, newCmoPatientId);
                                latestMetadata.setCmoSampleName(newCmoSampleLabel);
                                // add to sample update queue
                                sampleUpdateQueue.add(latestMetadata);
                            } else if (sample.getSampleCategory().equals("clinical")) {
                                LOG.info("CLINICAL SAMPLE UPDATES NOT SUPPORTED YET");
                            }
                        }
                        LOG.info("CMO sample label should be updated shortly through the "
                                + "IGO_SAMPLE_UPDATE topic");
                    }
                    if (interrupted && correctCmoPatientIdQueue.isEmpty()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (Exception e) {
                    LOG.error("Error during request handling", e);
                }
            }
            correctCmoPatientIdShutdownLatch.countDown();
        }
    }

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
                    MetadbRequest request = newRequestQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (request != null) {
                        MetadbRequest existingRequest =
                                requestService.getMetadbRequestById(request.getIgoRequestId());

                        // persist new request to database
                        if (existingRequest == null) {
                            LOG.info("Received new request with id: " + request.getIgoRequestId());
                            requestService.saveRequest(request);
                            messagingGateway.publish(request.getIgoRequestId(),
                                    CONSISTENCY_CHECK_NEW_REQUEST,
                                    mapper.writeValueAsString(
                                            requestService.getPublishedMetadbRequestById(
                                                    request.getIgoRequestId())));
                        } else if (requestService.requestHasUpdates(existingRequest, request)) {
                            // make call to update the requestJson member of existingRequest in the
                            // database to reflect the latest version of the raw json string that we got
                            // directly from IGO LIMS

                            // message handlers will check if there are updates to persist or not
                            requestUpdateQueue.add(request.getLatestRequestMetadata());
                            for (MetadbSample sample : request.getMetaDbSampleList()) {
                                sampleUpdateQueue.add(sample.getLatestSampleMetadata());
                            }
                        } else {
                            LOG.warn("Request already in database - it will not be saved: "
                                    + request.getIgoRequestId());
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
                        MetadbRequest existingRequest =
                                requestService.getMetadbRequestById(requestMetadata.getIgoRequestId());
                        if (existingRequest == null) {
                            LOG.info("Request does not already exist - saving to database: "
                                    + requestMetadata.getIgoRequestId());
                            // persist and handle new request
                            MetadbRequest request =
                                    RequestDataFactory.buildNewRequestFromMetadata(requestMetadata);

                            LOG.info("Publishing new request metadata to " + CMO_REQUEST_UPDATE_TOPIC);
                            requestService.saveRequest(request);
                            messagingGateway.publish(request.getIgoRequestId(),
                                    CMO_REQUEST_UPDATE_TOPIC,
                                    mapper.writeValueAsString(
                                          request.getRequestMetadataList()));
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
                        MetadbSample existingSample = sampleService.getMetadbSampleByRequestAndIgoId(
                                sampleMetadata.getIgoRequestId(), sampleMetadata.getPrimaryId());
                        if (existingSample == null) {
                            LOG.info("Sample metadata does not already exist - persisting to db: "
                                    + sampleMetadata.getPrimaryId());
                            // handle and persist new sample received
                            MetadbSample sample = SampleDataFactory
                                    .buildNewResearchSampleFromMetadata(sampleMetadata.getIgoRequestId(),
                                            sampleMetadata);
                            sampleService.saveSampleMetadata(sample);
                            LOG.info("Publishing metadata history for new sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(sample.getSampleMetadataList()));
                        } else if (sampleService.sampleHasMetadataUpdates(
                                existingSample.getLatestSampleMetadata(), sampleMetadata)) {
                            LOG.info("Found updates for sample - persisting to database: "
                                    + sampleMetadata.getPrimaryId());
                            // persist sample level updates to database and publish
                            // sample metadata history to CMO_SAMPLE_METADATA_UPDATE
                            existingSample.updateSampleMetadata(sampleMetadata);
                            sampleService.saveSampleMetadata(existingSample);
                            LOG.info("Publishing sample-level metadata history for sample: "
                                    + sampleMetadata.getPrimaryId());
                            messagingGateway.publish(CMO_SAMPLE_UPDATE_TOPIC,
                                    mapper.writeValueAsString(existingSample.getSampleMetadataList()));
                        } else {
                            LOG.info("There are no updates to persist for sample: "
                                    + sampleMetadata.getPrimaryId());
                        }
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
            setupCorrectCmoPatientIdHandler(messagingGateway, this);
            initializeNewRequestHandlers();
            initialized = true;
        } else {
            LOG.error("Messaging Handler Service has already been initialized, ignoring request.\n");
        }
    }

    @Override
    public void correctCmoPatientIdHandler(Map<String, String> idCorrectionMap) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("Message Handling Service has not been initialized");
        }
        if (!shutdownInitiated) {
            correctCmoPatientIdQueue.put(idCorrectionMap);
        } else {
            throw new IllegalStateException("Shutdown intiated, not accepting "
                    + "new CMO patient ID correction messages");
        }
    }

    @Override
    public void newRequestHandler(MetadbRequest request) throws Exception {
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
        correctCmoPatientIdShutdownLatch.await();
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

        correctCmoPatientIdShutdownLatch = new CountDownLatch(NUM_NEW_REQUEST_HANDLERS);
        final Phaser correctCmoPtIdPhaser = new Phaser();
        correctCmoPtIdPhaser.register();
        for (int lc = 0; lc < NUM_NEW_REQUEST_HANDLERS; lc++) {
            correctCmoPtIdPhaser.register();
            exec.execute(new CorrectCmoPatientIdReqReplyHandler(correctCmoPtIdPhaser));
        }
        correctCmoPtIdPhaser.arriveAndAwaitAdvance();
    }

    private void setupIgoNewRequestHandler(Gateway gateway, MessageHandlingService messageHandlingService)
        throws Exception {
        gateway.subscribe(IGO_NEW_REQUEST_TOPIC, Object.class, new MessageConsumer() {
            public void onMessage(Message msg, Object message) {
                try {
                    String requestJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    MetadbRequest request = RequestDataFactory.buildNewLimsRequestFromJson(requestJson);
                    LOG.info("Received message on topic: " + IGO_NEW_REQUEST_TOPIC + " and request id: "
                            + request.getIgoRequestId());
                    messageHandlingService.newRequestHandler(request);
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
                    RequestMetadata requestMetadata =
                            RequestDataFactory.buildNewRequestMetadataFromMetadata(requestMetadataJson);
                    LOG.info("Received message on topic: "  + IGO_REQUEST_UPDATE_TOPIC + " and request id: "
                            + requestMetadata.getIgoRequestId());
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
                LOG.info("Received message on topic: " + IGO_SAMPLE_UPDATE_TOPIC);
                try {
                    String sampleMetadataJson = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8), String.class);
                    SampleMetadata sampleMetadata =
                            SampleDataFactory.buildNewSampleMetadatafromJson(sampleMetadataJson);
                    messageHandlingService.sampleUpdateHandler(sampleMetadata);
                } catch (Exception e) {
                    LOG.error("Exception during processing of request update on topic: "
                            + IGO_REQUEST_UPDATE_TOPIC, e);
                }
            }
        });
    }

    private void setupCorrectCmoPatientIdHandler(Gateway gateway,
            MessageHandlingService messageHandlingService) throws Exception {
        gateway.subscribe(CORRECT_CMOPTID_TOPIC, Object.class, new MessageConsumer() {
            @Override
            public void onMessage(Message msg, Object message) {
                LOG.info("Received message on topic: " + CORRECT_CMOPTID_TOPIC);
                Map<String, String> incomingDataMap = new HashMap<>();
                try {
                    // do not log contents of incoming message
                    String incomingDataString = mapper.readValue(
                            new String(msg.getData(), StandardCharsets.UTF_8),
                            String.class);
                    incomingDataMap = mapper.readValue(incomingDataString, Map.class);
                } catch (JsonProcessingException e) {
                    LOG.error("Error processing the incoming data map. "
                            + "Refer to NATS logs for more details.");
                }
                if (incomingDataMap.isEmpty()) {
                    LOG.error("Was not able to deserialize incoming message as instance of Map.class - "
                            + "please confirm manually that the expected message contents were published");
                } else {
                    String oldCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                            incomingDataMap.get("oldId"));
                    String newCmoPatientId = crdbMappingService.getCmoPatientIdByInputId(
                            incomingDataMap.get("newId"));
                    Boolean crdbMappingStatus = Boolean.TRUE;

                    // verify that old and new ids resolve to a valid cmo patient id in crdb service
                    if (oldCmoPatientId == null || oldCmoPatientId.isEmpty()) {
                        LOG.error("Could not resolve 'old' provided patient ID to a CMO patient ID - "
                                + "please manually check the incoming message contents to verify contents");
                        crdbMappingStatus = Boolean.FALSE;
                    }
                    if (newCmoPatientId == null || newCmoPatientId.isEmpty()) {
                        LOG.error("Could not resolve 'new' provided patient ID to a CMO patient ID - "
                                + "please manually check the incoming message contents to verify contents");
                        crdbMappingStatus = Boolean.FALSE;
                    }
                    if (crdbMappingStatus) {
                        // if crdb mapping succeeded then update the incoming data map and
                        // proceed to process message by message handler
                        incomingDataMap.put("oldId", oldCmoPatientId);
                        incomingDataMap.put("newId", newCmoPatientId);
                        try {
                            messageHandlingService.correctCmoPatientIdHandler(incomingDataMap);
                        } catch (Exception e) {
                            LOG.error("Error occurred while adding the CMO Patient ID "
                                    + "correction data to message handler queue");
                        }
                    }
                }
            }
        });
    }
}
