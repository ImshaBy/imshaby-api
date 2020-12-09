package by.imsha.rest;

import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.domain.dto.CityInfo;
import by.imsha.domain.dto.LocalizedCityInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.mapper.CityMapper;
import by.imsha.exception.InvalidLocaleException;
import by.imsha.service.CityService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

/*
 * Demonstrates how to set up RESTful API endpoints using Spring MVC
 */

@RestController
@RequestMapping(value = "/api/cities")
public class CityController extends AbstractRestHandler {

    @Autowired
    private CityService cityService;

    @ApiOperation(value = "Create city")
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

    @ApiOperation(value = "Add localized info to the city")
    @RequestMapping(value = "/{cityId}/lang/{lc}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo createLocalizedCity(@Valid @RequestBody LocalizedCityInfo cityInfo,
                                    @PathVariable("cityId") String id,
                                     @ApiParam(value = "Language code according ISO 639-1/ISO 639-2 spec" )
                                    @PathVariable("lc") String lang) {
        City origin = this.cityService.retrieveCity(id);
        checkResourceFound(origin);
        Locale localeObj = new Locale(lang);
        if(!LocaleUtils.isAvailableLocale(localeObj)){
            throw new InvalidLocaleException("Invalid lang specified (please specify ISO 639-1/ISO 639-2 lang code : " + lang);
        }
        LocalizedCity localizedCity = new LocalizedCity(lang, id, cityInfo.getName());
        // hack to support backward compatibility
        if(origin.getLocalizedInfo() == null){
            origin.setLocalizedInfo(new HashMap<>());
        }
        origin.getLocalizedInfo().put(lang, localizedCity);

        City updatedCity = this.cityService.updateCity(origin);
        return new UpdateEntityInfo(updatedCity.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @ApiOperation(value = "Get all available cities.",
            notes = "The list is paginated. You can provide a page number (default 0) and a page size (default 100)")
    @RequestMapping(value = "",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Page<City> getAllCity(@ApiParam(value = "The page number (zero-based)")
                                      @RequestParam(value = "page",  defaultValue = DEFAULT_PAGE_NUM) int page,
                                      @ApiParam(value = "Tha page size" )
                                      @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int size,
                                      HttpServletRequest request, HttpServletResponse response) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            log.warn(String.format("%s = %s", headerName , request.getHeader(headerName)));
        }
        return this.cityService.getAllCities(page, size);
    }

    @ApiOperation(value = "Get city by ID")
    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public City getCity(@ApiParam(value = "The ID of the city.", required = true)
                             @PathVariable("id") String id) {
        City city = this.cityService.retrieveCity(id);
        checkResourceFound(city);
        //todo: http://goo.gl/6iNAkz
        return city;
    }

    @ApiOperation(value = "Update city details by ID")
    @RequestMapping(value = "/{id}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo updateCity(@ApiParam(value = "The ID of the existing city", required = true)
                                 @PathVariable("id") String id, @Valid @RequestBody CityInfo cityInfo) {
        City resource = this.cityService.retrieveCity(id);
        checkResourceFound(resource);
        CityMapper.MAPPER.updateCityFromDTO(cityInfo, resource);
        City updatedSity = this.cityService.updateCity(resource);
      return new UpdateEntityInfo(updatedSity.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @ApiOperation(value = "Delete city")
    @RequestMapping(value = "/{id}",
            method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo deleteCity(@ApiParam(value = "The ID of the existing city.", required = true)
                                 @PathVariable("id") String id, HttpServletRequest request,
                                       HttpServletResponse response) {
        checkResourceFound(this.cityService.retrieveCity(id));
        this.cityService.removeCity(id);
        return new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED);
    }
}
