package by.imsha.repository;

import by.imsha.domain.EntityWebhook;
import org.springframework.hateoas.EntityModel;

import java.util.List;

public interface EntityWebhookRepository extends QuerableMongoRepository<EntityWebhook, String> {
    EntityModel findByKey(String key);
    List<EntityWebhook> findAllByKey(String key);
    List<EntityWebhook> findAllByKeyAndType(String key, String type);

}
