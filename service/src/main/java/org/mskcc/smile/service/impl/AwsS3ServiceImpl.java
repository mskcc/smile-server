package org.mskcc.smile.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
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

    private static final Log LOG = LogFactory.getLog(AwsS3ServiceImpl.class);
    private S3Client s3;
    private static boolean initialized = Boolean.FALSE;

    @Override
    public void initialize() throws Exception {
        if (!initialized) {
            s3 = getAwsS3Client();
        } else {
            LOG.error("AWS s3 service has already been initialized, ignoring request.");
        }
    }

    @Override
    public S3Client getAwsS3Client() {
        if (s3 == null || sessionIsExpired(s3)) {
            try {
                generateToken();
                // TODO - decide if this is still necessary (ported from the databricks gateway go code)
                // saml2AWS returns without error, but without being fully setup, lets pause
                //Thread.sleep(30000);
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
            } catch (InvalidProtocolBufferException ex) {
                Logger.getLogger(AwsS3ServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Boolean pushObjectToS3Bucket(S3Client s3Client, TempoSample sample)
            throws JsonProcessingException, InvalidProtocolBufferException {
        // TODO- REMOVE _TEST from key
        String bucketKey = sample.getPrimaryId() + "_clinical_TEST.json";
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

    private void generateToken() throws IOException, InterruptedException {
        String usernameArg = new StringBuilder("--username=")
                .append(s3Username).toString();
        String passwordArg = new StringBuilder("--password=")
                .append(s3Password).toString();
        String[] saml2AwsCmd = {"saml2aws", "--role", s3Role, "login", "--force",
            "--mfa=Auto", usernameArg, passwordArg, "--skip-prompt", "--session-duration=3600"};

        ProcessBuilder loginBuilder = new ProcessBuilder(saml2AwsCmd);
        Process loginProcess = loginBuilder.start();

        // Read the output of the login process (for debugging or to check success/failure)
        BufferedReader loginReader = new BufferedReader(new InputStreamReader(loginProcess.getInputStream()));
        String line;
        while ((line = loginReader.readLine()) != null) {
            System.out.println("SAML2AWS Login Output: " + line);
        }

        int exitCode = loginProcess.waitFor();
        System.out.println("SAML2AWS Login Exit Code: " + exitCode);

        if (exitCode > 0) {
            throw new RuntimeException("Failed to run: " + StringUtils.join(saml2AwsCmd, " "));
        }
    }


    /**
     * TO-DO: GET THE CREDENTIALS REFRESH WORKING CORRECTLY
     * @param s3Client
     * @return
     */
    private Boolean sessionIsExpired(S3Client s3Client) {
        if (s3Client == null) {
            return Boolean.TRUE;
        }
        try {
            IdentityProvider credentialsProvider
                    = s3Client.serviceClientConfiguration().credentialsProvider();

            System.out.println("\n\nprinting credentials provider");
            System.out.println(credentialsProvider);
            System.out.println("\n\nprinting credentials provider identity type");
            System.out.println(credentialsProvider.identityType());
            System.out.println("\n\n");

            AwsCredentialsIdentity credentialsIdentity = (AwsCredentialsIdentity) credentialsProvider.resolveIdentity().join();
            if (credentialsIdentity instanceof AwsSessionCredentials) {
                System.out.println("Identified credentials identity is temporary meaning that there is a time-based expiration...");
                // per aws sdk java doc: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/identity/spi/Identity.html#expirationTime()
                // expiration time = The time after which this identity will no longer be valid.
                // - If this is empty, an expiration time is not known (but the identity may still
                // expire at some time in the future).
                AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentialsIdentity;

                if (sessionCredentials.expirationTime().isPresent()) {
                    Instant expirationTime = sessionCredentials.expirationTime().get();
                    Instant now = Instant.now();
                    System.out.println("\n\nexpiration time");
                    System.out.println(expirationTime);
                    System.out.println("time now");
                    System.out.println(now);
                    System.out.println("\n\n");

                    Duration timeLeft = Duration.between(now, expirationTime);
                    if (timeLeft.isPositive()) {
                        return Boolean.FALSE;
                    } else {
                        return Boolean.TRUE;
                    }
                } else {
                    System.out.println("Expiration time not available for these credentials.");
                    return Boolean.FALSE;
                }


            } else {
                // Not temporary credentials, so no time-based expiration
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error resolving credentials and/or resolving "
                    + "the remaining session duration.", e);
        }
    }
}
