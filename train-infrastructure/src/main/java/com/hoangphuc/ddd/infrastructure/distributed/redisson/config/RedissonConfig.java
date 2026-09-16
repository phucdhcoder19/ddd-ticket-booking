package com.hoangphuc.ddd.infrastructure.distributed.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson dùng CÙNG một Redis với RedisTemplate, nhưng là thư viện khác:
 *   - Lettuce (RedisTemplate) : GET / SET / DEL / Lua  -> việc cơ bản
 *   - Redisson               : khoá phân tán, semaphore, hàng đợi
 * Hai thư viện chạy song song, không xung đột.
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    /** destroyMethod = "shutdown": đóng kết nối khi app tắt, tránh treo tiến trình. */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(50)
                .setDatabase(0);
        return Redisson.create(config);
    }
}
