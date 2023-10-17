package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerRemoveMassesByParishIdTest {

    private static final String REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH = "/api/mass";

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

    @Test
    void whenMassIdNotSpecified_then400() throws Exception {
        mockMvc.perform(delete(REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("parishId"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenMassNotFound_then404() throws Exception {
        final String testUri = REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=123";

        when(parishService.getParish("123")).thenReturn(Optional.empty());

        mockMvc.perform(delete(testUri)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").value("parishId=123"),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verify(parishService).getParish("123");
    }

    @Test
    void whenNoMassesWereDeleted_then200() throws Exception {
        final String testUri = REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=123";

        when(parishService.getParish("123")).thenReturn(Optional.of(new Parish()));
        when(massService.removeMasses("123")).thenReturn(Collections.emptyList());

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.entities", hasSize(0)),
                                jsonPath("$.status").value("DELETED")
                        )
                );

        verify(parishService).getParish("123");
        verify(massService).removeMasses("123");
        verifyNoMoreInteractions(parishService);
        verifyNoMoreInteractions(massService);
    }

    @Test
    void whenMassesWereDeleted_then200() throws Exception {
        final String testUri = REMOVE_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=123";
        final Mass firstMass = mock(Mass.class);
        final Mass secondMass = mock(Mass.class);

        when(parishService.getParish("123")).thenReturn(Optional.of(new Parish()));
        when(massService.removeMasses("123")).thenReturn(Arrays.asList(firstMass, secondMass));
        when(firstMass.getId()).thenReturn("firstMassId");
        when(secondMass.getId()).thenReturn("secondMassId");

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.entities", containsInAnyOrder("firstMassId", "secondMassId")),
                                jsonPath("$.status").value("DELETED")
                        )
                );

        verify(parishService).getParish("123");
        verify(massService).removeMasses("123");
        verifyNoMoreInteractions(parishService);
        verifyNoMoreInteractions(massService);
    }
}
