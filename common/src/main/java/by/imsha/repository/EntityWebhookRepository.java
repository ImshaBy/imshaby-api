package by.imsha.repository;

import by.imsha.domain.EntityWebhook;

import java.util.List;

public interface EntityWebhookRepository extends QuerableMongoRepository<EntityWebhook, String> {
    List<EntityWebhook> findAllByKey(String key);
    List<EntityWebhook> findAllByKeyAndType(String key, String type);

}
