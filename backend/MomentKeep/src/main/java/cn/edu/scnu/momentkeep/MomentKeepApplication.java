/**
 * MomentKeep 朝暮记 - Spring Boot 应用启动类
 *
 * @description 应用程序入口点，负责启动Spring Boot应用并配置核心组件
 * @author MomentKeep Team
 * @since 2026-04-18
 */
package cn.edu.scnu.momentkeep;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Boot 应用主类
 * @SpringBootApplication 组合注解，启用自动配置、组件扫描和Spring Boot特性
 * @MapperScan 指定MyBatis Mapper接口扫描路径
 */
@SpringBootApplication
@MapperScan("cn.edu.scnu.momentkeep.mapper")
public class MomentKeepApplication {

    /**
     * 应用入口方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MomentKeepApplication.class, args);
    }

    /**
     * 配置 RestTemplate Bean
     * @description 用于HTTP请求调用外部服务（如AI接口）
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}