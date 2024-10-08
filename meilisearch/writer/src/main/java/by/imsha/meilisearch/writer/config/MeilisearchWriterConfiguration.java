package by.imsha.meilisearch.writer.config;

import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.DefaultMeilisearchWriter;
import by.imsha.meilisearch.writer.MeilisearchWriter;
import by.imsha.meilisearch.writer.feign.MeilisearchApiFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.model.Faceting;
import com.meilisearch.sdk.model.Pagination;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TypoTolerance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация writer без возможности переопределения бинов
 * (т.к. это не публичная библиотека, а только наша и при необходимости - доработаем)
 */
@Configuration
@Slf4j
public class MeilisearchWriterConfiguration {

    @Bean
    @ConfigurationProperties(MeilisearchWriterProperties.PREFIX)
    public MeilisearchWriterProperties meilisearchWriterProperties() {
        return new MeilisearchWriterProperties();
    }

    @Bean
    public MeilisearchWriter meilisearchWriter(final MeilisearchWriterProperties meilisearchReaderProperties,
                                               final Settings settings,
                                               final MeilisearchApiFeignClient meilisearchApiFeignClient,
                                               final ObjectMapper objectMapper) {
        final Config config = new Config(meilisearchReaderProperties.getHostUrl(), meilisearchReaderProperties.getApiKey());
        final Client client = new Client(config);

        return new DefaultMeilisearchWriter(client, meilisearchReaderProperties.getIndexUid(), settings,
                meilisearchApiFeignClient, objectMapper);
    }

    @Bean
    public Settings defaultIndexSettings() {
        final Settings settings = new Settings();

        settings.setFilterableAttributes(SearchRecord.FILTERABLE_ATTRIBUTES);
        settings.setSortableAttributes(SearchRecord.SORTABLE_ATTRIBUTES);
        settings.setSearchableAttributes(SearchRecord.SEARCHABLE_ATTRIBUTES);
        settings.setDisplayedAttributes(SearchRecord.DISPLAYED_ATTRIBUTES);

        settings.setTypoTolerance(new TypoTolerance().setEnabled(false));//мы не ищем по введенному слову, так что typoTolerance можно выключить

        final Faceting faceting = new Faceting();
        faceting.setMaxValuesPerFacet(200);//максимальный размер значений в facetDistribution по умолчанию
        settings.setFaceting(faceting);

        final Pagination pagination = new Pagination();
        pagination.setMaxTotalHits(1000);//максимальный размер выборки по умолчанию
        settings.setPagination(pagination);

        return settings;
    }
}
