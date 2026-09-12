package com.hoangphuc.ddd.infrastructure.cache.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

public interface RedisInfrasService {

    void setObject(String key, Object value);

    void setObject(String key, Object value, Duration ttl);

    <T> T getObject(String key, Class<T> targetClass);

    void setInt(String key, int value);

    /** @return -1 nếu key không tồn tại */
    int getInt(String key);

    void delete(String key);

    /**
     * Chạy 1 script Lua trên Redis — toàn bộ script là MỘT thao tác nguyên tử.
     * Đây là cách duy nhất để check-rồi-set mà không bị thread khác chen vào.
     */
    Long executeScript(RedisScript<Long> script, List<String> keys, Object... args);

    RedisTemplate<String, Object> getRedisTemplate();
}
