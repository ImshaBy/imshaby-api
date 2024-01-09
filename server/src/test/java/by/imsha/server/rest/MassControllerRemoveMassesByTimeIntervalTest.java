package by.imsha.server.rest;

import by.imsha.domain.Mass;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.server.ValidationConfiguration;
import by.imsha.server.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.server.service.DefaultCityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerRemoveMassesByTimeIntervalTest {

    private static final String REMOVE_MASSES_BY_TIME_INTERVAL_END_POINT_PATH = "/api/mass";

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
    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private DefaultCityService defaultCityService;

    @CsvSource({
            "from",
            "from=a-07-2023",
            "from=15-07-2023&to=a"
    })
    @ParameterizedTest
    void whenInvalidRequest_then400(final String query) throws Exception {
        final String testUri = REMOVE_MASSES_BY_TIME_INTERVAL_END_POINT_PATH + "/123";

        mockMvc.perform(delete(testUri + "?" + query)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").value(query),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenToDateIsBeforeAfterDate_then400_andMass012() throws Exception {
        final String testUri = REMOVE_MASSES_BY_TIME_INTERVAL_END_POINT_PATH + "/123";

        when(massService.getMass("123")).thenReturn(Optional.of(new Mass()));

        mockMvc.perform(delete(testUri + "?from=15-07-2023&to=14-07-2023")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").value("from=15-07-2023&to=14-07-2023"),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("from"),
                                jsonPath("$.errors[0].code").value("MASS.012"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );

        verify(massService).getMass("123");
        verifyNoMoreInteractions(massService);
    }

    @Test
    void whenMassFoundAndDatesValid_then200() throws Exception {
        final String testUri = REMOVE_MASSES_BY_TIME_INTERVAL_END_POINT_PATH + "/123";
        final Mass mass = mock(Mass.class);

        when(massService.getMass("123")).thenReturn(Optional.of(mass));
        when(massService.removeMass(mass, LocalDate.of(2023, 7, 15), LocalDate.of(2023, 7, 16)))
                .thenReturn(Triple.of("1", "2", "3"));

        mockMvc.perform(delete(testUri + "?from=15-07-2023&to=16-07-2023")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json("[{\"id\":\"1\",\"status\":\"UPDATED\"},{\"id\":\"2\",\"status\":\"CREATED\"},{\"id\":\"3\",\"status\":\"DELETED\"}]")
                );

    }
}
