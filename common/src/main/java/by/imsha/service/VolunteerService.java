package by.imsha.service;

import by.imsha.feign.FusionAuthApiFeignClient;
import by.imsha.feign.dto.request.UserSearchFilterRequest;
import by.imsha.feign.dto.response.UserSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final FusionAuthApiFeignClient fusionAuthApiFeignClient;

    @Lazy
    @Autowired
    private VolunteerService self;

    @Value("${fusionAuth.authorization-token}")
    private String authorization;

    @Value("${fusionAuth.user-search-pagination}")
    private Integer pagination;

    public Boolean volunteerNeededByParishName(final String parishName) {
        return self.getVolunteerNeededMap().get(parishName);
    }

    @Cacheable(cacheNames = "volunteerNeededMap")
    public Map<String, Boolean> getVolunteerNeededMap() {
        Map<String, Boolean> volunteerNeededInParish = new HashMap<>();
        UserSearchResponse userSearchResponse;
        int startRow = 0;
        do {
            userSearchResponse = fusionAuthApiFeignClient.usersSearch(UserSearchFilterRequest.builder()
                    .search(UserSearchFilterRequest.SearchFilter.builder()
                            .numberOfResults(pagination)
                            .queryString("*")
                            .startRow(startRow)
                            .build()).build(), authorization);

            userSearchResponse.getUsers().stream()
                    .flatMap(user -> Optional.ofNullable(user)
                            .map(UserSearchResponse.User::getData)
                            .filter(data -> !data.isSuperAdmin())
                            .map(UserSearchResponse.Data::getParishes).stream())
                    .flatMap(map -> map.values().stream())
                    .forEach(name -> {
                        Boolean volunteerNeeded = volunteerNeededInParish.computeIfPresent(name, (k, v) -> true);
                        if (volunteerNeeded == null) {
                            volunteerNeededInParish.put(name, false);
                        }
                    });
            startRow += pagination;
        } while (userSearchResponse.getUsers().size() < pagination);
        return volunteerNeededInParish;
    }
}