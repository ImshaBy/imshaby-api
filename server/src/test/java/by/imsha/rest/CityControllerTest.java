package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.domain.dto.CityInfo;
import by.imsha.domain.dto.mapper.CityMapper;
import by.imsha.properties.ImshaProperties;
import by.imsha.repository.CityRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.service.CityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CityController.class, ValidationConfiguration.class, CityService.class, TestTimeConfiguration.class})
class CityControllerTest {

    private static final String ROOT_PATH = "/api/cities";

    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private CityRepository cityRepository;
    @MockBean
    private ParishRepository parishRepository;
    @MockBean
    private CityMapper cityMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CityService cityService;
    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @CsvSource({"{}", "{\"name\":\"\"}"})
    void whenCreateRequestHasNoName_then400_andCITY001(final String requestBody) throws Exception {

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(ROOT_PATH),
                                jsonPath("$.requestInfo.method").value("POST"),
                                jsonPath("$.requestInfo.pathInfo").value(ROOT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("name"),
                                jsonPath("$.errors[0].code").value("CITY.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateRequestValid_then201() throws Exception {

        final String requestBody = "{\"name\":\"testName\",\"key\":\"testKey\"}";

        final City cityStub = City.builder()
                .id("stubId")
                .key("stubKey")
                .name("stubName")
                .build();

        final ArgumentCaptor<City> cityCaptor = ArgumentCaptor.forClass(City.class);

        when(cityRepository.save(cityCaptor.capture())).thenReturn(cityStub);

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(cityStub))
                );

        final City valueForStore = cityCaptor.getValue();

        assertAll(
                () -> verify(cityRepository).save(valueForStore),
                () -> assertThat(valueForStore.getId()).isNull(),
                () -> assertThat(valueForStore.getName()).isEqualTo("testName"),
                () -> assertThat(valueForStore.getKey()).isEqualTo("testKey")
        );
    }

    @ParameterizedTest
    @CsvSource({"{}", "{\"name\":\"\"}"})
    void whenCreateLocalizedRequestHasName_then400_andCITY001(final String requestBody) throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_CITY_ID/lang/ru";

        mockMvc.perform(put(validTestUri)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(validTestUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(validTestUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("name"),
                                jsonPath("$.errors[0].code").value("CITY.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateLocalizedRequestValid_andLocaleNotAvailable_then400_andCITY002() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_CITY_ID/lang/UNKNOWN_LOCALE";

        mockMvc.perform(put(validTestUri)
                        .contentType("application/json")
                        .content("{\"name\":\"any\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(validTestUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(validTestUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("locale"),
                                jsonPath("$.errors[0].code").value("CITY.002"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateLocalizedLocaleRequestValid_andCityNotFound_then404() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_CITY_ID/lang/ru";

        when(cityRepository.findById("TEST_CITY_ID")).thenReturn(Optional.empty());

        mockMvc.perform(put(validTestUri)
                        .contentType("application/json")
                        .content("{\"name\":\"any\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(validTestUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(validTestUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verify(cityRepository).findById("TEST_CITY_ID");
    }

    @Test
    void whenCreateLocalizedLocaleRequestValid_then200_andFirstLocalizedInfoAdded() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_CITY_ID/lang/ru";

        final City city = City.builder()
                .id("TEST_CITY_ID")
                .build();

        final ArgumentCaptor<City> cityCaptor = ArgumentCaptor.forClass(City.class);

        when(cityRepository.findById("TEST_CITY_ID")).thenReturn(Optional.of(city));
        when(cityRepository.save(cityCaptor.capture())).then(invocationOnMock -> invocationOnMock.getArguments()[0]);

        mockMvc.perform(put(validTestUri)
                        .contentType("application/json")
                        .content("{\"name\":\"anyName\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("TEST_CITY_ID"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        final City updatedCity = cityCaptor.getValue();
        final LocalizedCity ru = (LocalizedCity) updatedCity.getLocalizedInfo().get("ru");

        assertAll(
                () -> verify(cityRepository).findById("TEST_CITY_ID"),
                () -> verify(cityRepository).save(updatedCity),
                () -> assertThat(ru.getName()).isEqualTo("anyName"),
                () -> assertThat(ru.getOriginObjId()).isEqualTo("TEST_CITY_ID"),
                () -> assertThat(ru.getLang()).isEqualTo("ru")
        );
    }

    @Test
    void whenGetAllCityRequestHasNoParams_then200_andDefaultValuesUsed() throws Exception {

        final City firstCity = City.builder()
                .id("FIRST_CITY_ID")
                .build();
        final City secondCity = City.builder()
                .id("SECOND_CITY_ID")
                .build();
        final PageRequest defaultPageRequest = PageRequest.of(0, 40);
        final PageImpl<City> responsePage = new PageImpl<>(Arrays.asList(firstCity, secondCity), defaultPageRequest, 2);

        final ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(cityRepository.findAll(pageRequestArgumentCaptor.capture())).thenReturn(responsePage);

        mockMvc.perform(get(ROOT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );

        final PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

        assertAll(
                () -> verify(cityRepository).findAll(capturedPageRequest),
                () -> assertThat(capturedPageRequest.getPageSize()).isEqualTo(40),
                () -> assertThat(capturedPageRequest.getPageNumber()).isZero()
        );
    }

    @Test
    void whenGetAllCityRequestHasParams_then200() throws Exception {

        final String testUri = ROOT_PATH + "?page=11&size=23";

        final City firstCity = City.builder()
                .id("FIRST_CITY_ID")
                .build();
        final City secondCity = City.builder()
                .id("SECOND_CITY_ID")
                .build();
        final PageRequest currentPageRequest = PageRequest.of(11, 23);
        final PageImpl<City> responsePage = new PageImpl<>(Arrays.asList(firstCity, secondCity), currentPageRequest, 2);

        final ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(cityRepository.findAll(pageRequestArgumentCaptor.capture())).thenReturn(responsePage);

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );

        final PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

        assertAll(
                () -> verify(cityRepository).findAll(capturedPageRequest),
                () -> assertThat(capturedPageRequest.getPageSize()).isEqualTo(23),
                () -> assertThat(capturedPageRequest.getPageNumber()).isEqualTo(11)
        );
    }

    @Test
    void whenGetCityRequestValid_andCityNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(cityRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );
    }

    @Test
    void whenGetCityRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";
        final City city = City.builder()
                .id("any_id")
                .name("any_name")
                .key("any_key")
                .build();

        when(cityRepository.findById("any_id")).thenReturn(Optional.of(city));

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(city))
                );
    }

    @Test
    void whenUpdateCityRequestValid_andCityNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(cityRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );
    }

    @Test
    void whenUpdateCityRequestValid_andCityFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final City city = City.builder()
                .id("any_id")
                .name("name")
                .key("key")
                .build();
        final ArgumentCaptor<CityInfo> cityInfoArgumentCaptor = ArgumentCaptor.forClass(CityInfo.class);

        when(cityRepository.findById("any_id")).thenReturn(Optional.of(city));
        doNothing().when(cityMapper).updateCityFromDTO(cityInfoArgumentCaptor.capture(), eq(city));
        when(cityRepository.save(city)).thenReturn(city);

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{\"name\":\"newName\",\"key\":\"newKey\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        final CityInfo cityInfo = cityInfoArgumentCaptor.getValue();

        assertAll(
                () -> verify(cityRepository).findById("any_id"),
                () -> verify(cityRepository).save(city),
                () -> verify(cityMapper).updateCityFromDTO(cityInfo, city),
                () -> assertThat(cityInfo.getKey()).isEqualTo("newKey"),
                () -> assertThat(cityInfo.getName()).isEqualTo("newName")
        );
    }

    @Test
    void whenDeleteCityRequestValid_andCityNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(cityRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );
    }

    @Test
    void whenDeleteCityRequestValid_andCityFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final City city = City.builder()
                .id("any_id")
                .name("name")
                .key("key")
                .build();

        when(cityRepository.findById("any_id")).thenReturn(Optional.of(city));
        doNothing().when(cityRepository).deleteById("any_id");

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("DELETED")
                        )
                );

        assertAll(
                () -> verify(cityRepository).findById("any_id"),
                () -> verify(cityRepository).deleteById("any_id")
        );
    }
}
