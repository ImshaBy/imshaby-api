package by.imsha.server.rest;

import by.imsha.domain.LocalizedMass;
import by.imsha.domain.Mass;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.server.ValidationConfiguration;
import by.imsha.server.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.server.service.DefaultCityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerFilterMassesTest {

    private static final String FILTER_MASSES_END_POINT_PATH = "/api/mass";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MassService massService;
    @MockBean
    private ParishService parishService;
    @MockBean
    private CityService cityService;
    @MockBean
    private ScheduleFactory scheduleFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private DefaultCityService defaultCityService;

    @Test
    void whenNoParamsSpecified_then400() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenOnlyFilterSpecified_then200_andOtherParamsHaveDefaultValues() throws Exception {
        final Mass resultMass = new Mass();
        resultMass.setId("massId");
        resultMass.setStartDate(LocalDate.of(2023, Month.JULY, 25));
        resultMass.setEndDate(LocalDate.of(2023, Month.JULY, 25));
        resultMass.setDays(new int[]{1, 2});
        resultMass.setDeleted(true);
        resultMass.setTime("15:45");
        resultMass.setCityId("cityId");
        resultMass.setLangCode("langCode");
        resultMass.setDuration(11L);
        resultMass.setNotes("notes");
        resultMass.setOnline(true);
        resultMass.setRorate(true);
        resultMass.setSingleStartTimestamp(1L);
        resultMass.setLastModifiedDate(LocalDateTime.of(LocalDate.of(2023, Month.JULY, 24), LocalTime.of(19, 14)));
        resultMass.setParishId("parishId");
        resultMass.getLocalizedInfo().put("infoKey", new LocalizedMass("someValue"));

        final Mass massCopy = new Mass(resultMass);
        massCopy.setId("copyId");

        final List<Mass> responseData = Arrays.asList(resultMass, massCopy);
        when(massService.search("a", 0, 10, "+name")).thenReturn(responseData);

        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=a")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responseData))
                );

        verify(massService).search("a", 0, 10, "+name");
        verifyNoMoreInteractions(massService);
    }

    @Test
    void whenLimitParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=qwe&limit=abc")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("filter=qwe&limit=abc"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenLimitParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=qwe&limit=123")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(massService).search("qwe", 0, 123, "+name");
    }

    @Test
    void whenOffsetParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=qwe&offset=abc")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(FILTER_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("filter=qwe&offset=abc"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenOffsetParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=qwe&offset=123")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(massService).search("qwe", 123, 10, "+name");
    }

    @Test
    void whenSortParamHasValidValue_then200() throws Exception {
        mockMvc.perform(get(FILTER_MASSES_END_POINT_PATH + "?filter=qwe&sort=123zxc+")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(massService).search("qwe", 0, 10, "123zxc+");
    }
}