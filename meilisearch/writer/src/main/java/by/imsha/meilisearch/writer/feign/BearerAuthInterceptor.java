package by.imsha.meilisearch.writer.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class BearerAuthInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final String headerValue;

    public BearerAuthInterceptor(final String token) {
        this.headerValue = "Bearer " + token;
    }

    @Override
    public void apply(final RequestTemplate template) {
        template.header(AUTHORIZATION_HEADER, headerValue);
    }
}
