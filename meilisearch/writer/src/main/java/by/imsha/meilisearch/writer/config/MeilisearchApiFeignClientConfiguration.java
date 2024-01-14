package by.imsha.meilisearch.writer.config;

import by.imsha.meilisearch.writer.feign.BearerAuthInterceptor;
import by.imsha.meilisearch.writer.feign.MeilisearchApiFeignClient;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(FeignClientsConfiguration.class)
public class MeilisearchApiFeignClientConfiguration {

    @Bean
    MeilisearchApiFeignClient meilisearchApiFeignClient(final MeilisearchWriterProperties properties,
                                                        final Decoder decoder,
                                                        final Encoder encoder,
                                                        final Client client,
                                                        final Contract contract) {
        return Feign.builder()
                .client(client)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptor(
                        new BearerAuthInterceptor(properties.getApiKey())
                )
                .target(MeilisearchApiFeignClient.class, properties.getHostUrl());
    }
}
