package cn.edu.scnu.momentkeep.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 外部 HTTP 客户端配置
 *
 * <p>OkHttp 官方建议全局复用一个实例（内部维护连接池与线程池）。
 * 同时 DeepSeek 首字返回经常超过默认的 10 秒读超时，必须单独放大，
 * 否则 AI 对话会间歇性失败。</p>
 */
@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient aiOkHttpClient(
            @Value("${ai.http.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${ai.http.read-timeout-ms:60000}") long readTimeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(readTimeoutMs + 5000, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }
}
