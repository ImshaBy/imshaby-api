package by.imsha.feign.dto.response;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class UserSearchResponse {
    Integer total;
    List<User> users;

    @Value
    public static class User {
        Data data;
    }

    @Value
    public static class Data {
        boolean superAdmin;
        Map<String, String> parishes;
    }
}