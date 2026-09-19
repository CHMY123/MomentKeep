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

    /**
     * 区域标识
     *
     * <p>此前硬编码 {@code Region.CN_NORTH_1}，配置项 {@code s3.region} 形同虚设。
     * 默认值刻意保持为原来的 {@code cn-north-1}：S3 的 SigV4 签名会把区域计入摘要，
     * 贸然改成其它值可能导致上传报 SignatureDoesNotMatch。</p>
     */
    @Value("${s3.region:cn-north-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
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