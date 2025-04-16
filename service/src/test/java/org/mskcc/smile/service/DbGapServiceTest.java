package org.mskcc.smile.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mskcc.smile.model.DbGap;
import org.mskcc.smile.model.SmileRequest;
import org.mskcc.smile.model.json.DbGapJson;
import org.mskcc.smile.persistence.neo4j.DbGapRepository;
import org.mskcc.smile.persistence.neo4j.SmileRequestRepository;
import org.mskcc.smile.persistence.neo4j.SmileSampleRepository;
import org.mskcc.smile.service.util.RequestDataFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 *
 * @author ochoaa
 */
@SpringBootTest(
        classes = SmileTestApp.class,
        properties = {"spring.neo4j.authentication.username:neo4j"}
)
@Testcontainers
@Import(MockDataUtils.class)
public class DbGapServiceTest {
    @Autowired
    private MockDataUtils mockDataUtils;

    @Autowired
    private SmileRequestService requestService;

    @Autowired
    private SmileSampleService sampleService;

    @Autowired
    private DbGapService dbGapService;


    // required for all test classes
    @Container
    private static final Neo4jContainer<?> databaseServer = new Neo4jContainer<>(
            DockerImageName.parse("neo4j:5.19.0"))
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        databaseServer.start();
        registry.add("spring.neo4j.authentication.password", databaseServer::getAdminPassword);
        registry.add("spring.neo4j.uri", databaseServer::getBoltUrl);
    }

    @TestConfiguration
    static class Config {
        @Bean
        public org.neo4j.ogm.config.Configuration configuration() {
            return new org.neo4j.ogm.config.Configuration.Builder()
                    .uri(databaseServer.getBoltUrl())
                    .credentials("neo4j", databaseServer.getAdminPassword())
                    .build();
        }

        @Bean
        public SessionFactory sessionFactory() {
            // with domain entity base package(s)
            return new SessionFactory(configuration(), "org.mskcc.smile.persistence");
        }
    }

    @Autowired
    private SessionFactory sessionFactory;

    private final SmileRequestRepository requestRepository;
    private final SmileSampleRepository sampleRepository;
    private final DbGapRepository dbgapRepository;

    /**
     * Initializes the Neo4j repositories.
     * @param requestRepository
     * @param sampleRepository
     * @param dbgapRepository
     */
    @Autowired
    public DbGapServiceTest(SmileRequestRepository requestRepository,
            SmileSampleRepository sampleRepository,DbGapRepository dbgapRepository) {
        this.requestRepository = requestRepository;
        this.sampleRepository = sampleRepository;
        this.dbgapRepository = dbgapRepository;
    }

    /**
     * Persists the Mock Request data to the test database.
     * @throws Exception
     */
    @BeforeEach
    public void initializeMockDatabase() throws Exception {
        Session session = sessionFactory.openSession();
        session.purgeDatabase();

        // mock request id: MOCKREQUEST1_B
        MockJsonTestData request1Data = mockDataUtils.mockedRequestJsonDataMap
                .get("mockIncomingRequest1JsonDataWith2T2N");
        SmileRequest request1 =
                RequestDataFactory.buildNewLimsRequestFromJson(request1Data.getJsonString());
        requestService.saveRequest(request1);
    }

    @Test
    public void testDbGapMerge() throws Exception {
        // favorite mock sample id
        String samplePrimaryId = "MOCKREQUEST1_B_1";
        Assertions.assertTrue(sampleService.sampleExistsByInputId(samplePrimaryId));

        // now test save and fetch of dbgap node
        DbGapJson dbGapJson = new DbGapJson(samplePrimaryId, "mockDbgapStudyId.v1");
        dbGapService.updateDbGap(dbGapJson);
        DbGap dbGapNode = dbGapService.getDbGapBySamplePrimaryId(samplePrimaryId);
        Assertions.assertEquals("mockDbgapStudyId.v1", dbGapNode.getDbGapStudy());

        //pretend that the dbgap study is updated to v2
        DbGapJson dbGapJson2 = new DbGapJson(samplePrimaryId, "mockDbgapStudyId.v2");
        dbGapService.updateDbGap(dbGapJson2);
        DbGap dbGapNode2 = dbGapService.getDbGapBySamplePrimaryId(samplePrimaryId);
        Assertions.assertEquals("mockDbgapStudyId.v2", dbGapNode2.getDbGapStudy());
    }
}
