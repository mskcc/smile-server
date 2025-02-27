package org.mskcc.smile.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.messaging.utils.SSLUtils;
import org.mskcc.smile.commons.JsonComparator;
import org.mskcc.smile.commons.impl.JsonComparatorImpl;
import org.mskcc.smile.persistence.jpa.CrdbRepository;
import org.mskcc.smile.service.impl.ClinicalMessageHandlingServiceImpl;
import org.mskcc.smile.service.impl.CohortCompleteServiceImpl;
import org.mskcc.smile.service.impl.CorrectCmoPatientHandlingServiceImpl;
import org.mskcc.smile.service.impl.CrdbMappingServiceImpl;
import org.mskcc.smile.service.impl.PatientServiceImpl;
import org.mskcc.smile.service.impl.RequestReplyHandlingServiceImpl;
import org.mskcc.smile.service.impl.RequestServiceImpl;
import org.mskcc.smile.service.impl.ResearchMessageHandlingServiceImpl;
import org.mskcc.smile.service.impl.SampleServiceImpl;
import org.mskcc.smile.service.impl.TempoMessageHandlingServiceImpl;
import org.mskcc.smile.service.impl.TempoServiceImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author ochoaa
 */
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.smile.commons.*", "org.mskcc.smile.*"})
@EntityScan(basePackages = "org.mskcc.smile.model")
@EnableNeo4jRepositories(basePackages = "org.mskcc.smile.persistence.neo4j")
public class SmileTestApp {
    @Bean
    public SmileRequestService requestService() {
        return new RequestServiceImpl();
    }

    @Bean
    public SmileSampleService sampleService() {
        return new SampleServiceImpl();
    }

    @Bean
    public SmilePatientService patientService() {
        return new PatientServiceImpl();
    }

    @Bean
    public JsonComparator jsonComparator() {
        return new JsonComparatorImpl();
    }

    @Bean
    public TempoService tempoService() {
        return new TempoServiceImpl();
    }

    @Bean
    public CohortCompleteService cohortCompleteService() {
        return new CohortCompleteServiceImpl();
    }

    @Autowired
    public void logger() {
        Logger logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.drivers.bolt.response.BoltResponse.unrecognized");
        logger.setLevel(Level.OFF);
        logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.context.GraphEntityMapper");
        logger.setLevel(Level.OFF);
        logger = (Logger) LoggerFactory.getLogger("org.neo4j.ogm.context.EntityGraphMapper");
        logger.setLevel(Level.OFF);
    }

    @MockBean
    public CrdbRepository crdbRepository;

    @MockBean
    public CrdbMappingServiceImpl crdbMappingService;

    @MockBean
    public Gateway messagingGateway;

    @MockBean
    public SSLUtils sslUtils;

    @MockBean
    public ResearchMessageHandlingServiceImpl researchMessageHandlingService;

    @MockBean
    public ClinicalMessageHandlingServiceImpl clinicalMessageHandlingService;

    @MockBean
    public CorrectCmoPatientHandlingServiceImpl patientCorrectionHandlingService;

    @MockBean
    public RequestReplyHandlingServiceImpl requestReplyHandlingService;

    @MockBean
    public TempoMessageHandlingServiceImpl tempoMessageHandlingService;

}

