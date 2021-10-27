package org.mskcc.cmo.metadb.service;

public interface CRDBIdMappingService {
    Object getCountOfClinicalSamples();
    Object getCmoPatientIdbyDmpId(String dmpId);
}
