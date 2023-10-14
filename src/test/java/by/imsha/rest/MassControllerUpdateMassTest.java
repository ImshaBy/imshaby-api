package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.LocalizedMass;
import by.imsha.domain.Mass;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MassController.class, ValidationConfiguration.class, MassService.class, TestTimeConfiguration.class})
class MassControllerUpdateMassTest {

    private static final String MASS_ID_STUB = "massIdStub";
    private static final String UPDATE_MASS_END_POINT_PATH = "/api/mass/" + MASS_ID_STUB;

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
        String request = "" +
                "{" +
                "\"id\": \"massIdStub\"," +
                "\"cityId\": \"newCityId\"," +
                "\"langCode\": \"newLangCode\"," +
                "\"duration\": 10," +
                "\"online\": true," +
                "\"rorate\": true," +
                "\"parishId\": \"parishId\"," +
                "\"deleted\": true," +
                "\"notes\": \"notes\"," +
                "\"localizedInfo\": {" +
                "  \"infoKey\": {" +
                "    \"notes\": \"someValue\"" +
                "  }" +
                "}," +
                "\"singleStartTimestamp\": 1," +
                "\"lastModifiedDate\": null," +
                "\"startDate\": \"07/24/2023\"," +
                "\"endDate\": \"07/24/2023\"," +
                "\"periodic\": false" +
                "}";

        final Mass resultMass = new Mass();
        resultMass.setId(MASS_ID_STUB);
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

        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.of(resultMass));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(request)
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
        verify(massRepository).save(argThat(mass -> "newCityId".equals(mass.getCityId()) &&
                "newLangCode".equals(mass.getLangCode())));
    }

    @Test
    void whenMassNotFound_then404() throws Exception {
        when(massRepository.findById(MASS_ID_STUB)).thenReturn(Optional.empty());

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );

        verify(massRepository).findById(MASS_ID_STUB);
    }

    @Test
    void whenStartDateLessThanEndDate_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenStartDateEqualToEndDate_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"startDate\":\"01/20/2023\",\n" +
                "    \"endDate\":\"01/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenStartDateGreaterThanEndDate_then400_withError_MASS_001() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 6 ],\n" +
                "    \"startDate\":\"01/21/2023\",\n" +
                "    \"endDate\":\"01/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("startDate"),
                                jsonPath("$.errors[0].code").value("MASS.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenDaysHasValidValues_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 4 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/19/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @CsvSource({
            "1, 01/16/2023",
            "2, 01/17/2023",
            "3, 01/18/2023",
            "4, 01/19/2023",
            "5, 01/20/2023",
            "6, 01/21/2023",
            "7, 01/22/2023"
    })
    @ParameterizedTest
    void whenDaysHasValidValues_forPeriodLessThan1Week_then200(final Integer day, String date) throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ " + day + " ],\n" +
                "    \"startDate\":\"" + date + "\",\n" +
                "    \"endDate\":\"" + date + "\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenDaysHasDuplicates_then400_withError_MASS_002() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 4, 4, 5, 5 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/19/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.002"),
                                jsonPath("$.errors[0].payload.type").value("DUPLICATED"),
                                jsonPath("$.errors[0].payload.days", hasSize(2)),
                                jsonPath("$.errors[0].payload.days[0]").value("4"),
                                jsonPath("$.errors[0].payload.days[1]").value("5")
                        )
                );
    }

    @Test
    void whenDaysGreaterThan7_orLessThan1_then400_withError_MASS_002() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ -1, 0, 8 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/19/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.002"),
                                jsonPath("$.errors[0].payload.type").value("NUMBER_OUT_OF_RANGE"),
                                jsonPath("$.errors[0].payload.days", hasSize(3)),
                                jsonPath("$.errors[0].payload.days", containsInAnyOrder(-1, 0, 8))
                        )
                );
    }

    @Test
    void whenDaysHasNotAvailableValues_forPeriodLessThan1Week_then400_withError_MASS_002() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 1, 2, 3, 4, 5, 6, 7 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/19/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.002"),
                                jsonPath("$.errors[0].payload.type").value("NUMBER_NOT_AVAILABLE"),
                                jsonPath("$.errors[0].payload.days", hasSize(6)),
                                jsonPath("$.errors[0].payload.days", containsInAnyOrder(1, 2, 3, 5, 6, 7))
                        )
                );
    }

    @Test
    void whenNoOtherMassesFound_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"startDate\":\"01/19/2023\",\n" +
                "    \"endDate\":\"01/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.emptyList());

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenUsingSameTimeAndDaysAsAnotherMass_then400_withError_MASS_011() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateMassId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{5});

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.011"),
                                jsonPath("$.errors[0].payload.duplicateMass.id").value(duplicate.getId())
                        )
                );
    }

    @Test
    void whenDuplicateHasSameId_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId(MASS_ID_STUB);
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{5});

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenDuplicateIsDeleted_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{5});
        duplicate.setDeleted(true);

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenDuplicateHasAnotherTime_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:29");//вместо 16:28
        duplicate.setDays(new int[]{5});

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenDuplicateHasAnotherPeriod_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{5});
        duplicate.setStartDate(LocalDate.of(2023, 2, 21));// 02/21/2023 (слудеющий день, после endDate)

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenDuplicateHasAnotherDays_then201() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{1, 2, 3, 4, 6, 7});//все, кроме 5

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenHasOnly1CommonDay_withMass_then400_withError_MASS_011() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 1 ],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{1});//единственный общий день, на который выпадает месса
        duplicate.setStartDate(LocalDate.of(2023, 2, 20));// 02/20/2023 совпадает с endDate

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.011"),
                                jsonPath("$.errors[0].payload.duplicateMass.id").value(duplicate.getId())
                        )
                );
    }

    @Test
    void whenHasOnly1CommonDay_withoutMass_then200() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 1 ],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        final Mass duplicate = new Mass();
        duplicate.setId("duplicateId");
        duplicate.setTime("16:28");
        duplicate.setDays(new int[]{2});//единственный общий день, на который НЕ выпадает месса
        duplicate.setStartDate(LocalDate.of(2023, 2, 20));// 02/20/2023 совпадает с endDate

        when(massService.getMassByParish("63b67ddaef7fdb7e473eb06b")).thenReturn(Collections.singletonList(duplicate));

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }


    @NullSource
    @ValueSource(strings = "\"\"")
    @ParameterizedTest
    void whenCityIdIsNullOrEmpty_then400_withError_MASS_003(final String cityId) throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": " + cityId + "\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("cityId"),
                                jsonPath("$.errors[0].code").value("MASS.003"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @NullSource
    @ValueSource(strings = "\"\"")
    @ParameterizedTest
    void whenLangCodeIsNullOrEmpty_then400_withError_MASS_004(final String langCode) throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": " + langCode + ",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"11b67ddaef7fdb7e473eb06b\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("langCode"),
                                jsonPath("$.errors[0].code").value("MASS.004"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @NullSource
    @ValueSource(strings = "\"\"")
    @ParameterizedTest
    void whenParishIdIsNullOrEmpty_then400_withError_MASS_010(final String parishId) throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [ 5 ],\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": " + parishId + ",\n" +
                "    \"cityId\": \"63b67ddaef7fdb7e473eb06b\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("parishId"),
                                jsonPath("$.errors[0].code").value("MASS.010"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @ValueSource(strings = {
            "-0",
            "1233",
            "24:00",
            "23:60"
    })
    @ParameterizedTest
    void whenPeriodicMassHasInvalidTimeFormat_then400_withError_MASS_005(final String time) throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"" + time + "\",\n" +
                "    \"days\": [ 1 ],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.005"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenPeriodicMassHasNullTime_then400_withError_MASS_006() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": null,\n" +
                "    \"days\": [ 1 ],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.006"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenPeriodicMassHasEmptyDays_then400_withError_MASS_008() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": [],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.008"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenPeriodicMassHasNullDays_then400_withError_MASS_008() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"days\": null,\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.008"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenSingleMassHasTime_then400_withError_MASS_007() throws Exception {
        final String requestBody = "{\n" +
                "    \"time\": \"16:28\",\n" +
                "    \"singleStartTimestamp\": 1,\n" +
                "    \"days\": null,\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("time"),
                                jsonPath("$.errors[0].code").value("MASS.007"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenSingleMassHasDays_then400_withError_MASS_009() throws Exception {
        final String requestBody = "{\n" +
                "    \"singleStartTimestamp\": 1,\n" +
                "    \"days\": [1],\n" +
                "    \"endDate\":\"02/20/2023\",\n" +
                "    \"langCode\": \"беларуская\",\n" +
                "    \"parishId\": \"63b67ddaef7fdb7e473eb06b\",\n" +
                "    \"cityId\": \"63b67690ef7fdb7e473eb06a\"\n" +
                "}";

        mockMvc.perform(put(UPDATE_MASS_END_POINT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(UPDATE_MASS_END_POINT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("days"),
                                jsonPath("$.errors[0].code").value("MASS.009"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }
}
