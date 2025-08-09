package by.imsha.service;

import api_specification.by.imsha.common.fusionauth.secured_client.api.FusionauthApiClient;
import api_specification.by.imsha.common.fusionauth.secured_client.model.SearchUsersRequest;
import api_specification.by.imsha.common.fusionauth.secured_client.model.SearchUsersRequestSearch;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponse;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponseUsersInner;
import api_specification.by.imsha.common.fusionauth.secured_client.model.UsersSearchResponseUsersInnerData;
import by.imsha.properties.FusionauthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerService {

    private static final String ROLE_VOLUNTEER = "Volunteer";

    private final FusionauthApiClient fusionauthApiClient;

    @Lazy
    @Autowired
    private VolunteerService self;

    @Autowired
    private FusionauthProperties fusionAuthProperties;

    public boolean volunteerNeededByParishName(final String parishName, final boolean defaultValue) {
        return self.getVolunteerNeededMap().getOrDefault(parishName, defaultValue);
    }

    @Cacheable(cacheNames = "volunteerNeededMap", sync = true)
    public Map<String, Boolean> getVolunteerNeededMap() {
        Map<String, Boolean> volunteerNeededInParish = new HashMap<>();

        List<UsersSearchResponseUsersInner> users = new LinkedList<>();

        UsersSearchResponse usersSearchResponse;
        int startRow = 0;
        try {
            do {
                usersSearchResponse = fusionauthApiClient.searchUsers(SearchUsersRequest.builder()
                                .search(SearchUsersRequestSearch.builder()
                                        .numberOfResults(fusionAuthProperties.getUserSearchPagination())
                                        .queryString("*")
                                        .startRow(startRow)
                                        .build()).build())
                        .getBody();

                users.addAll(usersSearchResponse.getUsers());

                usersSearchResponse.getUsers().stream()
                        .flatMap(user -> Optional.ofNullable(user)
                                .filter(u -> u.getRegistrations().stream()
                                        .filter(registration -> registration.getApplicationId().equals(fusionAuthProperties.getApplicationId()))
                                        .anyMatch(registration -> registration.getRoles().contains(ROLE_VOLUNTEER)))
                                .map(UsersSearchResponseUsersInner::getData)
                                .map(UsersSearchResponseUsersInnerData::getParishes).stream())
                        .flatMap(map -> map.values().stream())
                        .forEach(name -> {
                            Boolean volunteerNeeded = volunteerNeededInParish.computeIfPresent(name, (k, v) -> false);
                            if (volunteerNeeded == null) {
                                volunteerNeededInParish.put(name, true);
                            }
                        });
                startRow += fusionAuthProperties.getUserSearchPagination();
            } while (usersSearchResponse.getUsers().size() >= fusionAuthProperties.getUserSearchPagination());
        } catch (Exception e) {
            log.error("При получении volunteerNeededMap произошла ошибка: ", e);
            return new VolunteerNotNeededStubMap();
        }

        try {
            log.info("При получении volunteerNeededMap были найдены {} парафии из них {} нуждаются в волонтерах",
                    volunteerNeededInParish.size(),
                    volunteerNeededInParish.entrySet().stream()
                            .filter(Map.Entry::getValue)
                            .count());
        } catch (Exception e) {
            log.error("При составлении лога volunteerNeededMap произошла ошибка: ", e);
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