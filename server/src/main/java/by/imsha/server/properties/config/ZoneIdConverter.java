package by.imsha.server.properties.config;

import org.springframework.core.convert.converter.Converter;

import java.time.ZoneId;

/**
 * Конвертер для создания объекта {@link ZoneId} из строки
 */
public class ZoneIdConverter implements Converter<String, ZoneId> {

    @Override
    public ZoneId convert(String from) {
        return ZoneId.of(from);
    }
}
