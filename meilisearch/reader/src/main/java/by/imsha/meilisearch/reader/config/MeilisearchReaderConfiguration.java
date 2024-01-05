package by.imsha.meilisearch.reader.config;

import by.imsha.meilisearch.reader.MeilisearchReader;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(com.meilisearch.sdk.Client.class)
public class MeilisearchReaderConfiguration {

    @Bean
    @ConfigurationProperties(MeilisearchReaderProperties.PREFIX)
    @ConditionalOnMissingBean
    public MeilisearchReaderProperties meilisearchReaderProperties() {
        return new MeilisearchReaderProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public MeilisearchReader meilisearchReader(final MeilisearchReaderProperties meilisearchReaderProperties) {
        final Config config = new Config(meilisearchReaderProperties.getHostUrl(), meilisearchReaderProperties.getApiKey());
        final Client client = new Client(config);

        return new MeilisearchReader(client);
    }
}
