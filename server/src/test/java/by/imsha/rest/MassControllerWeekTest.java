package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassNav;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.service.DefaultCityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.DateTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerWeekTest {

    private static final String WEEK_MASSES_END_POINT_PATH = "/api/mass/week";

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
    private DateTimeProvider dateTimeProvider;
    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private DefaultCityService defaultCityService;

    @Test
    void whenNoParamsSpecified_then200() throws Exception {
        final Mass firstMass = new Mass();
        firstMass.setId("firstMass");
        final Mass secondMass = new Mass();
        secondMass.setId("secondMass");
        final List<Mass> masses = Arrays.asList(firstMass, secondMass);
        final LocalDate today = dateTimeProvider.today();
        final MassSchedule massSchedule = new MassSchedule(today);
        final MassNav massNav = new MassNav();

        //когда нет параметров, запрашивается город по умолчанию
        when(defaultCityService.getCityIdOrDefault(null)).thenReturn("defaultCity");
        when(massService.getMassByCity("defaultCity")).thenReturn(masses);
        when(scheduleFactory.build(masses, today)).thenReturn(massSchedule);
        //параметр online имеет по-умолчанию значение "false"
        when(massService.buildMassNavigation(massSchedule, "defaultCity", null, "false", null, false))
                .thenReturn(massNav);

        mockMvc.perform(get(WEEK_MASSES_END_POINT_PATH)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(defaultCityService).getCityIdOrDefault(null);
        verify(massService).getMassByCity("defaultCity");
        verify(scheduleFactory).build(masses, today);
        verify(massService).buildMassNavigation(massSchedule, "defaultCity", null, "false", null, false);
    }

    @Test
    void whenDateParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(WEEK_MASSES_END_POINT_PATH + "?date=123")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("date=123"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenOnlineParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(WEEK_MASSES_END_POINT_PATH + "?online=asd")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("online=asd"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenRorateParamHasInvalidValue_then400() throws Exception {
        mockMvc.perform(get(WEEK_MASSES_END_POINT_PATH + "?rorate=asd")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("GET"),
                                jsonPath("$.requestInfo.pathInfo").value(WEEK_MASSES_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("rorate=asd"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenAllParamsHaveValidValues_then200() throws Exception {
        final Mass firstMass = new Mass();
        firstMass.setId("firstMass");
        final Mass secondMass = new Mass();
        secondMass.setId("secondMass");
        final List<Mass> masses = Arrays.asList(firstMass, secondMass);
        final LocalDate date = LocalDate.of(2023, 7, 26);
        final MassSchedule massSchedule = new MassSchedule(date);
        final MassNav massNav = new MassNav();
        final Parish parish = mock(Parish.class);

        when(parish.getCityId()).thenReturn("parishCityId");
        when(parishService.getParish("qwe")).thenReturn(Optional.of(parish));
        when(massService.getMassByParish("qwe")).thenReturn(masses);
        when(scheduleFactory.build(masses, date)).thenReturn(massSchedule);
        //параметр online имеет по-умолчанию значение "false"
        when(massService.buildMassNavigation(massSchedule, "parishCityId", "qwe", "true", "be", true))
                .thenReturn(massNav);
        when(massService.filterOutOnlyOnline(masses)).thenReturn(masses);
        when(massService.filterByMassLang(masses, "be")).thenReturn(masses);
        when(massService.filterOutRorateOnly(masses)).thenReturn(masses);

        mockMvc.perform(get(WEEK_MASSES_END_POINT_PATH + "?cityId=123&date=26-07-2023&parishId=qwe&online=true&massLang=be&rorate=true")
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(parish).getCityId();
        verify(parishService).getParish("qwe");
        verify(massService).getMassByParish("qwe");
        verify(scheduleFactory).build(masses, date);
        verify(massService).buildMassNavigation(massSchedule, "parishCityId", "qwe", "true", "be", true);
        verify(massService).filterOutOnlyOnline(masses);
        verify(massService).filterByMassLang(masses, "be");
        verify(massService).filterOutRorateOnly(masses);
    }
}
