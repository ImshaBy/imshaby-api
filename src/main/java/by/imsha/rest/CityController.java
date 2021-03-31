package by.imsha.rest;

import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.domain.dto.CityInfo;
import by.imsha.domain.dto.LocalizedCityInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.mapper.CityMapper;
import by.imsha.exception.InvalidLocaleException;
import by.imsha.service.CityService;
import by.imsha.utils.Constants;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

/*
 * Demonstrates how to set up RESTful API endpoints using Spring MVC
 */

@RestController
@RequestMapping(value = "/api/cities")
public class CityController extends AbstractRestHandler {

    @Autowired
    private CityService cityService;

    @RequestMapping(value = "",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public City createCity( @Valid @RequestBody CityInfo city,
                                 HttpServletRequest request, HttpServletResponse response) {
        City createdCity = this.cityService.createCity(new City(city.getName()));
        return createdCity;
    }

    @RequestMapping(value = "/{cityId}/lang/{lc}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo createLocalizedCity(@Valid @RequestBody LocalizedCityInfo cityInfo,
                                    @PathVariable("cityId") String id,
                                    @PathVariable("lc") String lang) {
        Optional<City> origin = this.cityService.retrieveCity(id);
        checkResourceFound(origin);
        City city = origin.get();
        Locale localeObj = new Locale(lang);
        if(!LocaleUtils.isAvailableLocale(localeObj)){
            throw new InvalidLocaleException("Invalid lang specified (please specify ISO 639-1/ISO 639-2 lang code : " + lang);
        }
        LocalizedCity localizedCity = new LocalizedCity(lang, id, cityInfo.getName());
        // hack to support backward compatibility
        if(city.getLocalizedInfo() == null){
            city.setLocalizedInfo(new HashMap<>());
        }
        city.getLocalizedInfo().put(lang, localizedCity);

        City updatedCity = this.cityService.updateCity(city);
        return new UpdateEntityInfo(updatedCity.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @RequestMapping(value = "",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<City> getAllCity(
                                      @RequestParam(value = "page",  defaultValue = Constants.DEFAULT_PAGE_NUM) int page,
                                      @RequestParam(value = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size,
                                      HttpServletRequest request, HttpServletResponse response) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            log.warn(String.format("%s = %s", headerName , request.getHeader(headerName)));
        }
        return this.cityService.getAllCities(page, size);
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public City getCity(
                             @PathVariable("id") String id) {
        Optional<City> city = this.cityService.retrieveCity(id);
        checkResourceFound(city);
        //todo: http://goo.gl/6iNAkz
        return city.get();
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo updateCity(
                                 @PathVariable("id") String id, @Valid @RequestBody CityInfo cityInfo) {
        Optional<City> resource = this.cityService.retrieveCity(id);
        checkResourceFound(resource);
        CityMapper.MAPPER.updateCityFromDTO(cityInfo, resource.get());
        City updatedSity = this.cityService.updateCity(resource.get());
      return new UpdateEntityInfo(updatedSity.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo deleteCity(
                                 @PathVariable("id") String id, HttpServletRequest request,
                                       HttpServletResponse response) {
        checkResourceFound(this.cityService.retrieveCity(id));
        this.cityService.removeCity(id);
        return new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED);
    }
}
