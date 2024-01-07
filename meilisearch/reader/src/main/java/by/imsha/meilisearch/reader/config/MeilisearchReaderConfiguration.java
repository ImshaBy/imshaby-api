package by.imsha.meilisearch.reader.config;

import by.imsha.meilisearch.reader.DefaultMeilisearchReader;
import by.imsha.meilisearch.reader.MeilisearchReader;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Конфигурация reader без возможности переопределения бинов
 * (т.к. это не публичная библиотека, а только наша и при необходимости - доработаем)
 */
@AutoConfiguration
@ConditionalOnClass(com.meilisearch.sdk.Client.class)
@Slf4j
public class MeilisearchReaderConfiguration {

    @Bean
    @ConfigurationProperties(MeilisearchReaderProperties.PREFIX)
    public MeilisearchReaderProperties meilisearchReaderProperties() {
        return new MeilisearchReaderProperties();
    }

    @Bean
    public MeilisearchReader meilisearchReader(final MeilisearchReaderProperties meilisearchReaderProperties) {
        final Config config = new Config(meilisearchReaderProperties.getHostUrl(), meilisearchReaderProperties.getApiKey());
        final Client client = new Client(config);

        return new DefaultMeilisearchReader(client, meilisearchReaderProperties.getIndexUid());
    }
}
