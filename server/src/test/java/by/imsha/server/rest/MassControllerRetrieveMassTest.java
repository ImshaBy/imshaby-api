package by.imsha.server.rest;

import by.imsha.domain.LocalizedMass;
import by.imsha.domain.Mass;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.server.ValidationConfiguration;
import by.imsha.server.properties.ImshaProperties;
import by.imsha.service.CityService;
import by.imsha.service.DefaultCityService;
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
import java.util.Optional;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
class MassControllerRetrieveMassTest {

    private static final String RETRIEVE_MASS_END_POINT_PATH = "/api/mass";

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
    void whenMassNotFound_then404() throws Exception {
        final String massId = "123";
        final String testUri = RETRIEVE_MASS_END_POINT_PATH + "/" + massId;

        when(massService.getMass(massId)).thenReturn(Optional.empty());

        mockMvc.perform(get(testUri)
                        .contentType("application/json")
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

        verify(massService).getMass(massId);
    }

    @Test
    void whenEmptyMassId_then404() throws Exception {
        final String massId = "";
        final String testUri = RETRIEVE_MASS_END_POINT_PATH + "/" + massId;

        mockMvc.perform(get(testUri)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound());

        verifyNoInteractions(massService);
    }

    @Test
    void whenMassFound_then200() throws Exception {
        final String massId = "1234";
        final String testUri = RETRIEVE_MASS_END_POINT_PATH + "/" + massId;

        final Mass mass = new Mass();
        mass.setId(massId);
        mass.setStartDate(LocalDate.of(2023, Month.JULY, 24));
        mass.setEndDate(LocalDate.of(2023, Month.JULY, 24));
        mass.setDays(new int[]{1, 2});
        mass.setDeleted(true);
        mass.setTime("16:45");
        mass.setCityId("cityId");
        mass.setLangCode("langCode");
        mass.setDuration(10L);
        mass.setNotes("notes");
        mass.setOnline(true);
        mass.setRorate(true);
        mass.setSingleStartTimestamp(1L);
        mass.setLastModifiedDate(LocalDateTime.of(LocalDate.of(2023, Month.JULY, 24), LocalTime.of(19, 14)));
        mass.setParishId("parishId");
        mass.getLocalizedInfo().put("infoKey", new LocalizedMass("someValue"));

        when(massService.getMass(massId)).thenReturn(Optional.of(mass));

        mockMvc.perform(get(testUri)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(mass))
                );

        verify(massService).getMass(massId);
    }
}
