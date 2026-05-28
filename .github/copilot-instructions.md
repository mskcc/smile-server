# Copilot Instructions for SMILE Server

## Project Overview
SMILE (CMO SMILE Server) is a Spring Boot 3.3.3 / Java 21 microservice that receives LIMS request messages via NATS, persists data to a Neo4j graph database, and publishes updated messages to downstream subscribers. It is part of a larger genomic data processing ecosystem at MSK.

---

## Build, Test, and Lint

```bash
# Build all modules (skips tests)
mvn clean install -DskipTests

# Build and run all tests
mvn clean install

# Run tests for a specific module
mvn test -pl service

# Run a single test class
mvn test -pl service -Dtest=RequestServiceTest

# Run a single test method
mvn test -pl service -Dtest=RequestServiceTest#testMethodName

# Run Checkstyle validation
mvn checkstyle:checkstyle

# View Checkstyle results
cat checkstyle_report.txt
```

The app requires `src/main/resources/application.properties` (copy from `application.properties.EXAMPLE` and fill in values) to run locally. It will not start without NATS and Neo4j connections.

---

## Multi-Module Architecture

```
smile-server (parent)
├── model/       → Neo4j OGM entities, converters, DTOs
├── persistence/ → Neo4j + Databricks JDBC repositories
├── service/     → Business logic, NATS message handlers
├── web/         → Spring MVC REST controllers
└── server/      → Spring Boot entry point, Neo4j config
```

**Dependency direction**: `web → service → persistence → model`. The `server` module depends on all others. Never introduce reverse dependencies.

**Main entry point**: `server/src/main/java/org/mskcc/smile/SmileApp.java` — implements `CommandLineRunner`; `run()` connects to NATS and initializes all message handler services.

---

## Key Conventions

### Naming
- Entities: `Smile[Type]` (e.g., `SmileRequest`, `SmileSample`, `SmilePatient`)
- Service interfaces: `[Type]Service`; implementations: `[Type]ServiceImpl` annotated `@Component`
- REST controllers: `[Type]Controller` with `@RestController @CrossOrigin(origins = "*")`
- API response DTOs (in `model/web`): `Published[Type]` or `[Type]Summary` — these hide internal graph structure
- Neo4j repositories: extend `Neo4jRepository` and live in `persistence/src/…/persistence/neo4j/`

### Root package
All code lives under `org.mskcc.smile`.

### Service layer
- Service interfaces are defined in `service/src/main/java/…/service/`; implementations in `.../service/impl/`
- Mutating service methods use `@Transactional(rollbackFor = {Exception.class})`
- `@Lazy` is used on `@Autowired` fields where circular dependencies exist — this is intentional; the JVM flag `-Dspring.main.allow-circular-references=true` is set at runtime

### Neo4j / OGM entities
- Entities are annotated `@NodeEntity(label = "...")` (OGM 4.x API, not Spring Data Neo4j annotations)
- IDs use `@Id @GeneratedValue(strategy = UuidStrategy.class)` — UUID strings, not numeric
- Relationships use `@Relationship(type = "HAS_SAMPLE", direction = OUTGOING)` etc.
- Complex property types (Map, arrays, domain objects) require a custom converter in `model/.../converter/` implementing `AttributeConverter<T, String>` and annotated `@Convert(...)`
- The `SessionFactory` is manually configured in `SmileConfiguration.java`; entity scanning is set to `org.mskcc.smile.model`

### Custom Cypher queries
Repositories use `@Query` with Cypher directly — prefer this over derived query methods for anything non-trivial. Always `OPTIONAL MATCH` for nullable relationships to avoid dropping nodes.

### Message handling
Each NATS topic has a dedicated handler service (e.g., `ResearchMessageHandlingService`, `ClinicalMessageHandlingService`). New message types should follow this pattern: add a new handler interface + impl in `service`, register it in `SmileApp.run()`.

### REST API
- All controllers return `ResponseEntity<?>` — never raw objects
- Swagger/OpenAPI annotations (`@Operation`, `@Parameter`, `@Tag` from SpringDoc 2.x) are used on all public endpoints
- Date-range endpoints accept `LocalDate` parameters serialized as ISO strings

### Testing
- Unit tests use JUnit 5 + Mockito and live in `service/src/test/`
- Integration tests use **TestContainers** with a real Neo4j container — see `SmileTestApp` for the test Spring context
- Shared test fixtures are in `MockDataUtils.java` and `MockJsonTestData.java`
- Test JSON fixtures live under `src/test/resources/`

### Code style
- **Google Java Style** enforced via Checkstyle (max line length: 110 chars)
- CI (CircleCI) runs `mvn checkstyle:checkstyle` on every build — violations will fail the pipeline

---

## Infrastructure Dependencies

| Dependency | Role |
|---|---|
| Neo4j | Primary graph database (OGM 4 / driver 5.x) |
| NATS | Async messaging (with optional TLS via keystore/truststore) |
| Databricks JDBC (HikariCP) | Analytical/reporting queries via `DatabricksRepository` |
| AWS SDK v2 | S3 integration used in service layer |

---

## Message Handler Flows

Each handler impl subscribes/replies on NATS topics (configured via `application.properties`) and processes messages off an internal `BlockingQueue` via a thread pool. The property keys below correspond to the `@Value` annotations in each class.

### `ResearchMessageHandlingServiceImpl`
Handles IGO research requests and metadata updates.

| Direction | Property key | Content | Action |
|---|---|---|---|
| **IN** | `igo.new_request_topic` | Full `SmileRequest` JSON from LIMS | Persist new request; **publish** to `consumers.new_request_topic` |
| **IN** | `igo.promoted_request_topic` | Full `SmileRequest` JSON | Persist new request; **publish** to `consumers.promoted_request_topic` |
| **IN** | `smile.igo_request_update_topic` | `RequestMetadata` JSON | Update request metadata; **publish** history to `smile.cmo_request_update_topic` |
| **IN** | `smile.igo_sample_update_topic` | `SampleMetadata` JSON | Update sample metadata; **publish** to `smile.cmo_sample_update_topic`; if sample has Tempo data, also triggers S3 upload |
| **OUT** | `consumers.new_request_topic` | `PublishedSmileRequest` JSON | Downstream consumers of new research requests |
| **OUT** | `consumers.promoted_request_topic` | `PublishedSmileRequest` JSON | Downstream consumers of promoted requests |
| **OUT** | `smile.cmo_request_update_topic` | Full `RequestMetadata` history list | Consumers tracking request-level metadata changes |
| **OUT** | `smile.cmo_sample_update_topic` | `SmileSample` JSON (Tempo field nulled) | Consumers tracking sample-level metadata changes |
| **REQ→** | `request_reply.cmo_label_generator_topic` | `SampleMetadata` JSON | Request new CMO sample label (used during patient ID correction) |

If a new request arrives on `igo.new_request_topic` but the request already exists, it is re-routed internally to the request-update and sample-update queues (`fromLims = true`).

---

### `ClinicalMessageHandlingServiceImpl`
Handles DMP clinical sample ingest and updates.

| Direction | Property key | Content | Action |
|---|---|---|---|
| **IN** | `smile.dmp_new_sample_topic` | `DmpSampleMetadata` JSON | Resolve CMO patient ID via `PatientIdMappingService`; if new → persist + **publish** to `consumers.dmp_new_sample_topic`; if updated → persist + **publish** to `consumers.dmp_sample_update_topic` |
| **IN** | `smile.dmp_sample_update_topic` | `DmpSampleMetadata` JSON | Same logic as new-sample topic (idempotent upsert) |
| **OUT** | `consumers.dmp_new_sample_topic` | `SmileSample` JSON | Downstream consumers of new clinical samples |
| **OUT** | `consumers.dmp_sample_update_topic` | `SmileSample` JSON | Downstream consumers of clinical sample updates |

Both inbound topics are handled by the same upsert logic; the distinction is only in which outbound topic is used.

---

### `TempoMessageHandlingServiceImpl`
Handles all WES pipeline completion events, cohort data, sample billing, and cBioPortal/S3 uploads.

| Direction | Property key | Content | Action |
|---|---|---|---|
| **IN** | `tempo.wes_bam_complete_topic` | `{primaryId, BamComplete}` | Persist BAM complete event to Tempo node |
| **IN** | `tempo.wes_qc_complete_topic` | `{primaryId, QcComplete}` | Persist QC complete event to Tempo node |
| **IN** | `tempo.wes_maf_complete_topic` | `{primaryId, MafComplete}` | Persist MAF complete event; resolves normal sample primary ID if given as CMO label |
| **IN** | `tempo.wes_cohort_complete_topic` | `CohortCompleteJson` | Persist new/updated cohort; upload tumor samples to S3 |
| **IN** | `tempo.sample_billing_topic` | `SampleBillingJson` | Update billing on Tempo node; **publish** `TempoSampleUpdateMessage` (protobuf) to `tempo.update_samples_embargo_topic`; push to S3 |
| **IN** | `tempo.update_samples_embargo_topic` | List of sample primary IDs | Set access level to PUBLIC; **publish** `TempoSampleUpdateMessage` to same topic; push to S3 |
| **IN** | `tempo.upload_samples_to_s3_topic` | List of sample primary IDs | Upload Tempo sample data to AWS S3 bucket for cBioPortal |
| **IN** | `tempo.update_cohort_sub_topic` | `CohortCompleteJson` | Persist cohort complete updates; **publish** `TempoCohortUpdate` (protobuf) to `tempo.update_cohort_pub_topic` |
| **IN** | `tempo.provisional_cohort_sub_topic` | `CohortCompleteJson` | Persist provisional cohort (no S3 upload) |
| **OUT** | `tempo.update_samples_embargo_topic` | `TempoSampleUpdateMessage` protobuf | cBioPortal consumers; also directly pushed to AWS S3 via `AwsS3Service` |
| **OUT** | `tempo.update_cohort_pub_topic` | `TempoCohortUpdate` protobuf | TEMPO bot consumers for cohort delivery notifications |

All complete-event handlers (BAM/QC/MAF) are idempotent — they check for duplicates before merging. `TempoSampleUpdateMessage` and `TempoCohortUpdate` use Protobuf (generated in `smile-commons`).

---

### `CorrectCmoPatientHandlingServiceImpl`
Implements `CorrectCmoPatientHandlingService` (`service/src/main/java/org/mskcc/smile/service/CorrectCmoPatientHandlingService.java`).
Handles patient ID corrections (old CMO ID → new CMO ID).

| Direction | Property key | Content | Action |
|---|---|---|---|
| **IN** | `smile.correct_cmoptid_topic` | `{"oldId": "...", "newId": "..."}` | Resolve both IDs via `PatientIdMappingService`; merge aliases to new patient; re-label all research samples via CMO label generator request-reply; delete old patient node; **publish** updated samples |
| **OUT** | `smile.cmo_sample_update_topic` | `SmileSample` JSON | Publishes updated metadata for samples that were swapped to the new patient (excludes samples already on the new patient before the correction) |
| **REQ→** | `request_reply.cmo_label_generator_topic` | `SampleMetadata` JSON | Synchronous request-reply to get new CMO sample label per research sample |

---

### `RequestReplyHandlingServiceImpl`
(`service/src/main/java/org/mskcc/smile/service/impl/RequestReplyHandlingServiceImpl.java`)
Synchronous request-reply handlers for external service queries. Uses `gateway.replySub()` (not `subscribe()`).

| Direction | Property key | Input | Reply |
|---|---|---|---|
| **REQ/REP** | `request_reply.patient_samples_topic` | CMO patient ID string | JSON array of `SampleMetadata` for all research samples under that patient |
| **REQ/REP** | `request_reply.samples_by_cmo_label_topic` | CMO sample label string | JSON array of `SampleMetadata` matching the label |
| **REQ/REP** | `request_reply.samples_by_alt_id_topic` | Alt ID string | JSON array of `SampleMetadata` matching the alt ID |
| **REQ/REP** | `request_reply.patient_mapping_topic` | Any patient input ID | `PatientIdTriplet` JSON (CMO ID, DMP ID, MRN) resolved via Databricks |

---

## Schema Migrations
Neo4j schema migrations are tracked in `scripts/migrate.cypher`. When modifying entity labels or property names, add a versioned migration block to this file following the existing format (see `v2.0`, `v2.3` comments).
