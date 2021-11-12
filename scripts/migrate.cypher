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
