package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.domain.dto.ParishInfo;
import by.imsha.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.DateTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

@WebMvcTest({ParishController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class ParishControllerTest {
    private static final String ROOT_PATH = "/api/parish";

    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private CityService cityService;
    @MockBean
    private ParishService parishService;
    @MockBean
    private MassService massService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ScheduleFactory scheduleFactory;
    @Autowired
    private DateTimeProvider dateTimeProvider;

    @ParameterizedTest
    @CsvSource({"{\"name\":null}", "{\"name\":\"\"}"})
    void whenCreateRequestHasNoName_then400_andPARISH003(final String requestBody) throws Exception {

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
                                jsonPath("$.errors", hasItem(
                                                allOf(
                                                        hasEntry("field", "name"),
                                                        hasEntry("code", "PARISH.003")
                                                )
                                        )
                                )
                        )
                );
    }

    @ParameterizedTest
    @CsvSource({"{\"cityId\":null}", "{\"cityId\":\"\"}"})
    void whenCreateRequestHasNoCityId_then400_andPARISH004(final String requestBody) throws Exception {

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
                                jsonPath("$.errors", hasItem(
                                                allOf(
                                                        hasEntry("field", "cityId"),
                                                        hasEntry("code", "PARISH.004")
                                                )
                                        )
                                )
                        )
                );
    }

    @Test
    void whenCreateRequestHasInvalidEmail_then400_andPARISH005() throws Exception {

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content("{\"email\": \"123\"}")
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
                                jsonPath("$.errors", hasItem(
                                                allOf(
                                                        hasEntry("field", "email"),
                                                        hasEntry("code", "PARISH.005")
                                                )
                                        )
                                )
                        )
                );
    }

    @Test
    void whenCreateRequestHasInvalidLastModifiedEmail_then400_andPARISH006() throws Exception {

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content("{\"lastModifiedEmail\": \"123\"}")
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
                                jsonPath("$.errors", hasItem(
                                                allOf(
                                                        hasEntry("field", "lastModifiedEmail"),
                                                        hasEntry("code", "PARISH.006")
                                                )
                                        )
                                )
                        )
                );
    }

    @Test
    void whenCreateRequestValid_then201() throws Exception {

        final String requestBody = "{\"name\":\"testName\",\"cityId\":\"testCityId\", \"state\":\"PENDING\"}";

        final Parish parishStub = new Parish();
        parishStub.setId("testId");
        parishStub.setCityId("testCityId");
        parishStub.setName("testName");

        final ArgumentCaptor<Parish> parishCaptor = ArgumentCaptor.forClass(Parish.class);

        when(parishService.createParish(parishCaptor.capture())).thenReturn(parishStub);

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(parishStub))
                );

        final Parish valueForStore = parishCaptor.getValue();

        assertAll(
                () -> verify(parishService).createParish(valueForStore),
                () -> assertThat(valueForStore.getId()).isNull(),
                () -> assertThat(valueForStore.getName()).isEqualTo("testName"),
                () -> assertThat(valueForStore.getCityId()).isEqualTo("testCityId"),
                () -> assertThat(valueForStore.getLastConfirmRelevance()).isNotNull()
        );
    }

    @ParameterizedTest
    @CsvSource({"{}", "{\"name\":\"\"}"})
    void whenCreateLocalizedRequestHasName_then400_andPARISH002(final String requestBody) throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_PARISH_ID/lang/ru";

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
                                jsonPath("$.errors[0].field").value("name"),
                                jsonPath("$.errors[0].code").value("PARISH.002"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateLocalizedRequestValid_andLocaleNotAvailable_then400_andPARISH001() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_PARISH_ID/lang/UNKNOWN_LOCALE";

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
                                jsonPath("$.errors[0].code").value("PARISH.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateLocalizedLocaleRequestValid_andParishNotFound_then404() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_PARISH_ID/lang/ru";

        when(parishService.getParish("TEST_PARISH_ID")).thenReturn(Optional.empty());

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

        verify(parishService).getParish("TEST_PARISH_ID");
    }

    @Test
    void whenCreateLocalizedLocaleRequestValid_then200_andFirstLocalizedInfoAdded() throws Exception {

        final String validTestUri = ROOT_PATH + "/TEST_PARISH_ID/lang/ru";

        final Parish parish = new Parish();
        parish.setId("TEST_PARISH_ID");
        parish.setName("testName");
        parish.setCityId("TEST_CITY_ID");

        final ArgumentCaptor<LocalizedParish> localizedParishCaptor = ArgumentCaptor.forClass(LocalizedParish.class);

        when(parishService.getParish("TEST_PARISH_ID")).thenReturn(Optional.of(parish));
        when(parishService.updateLocalizedParishInfo(localizedParishCaptor.capture(), same(parish)))
                .then(invocationOnMock -> invocationOnMock.getArguments()[1]);

        mockMvc.perform(put(validTestUri)
                        .contentType("application/json")
                        .content("{\"name\":\"anyName\",\"shortName\":\"anyShortName\",\"address\":\"anyAddress\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("TEST_PARISH_ID"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        final LocalizedParish localizedParish = localizedParishCaptor.getValue();

        assertAll(
                () -> verify(parishService).getParish("TEST_PARISH_ID"),
                () -> verify(parishService).updateLocalizedParishInfo(localizedParish, parish),
                () -> assertThat(localizedParish.getName()).isEqualTo("anyName"),
                () -> assertThat(localizedParish.getOriginObjId()).isEqualTo("TEST_PARISH_ID"),
                () -> assertThat(localizedParish.getLang()).isEqualTo("ru"),
                () -> assertThat(localizedParish.getAddress()).isEqualTo("anyAddress"),
                () -> assertThat(localizedParish.getShortName()).isEqualTo("anyShortName")
        );
    }

    @Test
    void whenGetParishRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

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
    void whenGetParishRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";
        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setName("anyName");

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(parish))
                );

        verify(parishService).getParish("any_id");
    }

    @Test
    void whenUpdateParishRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

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

        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setName("anyName");

        final ArgumentCaptor<ParishInfo> parishInfoArgumentCaptor = ArgumentCaptor.forClass(ParishInfo.class);

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));
        when(parishService.updateParish(parishInfoArgumentCaptor.capture(), same(parish)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[1]);
        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{\"name\":\"newName\"," +
                                "\"shortName\":\"newShortName\"," +
                                "\"imgPath\":\"newImgPath\"," +
                                "\"gps\": {\"latitude\":1.1,\"longitude\":2.2}," +
                                "\"address\":\"newAddress\"," +
                                "\"updatePeriodInDays\":\"15\"," +
                                "\"supportPhone\":\"newSupportPhone\"," +
                                "\"email\":\"newEmail\"," +
                                "\"key\":\"newKey\"," +
                                "\"phone\":\"newPhone\"," +
                                "\"lastModifiedEmail\":\"newLastModifiedEmail\"," +
                                "\"website\":\"newWebsite\"," +
                                "\"broadcastUrl\":\"newBroadcastUrl\"}")
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

        final ParishInfo parishInfo = parishInfoArgumentCaptor.getValue();

        assertAll(
                () -> verify(parishService).getParish("any_id"),
                () -> verify(parishService).updateParish(parishInfo, parish),
                () -> assertThat(parishInfo.getName()).isEqualTo("newName"),
                () -> assertThat(parishInfo.getShortName()).isEqualTo("newShortName"),
                () -> assertThat(parishInfo.getImgPath()).isEqualTo("newImgPath"),
                () -> assertThat(parishInfo.getGps().getLatitude()).isEqualTo(1.1f),
                () -> assertThat(parishInfo.getGps().getLongitude()).isEqualTo(2.2f),
                () -> assertThat(parishInfo.getAddress()).isEqualTo("newAddress"),
                () -> assertThat(parishInfo.getUpdatePeriodInDays()).isEqualTo(15),
                () -> assertThat(parishInfo.getSupportPhone()).isEqualTo("newSupportPhone"),
                () -> assertThat(parishInfo.getEmail()).isEqualTo("newEmail"),
                () -> assertThat(parishInfo.getKey()).isEqualTo("newKey"),
                () -> assertThat(parishInfo.getPhone()).isEqualTo("newPhone"),
                () -> assertThat(parishInfo.getLastModifiedEmail()).isEqualTo("newLastModifiedEmail"),
                () -> assertThat(parishInfo.getWebsite()).isEqualTo("newWebsite"),
                () -> assertThat(parishInfo.getBroadcastUrl()).isEqualTo("newBroadcastUrl")
        );
    }

    @Test
    void whenDeleteParishRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

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
    void whenDeleteParishRequestValid_andParishFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setName("anyName");

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));
        doNothing().when(parishService).removeParish("any_id");

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
                () -> verify(parishService).getParish("any_id"),
                () -> verify(parishService).removeParish("any_id")
        );
    }

    @Test
    void whenCascadeDeleteParishRequestValid_andParishFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id?cascade=true";

        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setName("anyName");

        final Mass firstMass = new Mass();
        firstMass.setId("firstMassId");
        final Mass secondMass = new Mass();
        secondMass.setId("secondMassId");

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));
        when(massService.getMassByParish("any_id")).thenReturn(Arrays.asList(firstMass, secondMass));
        doNothing().when(parishService).removeParish("any_id");

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("DELETED"),
                                jsonPath("$.relatedEntities", hasSize(2)),
                                jsonPath("$.relatedEntities", hasItem(
                                                allOf(
                                                        hasEntry("id", "firstMassId"),
                                                        hasEntry("status", "DELETED")
                                                )
                                        )
                                ),
                                jsonPath("$.relatedEntities", hasItem(
                                                allOf(
                                                        hasEntry("id", "secondMassId"),
                                                        hasEntry("status", "DELETED")
                                                )
                                        )
                                )
                        )
                );

        assertAll(
                () -> verify(parishService).getParish("any_id"),
                () -> verify(parishService).removeParish("any_id"),
                () -> verify(massService).removeMass(firstMass),
                () -> verify(massService).removeMass(secondMass)
        );
    }

    @Test
    void whenGetParishByUserRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/user/any_id";

        when(parishService.getParishByUser("any_id")).thenReturn(null);

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
    void whenGetParishByUserRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/user/any_id";
        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setName("anyName");

        when(parishService.getParishByUser("any_id")).thenReturn(parish);

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(parish))
                );

        verify(parishService).getParishByUser("any_id");
    }

    @Test
    void whenGetExpiredCityParishesRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/week/expired";
        final LocalDate fixedTestDate = dateTimeProvider.today();

        MassSchedule massSchedule = new MassSchedule(fixedTestDate);

        when(cityService.getCityIdOrDefault(null)).thenReturn("defaultCityId");
        when(massService.getMassByCity("defaultCityId")).thenReturn(mock(List.class));
        when(scheduleFactory.build(any(List.class), eq(fixedTestDate))).thenReturn(massSchedule);

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenNoParamsSpecified_then400() throws Exception {
        mockMvc.perform(get(ROOT_PATH)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(ROOT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(ROOT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(parishService);
    }

    @Test
    void whenOnlyFilterSpecified_then200_andOtherParamsHaveDefaultValues() throws Exception {
        final Parish resultParish = new Parish();
        resultParish.setId("massId");
        resultParish.setName("test");

        final List<Parish> responseData = Arrays.asList(resultParish);
        when(parishService.search("a", 0, 10, "+name")).thenReturn(responseData);

        mockMvc.perform(get(ROOT_PATH + "?filter=a")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responseData))
                );

        verify(parishService).search("a", 0, 10, "+name");
        verifyNoMoreInteractions(parishService);
    }

    @Test
    void whenLimitParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(ROOT_PATH + "?filter=qwe&limit=abc")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(ROOT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(ROOT_PATH),
                                jsonPath("$.requestInfo.query").value("filter=qwe&limit=abc"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(parishService);
    }

    @Test
    void whenLimitParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(ROOT_PATH + "?filter=qwe&limit=123")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(parishService).search("qwe", 0, 123, "+name");
    }

    @Test
    void whenOffsetParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(ROOT_PATH + "?filter=qwe&offset=abc")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(ROOT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(ROOT_PATH),
                                jsonPath("$.requestInfo.query").value("filter=qwe&offset=abc"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(parishService);
    }

    @Test
    void whenOffsetParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(ROOT_PATH + "?filter=qwe&offset=123")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(parishService).search("qwe", 123, 10, "+name");
    }

    @Test
    void whenSortParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(ROOT_PATH + "?filter=qwe&sort=123zxc+")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(parishService).search("qwe", 0, 10, "123zxc+");
    }

    @Test
    void whenGetParishStateRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

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


    @ParameterizedTest
    @CsvSource({"PENDING", "APPROVED"})
    void whenGetParishStateRequestValid_then200(Parish.State state) throws Exception {

        final String testUri = ROOT_PATH + "/any_id/state";
        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setState(state);

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.state").value(state.name())
                );

        verify(parishService).getParish("any_id");
    }

    @Test
    void whenUpdateStateRequestHasNoState_then400_andPARISH401() throws Exception {

        final String testUrl = ROOT_PATH + "/any_id/state";

        mockMvc.perform(put(testUrl)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUrl),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(testUrl),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasItem(
                                                allOf(
                                                        hasEntry("field", "state"),
                                                        hasEntry("code", "PARISH.401")
                                                )
                                        )
                                )
                        )
                );
    }

    @Test
    void whenUpdateStateRequestHasNoState_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id/state";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

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
    void whenGetParishStateRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id/state";
        final Parish parish = new Parish();
        parish.setId("any_id");
        parish.setState(Parish.State.PENDING);

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));
        when(parishService.updateParish(argThat(parishToSave -> parishToSave.getState() == Parish.State.APPROVED)))
                .then(invocationOnMock -> invocationOnMock.getArguments()[0]);

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{\"state\":\"APPROVED\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        verify(parishService).getParish("any_id");
        verify(parishService).updateParish(parish);
    }

    @Test
    void whenConfirmRelevanceRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id/confirm-relevance";
        final ArgumentCaptor<Parish> parishArgumentCaptor = ArgumentCaptor.forClass(Parish.class);
        final Parish parish = new Parish();
        parish.setId("any_id");

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));

        mockMvc.perform(post(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.entities").value("any_id"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        verify(parishService).getParish("any_id");
        verify(parishService).updateParish(parishArgumentCaptor.capture());
        verifyNoMoreInteractions(parishService);

        assertThat(parishArgumentCaptor.getValue().getLastConfirmRelevance()).isNotNull();
    }

    @Test
    void whenConfirmRelevanceRequestValid_andParishNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id/confirm-relevance";

        when(parishService.getParish("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(post(testUri)
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
                                jsonPath("$.requestInfo.method").value("POST"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );
    }

}
