package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.commons.generated.Smile.TempoSampleUpdateMessage;
import org.mskcc.smile.service.AwsS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 *
 * @author ochoaa
 */
@Component
public class AwsS3ServiceImpl implements AwsS3Service {
    @Value("${s3.aws_region:}")
    private String s3Region;

    @Value("${s3.aws_profile:}")
    private String s3Profile;

    @Value("${s3.aws_tempo_bucket:}")
    private String s3TempoBucket;

    @Value("${s3.aws_role:}")
    private String s3Role;

    @Value("${s3.aws_username:}")
    private String s3Username;

    @Value("${s3.aws_password:}")
    private String s3Password;
    
    @Value("${s3.saml_aws_setup_script:}")
    private String SAML_AWS_SETUP_SCRIPT;

    private static final Log LOG = LogFactory.getLog(AwsS3ServiceImpl.class);
    private S3Client s3;
    private static boolean initialized = Boolean.FALSE;

    @Override
    public void initialize() throws Exception {
        if (!initialized) {
            if (StringUtils.isBlank(SAML_AWS_SETUP_SCRIPT)) {
                throw new RuntimeException("Cannot initialize AWS s3 service without"
                        + " defined ${s3.saml_aws_setup_script}");
            }
            s3 = getAwsS3Client();
//            LOG.info("Running saml2aws setup script: " + SAML_AWS_SETUP_SCRIPT);
//            String[] saml2AwsSetupCmd = {"bash", SAML_AWS_SETUP_SCRIPT};
//            Process process = Runtime.getRuntime().exec(saml2AwsSetupCmd);
//            int exitCode = process.waitFor();
//            System.out.println(process);
//            if (exitCode > 0) {
//                throw new RuntimeException("Failed to run: " + StringUtils.join(saml2AwsSetupCmd, " ", exitCode));
//            } else {
//                initialized = Boolean.TRUE;
//            }
        } else {
            LOG.error("AWS s3 service has already been initialized, ignoring request.");
        }
    }
    
    
    @Override
    public S3Client getAwsS3Client() {
        if (s3 == null || sessionIsExpired(s3)) {
            try {
                generateToken();
                // saml2AWS returns without error, but without being fully setup, lets pause
                Thread.sleep(60000);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException("Error during attempt to authenticate with saml2aws", e);
            }

            Region region = Region.of(s3Region);
            try {
                s3 = S3Client.builder()
                        .region(region)
                        .credentialsProvider(ProfileCredentialsProvider.create(s3Profile))
                        .build();
                return s3;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create s3 client", e);
            }
        }
        return s3;
    }

    @Override
    public void pushTempoSamplesToS3Bucket(TempoSampleUpdateMessage tempoSampleUpdateMessage) {
        S3Client s3Client = getAwsS3Client();
        for (TempoSample sample : tempoSampleUpdateMessage.getTempoSamplesList()) {
            try {
                Boolean success = pushObjectToS3Bucket(s3Client, sample);
                if (!success) {
                    LOG.error("Failed to upload TEMPO sample to s3 bucket: "
                            + sample.getPrimaryId());
                }
            } catch (JsonProcessingException ex) {
                LOG.error("Error during attempt to upload TEMPO sample to s3 bucket", ex);
            }
        }
    }

    private Boolean pushObjectToS3Bucket(S3Client s3Client, TempoSample sample)
            throws JsonProcessingException {
        String bucketKey = sample.getPrimaryId() + "_clinical.json";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3TempoBucket)
                .key(bucketKey)
                .build();
        PutObjectResponse response = s3Client.putObject(putObjectRequest,
                RequestBody.fromBytes(sample.toByteArray()));
        if (response.sdkHttpResponse().isSuccessful()) {
            return Boolean.TRUE;
        } else {
            LOG.error("Failed to upload TEMPO sample: " + sample.getPrimaryId()
                    + ", with SDK HttpResponse: " + response.sdkHttpResponse().statusText());
            return Boolean.FALSE;
        }
    }

    private void generateToken() throws IOException, InterruptedException {
        String usernameArg = new StringBuilder("--username=")
                .append(s3Username).toString();
        String passwordArg = new StringBuilder("--password=")
                .append(s3Password).toString();
        String[] saml2AwsCmd = {"saml2aws", "--role", s3Role, "login", "--force",
            "--mfa=Auto", usernameArg, passwordArg, "--skip-prompt", "--session-duration=3600"};
        Process process = Runtime.getRuntime().exec(saml2AwsCmd);
        int exitCode = process.waitFor();
        if (exitCode > 0) {
            throw new RuntimeException("Failed to run: " + StringUtils.join(saml2AwsCmd, " "));
        }
    }

    private Boolean sessionIsExpired(S3Client s3Client) {
        if (s3Client == null) {
            return Boolean.TRUE;
        }
        try {
            IdentityProvider credentialsProvider
                    = s3Client.serviceClientConfiguration().credentialsProvider();
            AwsSessionCredentials credentials = (AwsSessionCredentials)
                    credentialsProvider.resolveIdentity().join();
            Optional<Instant> expirationTime = credentials.expirationTime();
            Instant now = Instant.now();
            Duration timeLeft = Duration.between(now, expirationTime.get());
            if (timeLeft.isPositive()) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        } catch (Exception e) {
            throw new RuntimeException("Error resolving credentials and/or resolving "
                    + "the remaining session duration.", e);
        }
    }
}
