package by.imsha.cache;

import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author Alena Misan
 */

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        return new SpringCache2kCacheManager()
                .defaultSetup(b -> b.entryCapacity(500).enableJmx(true).permitNullValues(true))
                .addCaches(
                        b -> b.name("parishCache").entryCapacity(100).expireAfterWrite(14, TimeUnit.DAYS),
                        b -> b.name("cityCache").entryCapacity(10),
                        b -> b.name("massCache").expireAfterWrite(14, TimeUnit.DAYS),
                        b -> b.name("webhookCache").entryCapacity(100),
                        b -> b.name("pendingParishes").expireAfterWrite(14, TimeUnit.DAYS),
                        b -> b.name("citiesWithParishCache").entryCapacity(1).expireAfterWrite(1, TimeUnit.DAYS)
                );
    }

}
