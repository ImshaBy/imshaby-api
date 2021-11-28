package by.imsha.rest;

import by.imsha.domain.City;
import by.imsha.domain.EntityWebhook;
import by.imsha.domain.Mass;
import by.imsha.domain.dto.CityInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.WebHookInfo;
import by.imsha.domain.dto.mapper.CityMapper;
import by.imsha.service.EntityWebhookService;
import by.imsha.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping(value = "/hook")
public class WebHookController extends AbstractRestHandler {

    @Autowired
    private EntityWebhookService entityWebhookService;

    @RequestMapping(value = "/city",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityWebhook> createCityWebHook(@Validated @RequestBody WebHookInfo webHookInfo){
        return ResponseEntity.ok().body(entityWebhookService.createCityWebHook(webHookInfo));
    }

    @RequestMapping(value = "/parish",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityWebhook> createParishWebHook(@Validated @RequestBody WebHookInfo webHookInfo){
        return ResponseEntity.ok().body(entityWebhookService.createParishWebHook(webHookInfo));
    }


    @RequestMapping(value = "",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<EntityWebhook> getAllHooks(
            @RequestParam(value = "page",  defaultValue = Constants.DEFAULT_PAGE_NUM) int page,
            @RequestParam(value = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size) {
        return this.entityWebhookService.getAllHooks(page, size);
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public EntityWebhook getHook(
            @PathVariable("id") String id) {
        Optional<EntityWebhook> hook = this.entityWebhookService.retrieveHook(id);
        checkResourceFound(hook);
        return hook.get();
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo deleteHook(
            @PathVariable("id") String id) {
        checkResourceFound(this.entityWebhookService.retrieveHook(id));
        this.entityWebhookService.removeHook(id);
        return new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED);
    }


}
