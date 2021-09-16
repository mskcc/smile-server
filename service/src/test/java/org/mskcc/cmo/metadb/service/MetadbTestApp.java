package org.mskcc.cmo.metadb.service;

import org.mskcc.cmo.common.MetadbJsonComparator;
import org.mskcc.cmo.common.impl.MetadbJsonComparatorImpl;
import org.mskcc.cmo.metadb.service.impl.MetadbRequestServiceImpl;
import org.mskcc.cmo.metadb.service.impl.SampleServiceImpl;
import org.mskcc.cmo.metadb.service.util.RequestStatusLogger;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 *
 * @author ochoaa
 */
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.cmo.common.*", "org.mskcc.cmo.metadb.*"})
@EntityScan(basePackages = "org.mskcc.cmo.metadb.model")
@EnableNeo4jRepositories(basePackages = "org.mskcc.cmo.metadb.persistence")
public class MetadbTestApp {
    @Bean
    public MetadbRequestService requestService() {
        return new MetadbRequestServiceImpl();
    }

    @Bean
    public SampleService sampleService() {
        return new SampleServiceImpl();
    }

    @Bean
    public MetadbJsonComparator metadbJsonComparator() {
        return new MetadbJsonComparatorImpl();
    }

    @MockBean
    public RequestStatusLogger requestStatusLogger;
}