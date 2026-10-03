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

    /** @return -1 if the key does not exist */
    int getInt(String key);

    void delete(String key);

    /**
     * Run a Lua script on Redis — the whole script is ONE atomic operation.
     * It is the only way to check-then-set without another thread cutting in.
     */
    Long executeScript(RedisScript<Long> script, List<String> keys, Object... args);

    RedisTemplate<String, Object> getRedisTemplate();
}
