package cn.edu.scnu.momentkeep.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * <p>缺少分页拦截器时 {@code selectPage(...)} 会退化为全表查询，
 * 缺少乐观锁拦截器时实体上的 {@code @Version} 完全不生效，因此两者都必须注册。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /** 单页最大条数，防止前端传入超大 size 造成数据库压力 */
    private static final long MAX_PAGE_SIZE = 200L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        // 超出最大页后返回空集合而不是回到首页，避免前端拿到重复数据
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
