package by.imsha.feign.dto.request;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserSearchFilterRequest {

    SearchFilter search;

    @Value
    @Builder
    public static class SearchFilter {
        Integer numberOfResults;
        String queryString;
        List<SortField> sortFields;
        Integer startRow;
    }

    @Value
    @Builder
    public static class SortField {
        String name;
        String order;
    }
}