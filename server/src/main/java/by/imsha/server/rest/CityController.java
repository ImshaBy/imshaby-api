package by.imsha.server.rest;

import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.domain.dto.CityInfo;
import by.imsha.domain.dto.LocalizedCityInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.mapper.CityMapper;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.service.CityService;
import by.imsha.utils.Constants;
import by.imsha.validation.common.AvailableLocale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/*
 * Demonstrates how to set up RESTful API endpoints using Spring MVC
 */

@RestController
@RequestMapping(value = "/api/cities")
@Slf4j
@Validated
public class CityController {

    @Autowired
    private CityService cityService;

    @Autowired
    private CityMapper cityMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<City> createCity(@RequestBody CityInfo city) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        this.cityService.createCity(
                                City.builder()
                                        .name(city.getName())
                                        .key(city.getKey())
                                        .build()
                        )
                );
    }

    @PutMapping("/{cityId}/lang/{locale}")
    public ResponseEntity<UpdateEntityInfo> createLocalizedCity(final @Valid @RequestBody LocalizedCityInfo cityInfo,
                                                                final @PathVariable("cityId") String id,
                                                                final @AvailableLocale(field = "locale", message = "CITY.002")
                                                                @PathVariable("locale") Locale locale) {
        final City city = this.cityService.retrieveCity(id)
                .orElseThrow(ResourceNotFoundException::new);

        final LocalizedCity localizedCity = new LocalizedCity(locale.getLanguage(), id, cityInfo.getName());
        // hack to support backward compatibility
        if (city.getLocalizedInfo() == null) {
            city.setLocalizedInfo(new HashMap<>());
        }
        city.getLocalizedInfo().put(locale.getLanguage(), localizedCity);

        final City updatedCity = this.cityService.updateCity(city);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedCity.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @GetMapping
    public ResponseEntity<Page<City>> getAllCity(
            final @RequestParam(value = "page", defaultValue = Constants.DEFAULT_PAGE_NUM) int page,
            final @RequestParam(value = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size,
            final HttpServletRequest request) {

        logHeaders(request);

        return ResponseEntity.ok(
                this.cityService.getAllCities(page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<City> getCity(final @PathVariable("id") String id) {
        return ResponseEntity.ok(
                this.cityService.retrieveCity(id)
                        .orElseThrow(ResourceNotFoundException::new)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateEntityInfo> updateCity(final @PathVariable("id") String id, final @RequestBody CityInfo cityInfo) {
        final City city = this.cityService.retrieveCity(id)
                .orElseThrow(ResourceNotFoundException::new);

        cityMapper.updateCityFromDTO(cityInfo, city);

        final City updatedCity = this.cityService.updateCity(city);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedCity.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UpdateEntityInfo> deleteCity(final @PathVariable("id") String id) {
        if (!this.cityService.retrieveCity(id).isPresent()) {
            throw new ResourceNotFoundException();
        }

        this.cityService.removeCity(id);

        return ResponseEntity.ok(
                new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED)
        );
    }

    //TODO выяснить для чего, и отрефакторить
    @Deprecated
    private void logHeaders(HttpServletRequest request) {
        if (!log.isWarnEnabled()) {
            return;
        }

        final List<String> headerAndValuesList = new LinkedList<>();
        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final Enumeration<String> headerValues = request.getHeaders(headerName);
            final List<String> values = new LinkedList<>();

            while (headerValues.hasMoreElements()) {
                values.add(headerValues.nextElement());
            }

            headerAndValuesList.add(
                    String.format("%s = [%s]", headerName, String.join(", ", values))
            );
        }

        log.warn(request.getRequestURI() + " headers: " + String.join("; ", headerAndValuesList));
    }
}
