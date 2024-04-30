package by.imsha.server.rest;

import by.imsha.ValidationConfiguration;
import by.imsha.domain.Parish;
import by.imsha.meilisearch.reader.MeilisearchReader;
import by.imsha.properties.ImshaProperties;
import by.imsha.rest.MassController;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.service.CityService;
import by.imsha.service.DefaultCityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH = "/api/mass";

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
    @MockBean
    private MeilisearchReader meilisearchReader;

    @Test
    void whenParishHasNoMasses_then200() throws Exception {
        final ArgumentCaptor<Parish> parishArgumentCaptor = org.mockito.ArgumentCaptor.forClass(Parish.class);
        final Parish parish = new Parish();
        parish.setId("any_id");

        when(parishService.getParish("any_id")).thenReturn(Optional.of(parish));

        mockMvc.perform(put(REFRESH_MASSES_BY_PARISH_ID_END_POINT_PATH + "?parishId=any_id")
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
