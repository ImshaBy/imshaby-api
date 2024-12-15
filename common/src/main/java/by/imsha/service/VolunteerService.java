package by.imsha.service;

import by.imsha.feign.FusionAuthApiFeignClient;
import by.imsha.feign.dto.request.UserSearchFilterRequest;
import by.imsha.feign.dto.response.UserSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerService {

    private static final String ROLE_VOLUNTEER = "Volunteer";

    private final FusionAuthApiFeignClient fusionAuthApiFeignClient;

    @Lazy
    @Autowired
    private VolunteerService self;

    @Value("${fusionAuth.authorization-token}")
    private String authorization;
    @Value("${fusionAuth.user-search-pagination}")
    private Integer pagination;
    @Value("${fusionAuth.application-id}")
    private String applicationId;

    public boolean volunteerNeededByParishName(final String parishName, final boolean defaultValue) {
        return self.getVolunteerNeededMap().getOrDefault(parishName, defaultValue);
    }

    @Cacheable(cacheNames = "volunteerNeededMap", sync = true)
    public Map<String, Boolean> getVolunteerNeededMap() {
        Map<String, Boolean> volunteerNeededInParish = new HashMap<>();
        UserSearchResponse userSearchResponse;
        int startRow = 0;
        try {
            do {
                userSearchResponse = fusionAuthApiFeignClient.usersSearch(UserSearchFilterRequest.builder()
                        .search(UserSearchFilterRequest.SearchFilter.builder()
                                .numberOfResults(pagination)
                                .queryString("*")
                                .startRow(startRow)
                                .build()).build(), authorization);

                userSearchResponse.getUsers().stream()
                        .flatMap(user -> Optional.ofNullable(user)
                                .filter(u -> u.getRegistrations().stream()
                                        .filter(registration -> registration.getApplicationId().equals(applicationId))
                                        .anyMatch(registration -> registration.getRoles().contains(ROLE_VOLUNTEER)))
                                .map(UserSearchResponse.User::getData)
                                .map(UserSearchResponse.ParishData::getParishes).stream())
                        .flatMap(map -> map.values().stream())
                        .forEach(name -> {
                            Boolean volunteerNeeded = volunteerNeededInParish.computeIfPresent(name, (k, v) -> false);
                            if (volunteerNeeded == null) {
                                volunteerNeededInParish.put(name, true);
                            }
                        });
                startRow += pagination;
            } while (userSearchResponse.getUsers().size() >= pagination);
        } catch (Exception e) {
            log.error("При получении volunteerNeededMap произошла ошибка: ", e);
            return new VolunteerNotNeededStubMap();
        }
        return volunteerNeededInParish;
    }

    /**
     * Заглушка, возвращающая всегда false
     * <p>
     * Необходима, если мы не смогли загрузить данные из fusionAuth
     */
    private static class VolunteerNotNeededStubMap implements Map<String, Boolean> {

        @Override
        public Boolean getOrDefault(Object key, Boolean defaultValue) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Boolean get(Object key) {
            return null;
        }

        @Override
        public Boolean put(String key, Boolean value) {
            return null;
        }

        @Override
        public Boolean remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Boolean> m) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Set<String> keySet() {
            return null;
        }

        @Override
        public Collection<Boolean> values() {
            return null;
        }

        @Override
        public Set<Entry<String, Boolean>> entrySet() {
            return null;
        }
    }
}