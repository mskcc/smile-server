package org.mskcc.smile.service;

import org.mskcc.smile.commons.generated.Smile;

/**
 *
 * @author ochoaa
 */
public interface AwsS3Service {
    void initialize() throws Exception;
    void pushTempoSamplesToS3Bucket(Smile.TempoSampleUpdateMessage tempoSampleUpdateMessage);
}
