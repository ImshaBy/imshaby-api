package by.imsha.rest;

import by.imsha.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/cache")
@Slf4j
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @Secured("ROLE_INTERNAL")
    @PostMapping("/clear")
    public ResponseEntity<Void> createMass() {
        cacheService.clearAllCache();
        log.info("Кэш успешно очищен");
        return ResponseEntity.ok().build();
    }
}