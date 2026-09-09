package com.hoangphuc.ddd.infrastructure.cache.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public interface RedisInfrasService {

    void setObject(String key, Object value);

    void setObject(String key, Object value, Duration ttl);

    <T> T getObject(String key, Class<T> targetClass);

    void setInt(String key, int value);

    /** @return -1 nếu key không tồn tại */
    int getInt(String key);

    void delete(String key);

    RedisTemplate<String, Object> getRedisTemplate();   // để chạy Lua ở bước 6
}
