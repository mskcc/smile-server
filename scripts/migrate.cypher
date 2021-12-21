// migrate.cypher documents database migrations executed after metadb.schema_version: v1.1


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