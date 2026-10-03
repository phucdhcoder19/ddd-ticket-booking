package com.hoangphuc.ddd.infrastructure.distributed.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson uses the SAME Redis as RedisTemplate, but it is a different library:
 *   - Lettuce (RedisTemplate) : GET / SET / DEL / Lua  -> the basics
 *   - Redisson               : distributed locks, semaphores, queues
 * The two libraries run side by side without conflict.
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    /** destroyMethod = "shutdown": close connections when the app stops, so the process does not hang. */
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
