package by.imsha.cache;

import org.redisson.Redisson;
import org.redisson.api.NameMapper;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SslProvider;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;

/**
 * @author Alena Misan
 */

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
                .setClientName(null)
                .setAddress("redis://localhost:6379")
                .setUsername(null)
                .setPassword(null)
                .setDatabase(0)
                .setConnectTimeout(10_000)
                .setTimeout(3_000) //server response timeout
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(24)
                .setIdleConnectionTimeout(10_000)
                //minimum idle subscription connection
                .setSubscriptionConnectionMinimumIdleSize(1)
                .setSubscriptionConnectionPoolSize(50)
                //Subscriptions per Redis connection limit
                .setSubscriptionsPerConnection(5)
                .setDnsMonitoringInterval(5_000) //-1 to disable
                .setPingConnectionInterval(30_000)
                //if it sent request successfully then "timeout" will be started.
                .setRetryAttempts(3)
                .setRetryInterval(1_500)
                //maps any redisson object name
                .setNameMapper(NameMapper.direct())

                .setTcpNoDelay(true)
                .setKeepAlive(false)
                //SSL
                .setSslEnableEndpointIdentification(true)
                .setSslKeystore(null)
                .setSslKeystorePassword(null)
                .setSslProvider(SslProvider.JDK)
                .setSslProtocols(null)
                .setSslTruststore(null)
                .setSslTruststorePassword(null);

        return Redisson.create(config);
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) {
        // https://github.com/redisson/redisson/wiki/2.-Configuration
        HashMap<String, CacheConfig> config = new HashMap<>();

        CacheConfig parishCacheConfig = new CacheConfig();
        parishCacheConfig.setMaxSize(100);
        parishCacheConfig.setMaxIdleTime(0);
        parishCacheConfig.setTTL(Duration.ofDays(14).toMillis());
        config.put("parishCache", parishCacheConfig);

        CacheConfig cityCacheConfig = new CacheConfig();
        cityCacheConfig.setMaxSize(10);
        cityCacheConfig.setMaxIdleTime(0);
        cityCacheConfig.setMaxIdleTime(Duration.ofDays(1).toMillis());
        config.put("cityCache", cityCacheConfig);

        CacheConfig massCacheConfig = new CacheConfig();
        massCacheConfig.setMaxSize(0);
        massCacheConfig.setMaxIdleTime(0);
        massCacheConfig.setTTL(Duration.ofDays(14).toMillis());
        config.put("massCache", massCacheConfig);

        CacheConfig webhookCacheConfig = new CacheConfig();
        webhookCacheConfig.setMaxSize(100);
        webhookCacheConfig.setMaxIdleTime(0);
        webhookCacheConfig.setTTL(0);
        config.put("webhookCache", webhookCacheConfig);

        return new RedissonSpringCacheManager(redissonClient, config);
    }

}
