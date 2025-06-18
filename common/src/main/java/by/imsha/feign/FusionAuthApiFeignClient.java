package by.imsha.feign;

import by.imsha.feign.dto.request.UserSearchFilterRequest;
import by.imsha.feign.dto.response.UserSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * API для feign-клиента FusionAuth
 */
@FeignClient(name = "fusionAuthApiFeignClient", url ="${fusionAuth.host-url:http://rewrite-me}")
public interface FusionAuthApiFeignClient {

    @PostMapping("/api/user/search")
    UserSearchResponse usersSearch(@RequestBody UserSearchFilterRequest deleteDocumentsByFilterRequest,
                                   @RequestHeader("Authorization") String authorization);
}