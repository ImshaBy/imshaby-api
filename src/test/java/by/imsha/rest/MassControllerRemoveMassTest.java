package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.Mass;
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
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerRemoveMassTest {

    private static final String REMOVE_MASS_END_POINT_PATH = "/api/mass/";

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

    @Test
    void whenMassIdNotSpecified_then400() throws Exception {
        mockMvc.perform(delete(REMOVE_MASS_END_POINT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REMOVE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("DELETE"),
                                jsonPath("$.requestInfo.pathInfo").value(REMOVE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }

    @Test
    void whenMassNotFound_then404() throws Exception {
        final String testUri = REMOVE_MASS_END_POINT_PATH + "123";

        when(massService.getMass("123")).thenReturn(Optional.empty());

        mockMvc.perform(delete(testUri)
                        .contentType("application/json")
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

        verify(massService).getMass("123");
    }

    @Test
    void whenDeleteExistingMass_then200() throws Exception {
        final Mass mass = mock(Mass.class);

        when(massService.getMass("123")).thenReturn(Optional.of(mass));
        when(mass.getId()).thenReturn("massId");

        mockMvc.perform(delete(REMOVE_MASS_END_POINT_PATH + "123")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("123"),
                                jsonPath("$.status").value("DELETED")
                        )
                );

        verify(massService).getMass("123");
        verify(massService).removeMass(mass);
        verifyNoMoreInteractions(massService);
    }
}
