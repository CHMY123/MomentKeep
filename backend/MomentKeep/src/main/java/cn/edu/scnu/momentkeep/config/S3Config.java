package cn.edu.scnu.momentkeep.config;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${s3.endpoint}")
    private String endpoint;

    @Value("${s3.access-key}")
    private String accessKey;

    @Value("${s3.secret-key}")
    private String secretKey;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.CN_NORTH_1)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return new AwsCredentials() {
                            @Override
                            public String accessKeyId() {
                                return accessKey;
                            }

                            @Override
                            public String secretAccessKey() {
                                return secretKey;
                            }
                        };
                    }
                })
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }
}