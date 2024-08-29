package by.imsha.server.bdd.glue.components;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GlobalStorage {

    public final Map<String, Object> storage = new ConcurrentHashMap<>();

    public void put(final String id, final Object object) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("идентификатор является обязательным для хранения данных");
        }

        storage.put(id, object);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String id) {
        return (T) storage.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T> T tryResolveModifier(final String id) {
        if (id != null && id.startsWith("[ключ]")) {
            return (T) storage.getOrDefault(id.substring(6).trim(), id);
        }
        return (T) id;
    }
}
