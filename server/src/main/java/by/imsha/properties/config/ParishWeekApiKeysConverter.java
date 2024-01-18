package by.imsha.properties.config;


import by.imsha.properties.ImshaProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Конвертер их строки формата "apiKey1:parishKey1,apiKey2:parishKey2,apiKey3:parishKey3"
 * (допустимы лишние пробелы)
 */
@Component
@ConfigurationPropertiesBinding
public class ParishWeekApiKeysConverter implements Converter<String, ImshaProperties.ParishWeekApiKeys> {

    @Override
    public ImshaProperties.ParishWeekApiKeys convert(final String from) {

        final ImshaProperties.ParishWeekApiKeys parishScheduleApiKeys = new ImshaProperties.ParishWeekApiKeys();

        if (!StringUtils.isBlank(from)) {
            final Map<String, String> apiKeyAndParishKeyMap = Arrays.stream(from.split(","))
                    .map(value -> {
                        String[] apiKeyAndParishKey = value.split(":");
                        return Pair.of(apiKeyAndParishKey[0].trim(), apiKeyAndParishKey[1].trim());
                    })
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

            parishScheduleApiKeys.getMap().putAll(apiKeyAndParishKeyMap);
        }

        return parishScheduleApiKeys;
    }
}
