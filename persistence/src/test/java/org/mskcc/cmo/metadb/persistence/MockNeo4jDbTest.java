package org.mskcc.cmo.metadb.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mskcc.cmo.common.enums.NucleicAcid;
import org.mskcc.cmo.common.enums.SpecimenType;
import org.mskcc.cmo.metadb.model.MetaDbPatient;
import org.mskcc.cmo.metadb.model.MetaDbSample;
import org.mskcc.cmo.metadb.model.PatientAlias;
import org.mskcc.cmo.metadb.model.SampleAlias;
import org.mskcc.cmo.metadb.model.SampleMetadata;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * @author ochoaa
 */
@Testcontainers
@DataNeo4jTest
public class MockNeo4jDbTest {
	@Container
	private static final Neo4jContainer databaseServer = new Neo4jContainer<>()
		.withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
	// end::copy-plugin[]

	// tag::sdn-neo4j-testcontainer-setup[]
	@TestConfiguration // <2>
	static class Config {

		@Bean // <3>
		public org.neo4j.ogm.config.Configuration configuration() {
			return new org.neo4j.ogm.config.Configuration.Builder()
				.uri(databaseServer.getBoltUrl())
				.credentials("neo4j", databaseServer.getAdminPassword())
				.build();
		}
	}

    private final MetaDbSampleRepository sampleRepository;
    @Autowired
    public MockNeo4jDbTest(MetaDbSampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }
    
//    private static final String TEST_DATA = "CREATE (s:SampleAlias {value: 'C-1235-X002-r', namespace: 'igoId'})";
//    
//    @BeforeAll
//    public static void prepareTestData() {
//        AuthToken token = AuthTokens.basic("neo4j", databaseServer.getAdminPassword());
//        Driver driver = GraphDatabase.driver(databaseServer.getBoltUrl(), token);
//        Session session = driver.session();
//        session.writeTransaction(new TransactionWork() {
//            @Override
//            public Object execute(Transaction t) {
//                return t.run(TEST_DATA);
//            }
//        });
//    }

    // not sure if we need this
//    private static SessionFactory sessionFactory;
//    @BeforeAll
//    public void prepareSessionFactory() {
//        org.neo4j.ogm.config.Configuration ogmConfiguration = new Configuration.Builder()
//        .uri(databaseServer.getBoltUrl())
//        .credentials("neo4j", databaseServer.getAdminPassword())
//        .build();
//        sessionFactory = new SessionFactory(
//            ogmConfiguration,
//            "org.mskcc.cmo.metadb.persistence");
//    }
    
    @Test
    public void testDatabaseServer() {
        System.out.println("\n\n\n\nDatabase server info:");
    }
    
    @Test
    public void testSaveMethod() {
//        assertThat(sampleRepository.findAll()).isEmpty();
        SampleMetadata metadata = getSampleMetadata("request_1234", "C-1235", "C-1235", SpecimenType.ORGANOID, NucleicAcid.DNA);
        SampleAlias sample = new SampleAlias("C-1235-X002-r", "igoId");
        MetaDbPatient patient = new MetaDbPatient();
        patient.setMetaDbPatientId(UUID.randomUUID());
        patient.addPatientAlias(new PatientAlias("C-1235", "igoId"));
        
        MetaDbSample mdbSample = new MetaDbSample();
        mdbSample.addSampleAlias(sample);
        mdbSample.setMetaDbSampleId(UUID.randomUUID());
        mdbSample.setPatient(patient);
        mdbSample.addSampleMetadata(metadata);
        sampleRepository.save(mdbSample);
        
        MetaDbSample result = sampleRepository.findMetaDbSampleById(mdbSample.getMetaDbSampleId());
        assertThat(result).isNotNull();
//        System.out.println("\n\n\nsample result:");
//        System.out.println(sample.getSampleId());
//        assertThat(sample).isNotNull();
//        System.out.println("SAVED SAMPLE IGO ID: " + sample.getSampleId());
    }
    
    private SampleMetadata getSampleMetadata(String requestId, String igoId, String cmoPatientId,
            SpecimenType specimenType, NucleicAcid naToExtract) {
        SampleMetadata sample = new SampleMetadata();
        sample.setRequestId(requestId);
        sample.setIgoId(igoId);
        sample.setCmoPatientId(cmoPatientId);
        sample.setSpecimenType(specimenType.getValue());

        Map<String, String> cmoSampleIdFields = new HashMap<>();
        cmoSampleIdFields.put("naToExtract", naToExtract.getValue());
        sample.setCmoSampleIdFields(cmoSampleIdFields);
        return sample;
    }
}
