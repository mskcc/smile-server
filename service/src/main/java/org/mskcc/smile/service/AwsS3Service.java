package org.mskcc.smile.service;

import org.mskcc.smile.commons.generated.Smile;
import software.amazon.awssdk.services.s3.S3Client;

/**
 *
 * @author ochoaa
 */
public interface AwsS3Service {
    void initialize() throws Exception;
    S3Client getAwsS3Client();
    void pushTempoSamplesToS3Bucket(Smile.TempoSampleUpdateMessage tempoSampleUpdateMessage);
}
