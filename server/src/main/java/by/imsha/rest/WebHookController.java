package by.imsha.rest;

import by.imsha.domain.EntityWebhook;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.WebHookInfo;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.service.EntityWebhookService;
import by.imsha.utils.Constants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/hook")
public class WebHookController {

    @Autowired
    private EntityWebhookService entityWebhookService;

    @PostMapping("/city")
    public ResponseEntity<EntityWebhook> createCityWebHook(@Valid @RequestBody WebHookInfo webHookInfo) {
        return ResponseEntity.ok(
                entityWebhookService.createCityWebHook(webHookInfo)
        );
    }

    @PostMapping("/parish")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityWebhook> createParishWebHook(@Valid @RequestBody WebHookInfo webHookInfo) {
        return ResponseEntity.ok(
                entityWebhookService.createParishWebHook(webHookInfo)
        );
    }

    @GetMapping
    public ResponseEntity<Page<EntityWebhook>> getAllHooks(
            @RequestParam(value = "page", defaultValue = Constants.DEFAULT_PAGE_NUM) int page,
            @RequestParam(value = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(
                this.entityWebhookService.getAllHooks(page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityWebhook> getHook(@PathVariable("id") String id) {
        return this.entityWebhookService.retrieveHook(id)
                .map(ResponseEntity::ok)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UpdateEntityInfo> deleteHook(@PathVariable("id") String id) {
        if (!this.entityWebhookService.retrieveHook(id).isPresent()) {
            throw new ResourceNotFoundException();
        }

        this.entityWebhookService.removeHook(id);

        return ResponseEntity.ok(
                new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED)
        );
    }


}
