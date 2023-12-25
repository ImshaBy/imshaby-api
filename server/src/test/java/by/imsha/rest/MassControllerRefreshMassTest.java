package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.LocalizedMass;
import by.imsha.domain.Mass;
import by.imsha.properties.ImshaProperties;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.service.CityService;
import by.imsha.service.DefaultCityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import org.junit.jupiter.api.BeforeEach;
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

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, MassService.class, TestTimeConfiguration.class})
class MassControllerRefreshMassTest {

    private static final String MASS_ID_STUB = "massIdStub";
    private static final String REFRESH_MASS_END_POINT_PATH = "/api/mass/refresh/" + MASS_ID_STUB;

    @MockBean
    private MassRepository massRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MassService massService;
    @MockBean
    private ParishService parishService;
    @MockBean
    private CityService cityService;
    @MockBean
    private ScheduleFactory scheduleFactory;
    @MockBean
    private ParishRepository parishRepository;
    @MockBean
    private ImshaProperties imshaProperties;
    @MockBean
    private DefaultCityService defaultCityService;

    @BeforeEach
    void setUp() {
        //т.к. валидация запускается сервисом, то необходимо ешё добраться до вызова сервиса
        final Mass massStub = new Mass();
        massStub.setId(MASS_ID_STUB);

        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.of(massStub));
        when(massRepository.save(any(Mass.class))).then(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @Test
    void whenMassFound_then200() throws Exception {
        final Mass resultMass = new Mass();
        resultMass.setId(MASS_ID_STUB);
        resultMass.setStartDate(LocalDate.of(2023, Month.JULY, 25));
        resultMass.setEndDate(LocalDate.of(2023, Month.JULY, 25));
        resultMass.setDeleted(true);
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

        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.of(resultMass));

        mockMvc.perform(put(REFRESH_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value(MASS_ID_STUB),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        verify(massRepository).findById(MASS_ID_STUB);
        verify(massRepository).save(argThat(mass -> MASS_ID_STUB.equals(mass.getId())));
    }

    @Test
    void whenTimeIsInvalid_then400_and_MASS005() throws Exception {
        final Mass validStoredMass = new Mass();
        validStoredMass.setId(MASS_ID_STUB);
        validStoredMass.setStartDate(LocalDate.of(2023, Month.JULY, 25));
        validStoredMass.setEndDate(LocalDate.of(2023, Month.AUGUST, 25));
        validStoredMass.setDays(new int[]{1, 2, 3, 4, 5, 6, 7});
        validStoredMass.setTime("16:45");
        validStoredMass.setDeleted(false);
        validStoredMass.setCityId("cityId");
        validStoredMass.setLangCode("langCode");
        validStoredMass.setDuration(11L);
        validStoredMass.setNotes("notes");
        validStoredMass.setOnline(true);
        validStoredMass.setRorate(true);
        validStoredMass.setSingleStartTimestamp(0L);
        validStoredMass.setLastModifiedDate(LocalDateTime.of(LocalDate.of(2023, Month.JULY, 24), LocalTime.of(19, 14)));
        validStoredMass.setParishId("parishId");
        validStoredMass.getLocalizedInfo().put("infoKey", new LocalizedMass("someValue"));

        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.of(validStoredMass));

        mockMvc.perform(put(REFRESH_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content("{\"time\":\"29:00\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REFRESH_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(REFRESH_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.005"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );

        verify(massRepository).findById(MASS_ID_STUB);
    }

    @Test
    void whenMassNotFound_then404() throws Exception {
        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.empty());

        mockMvc.perform(put(REFRESH_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(REFRESH_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(REFRESH_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verify(massRepository).findById(MASS_ID_STUB);
    }

    @Test
    void whenEmptyMassId_then404() throws Exception {
        final String testUri = "/api/mass/refresh/";

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound());

        verifyNoInteractions(massRepository);
    }
}
