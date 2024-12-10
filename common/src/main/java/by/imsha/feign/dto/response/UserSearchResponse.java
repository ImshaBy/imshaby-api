package by.imsha.feign.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserSearchResponse {
    Integer total;
    List<User> users;

    @Data
    public static class User {
        ParishData data;
        List<Registration> registrations;
    }

    @Data
    public static class ParishData {
        Map<String, String> parishes;
    }

    @Data
    public static class Registration {
        String applicationId;
        List<String> roles;
    }
}