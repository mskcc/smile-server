package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.smile.commons.generated.Smile.TempoSample;
import org.mskcc.smile.commons.generated.Smile.TempoSampleUpdateMessage;
import org.mskcc.smile.service.AwsS3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
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

    @Value("${ENABLE_S3_UPLOAD:false}")
    private Boolean sysEnableS3Upload;

    @Value("${s3.enable_upload:false}")
    private Boolean localEnableS3Upload;

    private static final Log LOG = LogFactory.getLog(AwsS3ServiceImpl.class);
    private S3Client s3;
    private static Instant s3SessionTimestamp;

    @Override
    public void pushTempoSamplesToS3Bucket(TempoSampleUpdateMessage tempoSampleUpdateMessage) {
        if (sysEnableS3Upload || localEnableS3Upload) {
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
                } catch (InvalidProtocolBufferException ex) {
                    LOG.error(ex);
                }
            }
        } else {
            LOG.info("Upload to s3 bucket disabled - enable by setting property 's3.enable_upload=true'");
        }
    }

    private Boolean pushObjectToS3Bucket(S3Client s3Client, TempoSample sample)
            throws JsonProcessingException, InvalidProtocolBufferException {
        String bucketKey = sample.getDmpSampleId() + "_clinical.json";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3TempoBucket)
                .key(bucketKey)
                .contentType("application/json")
                .build();

        // protobuf json printer
        JsonFormat.Printer printer = JsonFormat.printer().preservingProtoFieldNames();
        PutObjectResponse response = s3Client.putObject(putObjectRequest,
                RequestBody.fromString(printer.print(sample.toBuilder().build())));
        if (response.sdkHttpResponse().isSuccessful()) {
            return Boolean.TRUE;
        } else {
            LOG.error("Failed to upload TEMPO sample: " + sample.getPrimaryId()
                    + ", with SDK HttpResponse: " + response.sdkHttpResponse().statusText());
            return Boolean.FALSE;
        }
    }

    private S3Client getAwsS3Client() {
        if (s3 == null || sessionIsExpired(s3)) {
            try {
                System.out.println("Creating or refreshing s3 session.");
                s3SessionTimestamp = Instant.now();
                generateToken();
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

    private void generateToken() throws IOException, InterruptedException {
        String usernameArg = new StringBuilder("--username=")
                .append(s3Username).toString();
        String passwordArg = new StringBuilder("--password=")
                .append(s3Password).toString();
        String[] saml2AwsCmd = {"saml2aws", "login", "--profile", s3Profile, "--role", s3Role, "--force",
            "--mfa=Auto", usernameArg, passwordArg, "--skip-prompt", "--session-duration=3600",
            "--idp-account", s3Profile};
        System.out.println("Running SAML2AWS login...");

        ProcessBuilder loginBuilder = new ProcessBuilder(saml2AwsCmd);
        Process loginProcess = loginBuilder.start();
        // Read the output of the login process (for debugging or to check success/failure)
        BufferedReader loginReader = new BufferedReader(
                new InputStreamReader(loginProcess.getInputStream()));
        String line;
        while ((line = loginReader.readLine()) != null) {
            System.out.println("SAML2AWS Login Output: " + line);
        }

        int exitCode = loginProcess.waitFor();
        System.out.println("SAML2AWS Login Exit Code: " + exitCode);

        if (exitCode > 0) {
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(loginProcess.getErrorStream()));
            String err;
            while ((err = errorReader.readLine()) != null) {
                System.out.println("SAML2AWS Error Output: " + err);
            }
            throw new RuntimeException("Failed to run: " + StringUtils.join(saml2AwsCmd, " "));
        }
    }

    /**
     * Simple session expiration check.
     * @param s3Client
     * @return Boolean
     */
    private Boolean sessionIsExpired(S3Client s3Client) {
        if (s3Client == null) {
            return Boolean.TRUE;
        }
        Instant expirationTime = s3SessionTimestamp.plusSeconds(3600);
        return Instant.now().isAfter(expirationTime);
    }
}
