package by.imsha.logging;

import ch.qos.logback.contrib.json.JsonFormatter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/** Formats Logback's structured event map with the same Jackson major version as the HTTP API. */
public final class Jackson3JsonFormatter implements JsonFormatter {

    private static final ObjectMapper COMPACT_MAPPER = JsonMapper.builder().build();
    private static final ObjectMapper PRETTY_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private boolean prettyPrint;

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    @Override
    public String toJsonString(Map map) {
        return (prettyPrint ? PRETTY_MAPPER : COMPACT_MAPPER).writeValueAsString(map);
    }
}
