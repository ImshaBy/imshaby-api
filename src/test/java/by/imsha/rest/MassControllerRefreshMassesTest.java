package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.Mass;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class MassControllerRefreshMassesTest {

    private static final String REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH = "/api/mass/";

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
    void whenParishHasNoMasses_then200() throws Exception {
        when(massService.getMassByParish("123")).thenReturn(Collections.emptyList());

        mockMvc.perform(put(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=123")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.entities", hasSize(0)),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        verify(massService).getMassByParish("123");
        verifyNoMoreInteractions(massService);
    }

    @Test
    void whenParishHasMasses_then200() throws Exception {
        final Mass firstMass = mock(Mass.class);
        final Mass secondMass = mock(Mass.class);

        when(massService.getMassByParish("123")).thenReturn(Arrays.asList(firstMass, secondMass));
        when(firstMass.getId()).thenReturn("firstMassId");
        when(secondMass.getId()).thenReturn("secondMassId");

        mockMvc.perform(put(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=123")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.entities", hasSize(2)),
                                jsonPath("$.entities", containsInAnyOrder("firstMassId", "secondMassId")),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        verify(massService).getMassByParish("123");
        verify(massService).updateMass(firstMass);
        verify(massService).updateMass(secondMass);
        verifyNoMoreInteractions(massService);
    }

    @Test
    void whenRequestHasNoParishId_then200() throws Exception {
        mockMvc.perform(put(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verifyNoInteractions(massService);
    }
}
