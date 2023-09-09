package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.EntityWebhook;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.EntityWebHookType;
import by.imsha.domain.dto.WebHookInfo;
import by.imsha.repository.EntityWebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class EntityWebhookService {

    private static final Logger log = LoggerFactory.getLogger(EntityWebhookService.class);

    @Autowired
    private EntityWebhookRepository entityWebhookRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private ParishService parishService;

    public EntityWebhook createCityWebHook(WebHookInfo webHookInfoOptional){
        return createWebHook(Optional.ofNullable(webHookInfoOptional), EntityWebHookType.CITY).get();
    }

    public EntityWebhook createParishWebHook(WebHookInfo webHookInfoOptional){
        return createWebHook(Optional.ofNullable(webHookInfoOptional), EntityWebHookType.PARISH).get();
    }


    private Optional<EntityWebhook> createWebHook(Optional<WebHookInfo> webHookInfoOptional, EntityWebHookType type){
        EntityWebhook result = null;
        if(webHookInfoOptional.isPresent()){
            EntityWebhook wh = EntityWebhook.builder()
                    .type(type.getType())
                    .key(webHookInfoOptional.get().getKey())
                    .url(webHookInfoOptional.get().getUrl())
                    .build();
            result = createWebhook(wh);
        }
        return Optional.of(result);
    }

    private EntityWebhook createWebhook(EntityWebhook wh){
        return this.entityWebhookRepository.save(wh);
    }

    public List<EntityWebhook> retrieveCityHooks(Mass mass){
        Optional<City> city = cityService.retrieveCity(mass.getCityId());
        if(city.isPresent()) {
            String key = city.get().getKey();
            if (key != null) {
                return retrieveHooks(key, EntityWebHookType.CITY.getType());
            }
        }
        log.warn("Key is not configured for CITY, id: %s ", mass.getCityId());
        return Collections.emptyList();
    }

    public List<EntityWebhook> retrieveParishHooks(Mass mass){
        Optional<Parish> parish = parishService.getParish(mass.getParishId());
        if(parish.isPresent()) {
            String key = parish.get().getKey();
            if (key != null) {
                return retrieveHooks(key, EntityWebHookType.PARISH.getType());
            }
        }
        log.warn("Key is not configured for PARISH, id: %s ", mass.getParishId());
        return Collections.emptyList();
    }

    private List<EntityWebhook> retrieveHooksByKey(String key){
        return entityWebhookRepository.findAllByKey(key);
    }

    private List<EntityWebhook> retrieveHooks(String key, String type){
        return entityWebhookRepository.findAllByKeyAndType(key, type);
    }


    @Cacheable(cacheNames = "webhookCache")
    public Optional<EntityWebhook> retrieveHook(String id) {
        return this.entityWebhookRepository.findById(id);
    }

    @CacheEvict(cacheNames = "webhookCache")
    public void removeHook(String id) {
        this.entityWebhookRepository.deleteById(id);
    }

    public Page<EntityWebhook> getAllHooks(int page, int size) {
        return entityWebhookRepository.findAll(PageRequest.of(page, size));
    }
}
