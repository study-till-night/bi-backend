package com.yupi.springbootinit.manager;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 使用令牌桶进行限流
     *
     * @param key 限流key
     */
    public boolean doRateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        /*
          参数1  限流作用于整个令牌桶  针对所有请求
          参数2  单位时间段内允许的次数
          参数3  限流的单位时间段
          参数4  时间单位---年、月、日、时分秒
          1s限制2次请求
         */
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        // 尝试取得令牌 若失败则抛出异常
        return rateLimiter.tryAcquire(1);
    }
}
