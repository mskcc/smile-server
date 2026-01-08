// migrate.cypher documents database migrations executed after smile.schema_version: v1.1


// SCHEMA VERSION: v1.2
// ------------------------------------------------------------

// adds sampleCategory property to sample nodes
MATCH (s: Sample)
WHERE s.sampleCategory IS NULL
SET s.sampleCategory = "research"
RETURN true;

// updates sampletMetadta property 'igoId' to 'primaryId'
MATCH (sm: SampleMetadata)
WHERE sm.primaryId IS NULL
SET sm.primaryId = sm.igoId
REMOVE sm.igoId
RETURN true;


// SCHEMA VERSION: v2.0
// ------------------------------------------------------------

// request-level property name changes
// - projectId --> igoProjectId
// - requestId --> igoRequestId
// - recipe --> genePanel
MATCH (r: Request)
WHERE r.igoProjectId IS NULL
SET r.igoProjectId = r.projectId
REMOVE r.projectId
RETURN true;

MATCH (r: Request)
WHERE r.igoRequestId IS NULL
SET r.igoRequestId = r.requestId
REMOVE r.requestId
RETURN true;

MATCH (r: Request)
WHERE r.genePanel IS NULL
SET r.genePanel = r.recipe
REMOVE r.recipe
RETURN true;

// sample metadata-level property name changes
// - cmoSampleClass --> sampleType
// - specimenType --> sampleClass
// - oncoTreeCode --> oncotreeCode
// - recipe --> genePanel

// remove existing sample type data
MATCH (sm: SampleMetadata)
REMOVE sm.sampleType
RETURN true;

MATCH (sm: SampleMetadata)
WHERE sm.sampleType IS  NULL
SET sm.sampleType = sm.cmoSampleClass
REMOVE sm.cmoSampleClass
RETURN true;

MATCH (sm: SampleMetadata)
WHERE sm.sampleClass IS  NULL
SET sm.sampleClass = sm.specimenType
REMOVE sm.specimenType
RETURN true;

MATCH (sm: SampleMetadata)
WHERE sm.oncotreeCode IS  NULL
SET sm.oncotreeCode = sm.oncoTreeCode
REMOVE sm.oncoTreeCode
RETURN true;

MATCH (sm: SampleMetadata)
WHERE sm.genePanel IS  NULL
SET sm.genePanel = sm.recipe
REMOVE sm.recipe
RETURN true;

// add datasource to sample-level
MATCH (s: Sample)
WHERE s.datasource IS NULL
SET s.datasource = "igo"
RETURN true;


// SCHEMA VERSION: v2.1
// ------------------------------------------------------------

// property name changes
// - metaDbRequestId --> smileRequestId
// - metaDbSampleId --> smileSampleId
// - metaDbPatientId --> smilePatientId

MATCH (r: Request)
WHERE r.smileRequestId IS NULL
SET r.smileRequestId = r.metaDbRequestId
REMOVE r.metaDbRequestId
RETURN true;

MATCH (p: Patient)
WHERE p.smilePatientId IS NULL
SET p.smilePatientId = p.metaDbPatientId
REMOVE p.metaDbPatientId
RETURN true;

MATCH (s: Sample)
WHERE s.smileSampleId IS NULL
SET s.smileSampleId = s.metaDbSampleId
REMOVE s.metaDbSampleId
RETURN true;

// populate research samples with additionalProperties = {"igoRequestId": [requestId], "isCmoSample": [isCmoRequest]}
MATCH (sm: SampleMetadata)<-[:HAS_METADATA]-(s: Sample {sampleCategory: "research"})<-[:HAS_SAMPLE]-(r: Request)
CALL {
	WITH r
	MATCH (sm2: SampleMetadata)<-[:HAS_METADATA]-(s2:Sample)<-[:HAS_SAMPLE]-(r)
	RETURN "{\"igoRequestId\": \"" + r.igoRequestId + "\", \"isCmoSample\": \"" + r.isCmoRequest + "\"}" as additionalProperties
}
SET sm.additionalProperties = additionalProperties
RETURN true



// SCHEMA VERSION: v2.3
// ------------------------------------------------------------

// add property "revisable" to request and sample nodes
MATCH (s: Sample)
WHERE s.revisable IS NULL
SET s.revisable = true
RETURN true;

MATCH (r: Request)
WHERE r.revisable IS NULL
SET r.revisable = true
RETURN true;

// add node and relationship for sample metadata and request metadata nodes
MATCH (rm: RequestMetadata)
CREATE (st: Status {validationStatus: true, validationReport: ""})<-[:HAS_STATUS]-(rm)
RETURN true

MATCH (sm: SampleMetadata)
CREATE (st: Status {validationStatus: true, validationReport: ""})<-[:HAS_STATUS]-(sm)
RETURN true

// add property "smileTempoId" to Tempo nodes
MATCH (t: Tempo)
SET t.smileTempoId = apoc.create.uuid()
RETURN true

// flatten pmUsers and endUsers on CohortComplete nodes
MATCH (cc:CohortComplete)
WITH cc, REDUCE(x="", v IN cc.pmUsers | x + v + ", ") AS newPmUsers
SET cc.pmUsers = LEFT(newPmUsers, SIZE(newPmUsers)-2)
RETURN cc

MATCH (cc:CohortComplete)
WITH cc, REDUCE(x="", v IN cc.endUsers | x + v + ", ") AS newEndUsers
SET cc.endUsers = LEFT(newEndUsers, SIZE(newEndUsers)-2)
RETURN cc

// add properties "initialPipelineRunDate" and "embargoDate" to Tempo nodes for samples that belong to Tempo cohorts
MATCH (t:Tempo)<-[:HAS_TEMPO]-(s:Sample)<-[:HAS_COHORT_SAMPLE]-(c:Cohort)-[:HAS_COHORT_COMPLETE]->(cc:CohortComplete)
WITH t,s,c,cc, 
    apoc.date.parse(min(cc.date), "ms", "yyyy-MM-dd HH:mm") as initRunDatetime, 
    apoc.date.parse(min(cc.date), "ms", "yyyy-MM-dd HH:mm") as minDate
with t,s,c,cc,
    initRunDatetime, 
    datetime(apoc.date.format(minDate, "ms", "yyyy-MM-dd")) + Duration({months:18}) as embargoDatetime
SET 
    t.initialPipelineRunDate = apoc.date.format(initRunDatetime, "ms", "yyyy-MM-dd"), 
    t.embargoDate = apoc.temporal.format(embargoDatetime, 'YYYY-MM-dd')
RETURN s.smileSampleId, collect(DISTINCT cc.date), min(cc.date), t.initialPipelineRunDate, t.embargoDate

// resolve 'igoDeliveryDate' from requestJson string
MATCH (r:Request)
WITH r, apoc.convert.fromJsonMap(r.requestJson).deliveryDate as deliveryDate
WHERE deliveryDate IS NOT NULL
SET r.igoDeliveryDate = toInteger(deliveryDate)
RETURN r.igoRequestId, r.igoDeliveryDate, deliveryDate

