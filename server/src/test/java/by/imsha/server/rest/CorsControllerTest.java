package by.imsha.server.rest;

import by.imsha.ValidationConfiguration;
import by.imsha.domain.Cors;
import by.imsha.domain.dto.CorsInfo;
import by.imsha.domain.dto.mapper.CorsMapper;
import by.imsha.repository.CorsConfigRepository;
import by.imsha.rest.CorsController;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.service.CorsConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CorsController.class, ValidationConfiguration.class, CorsConfigService.class, TestTimeConfiguration.class})
class CorsControllerTest {

    private static final String ROOT_PATH = "/api/cors";

    @MockBean
    private CorsConfigRepository corsConfigRepository;
    @MockBean
    private CorsMapper corsMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CorsConfigService corsConfigService;
    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @CsvSource({"{}", "{\"origin\":\"\"}"})
    void whenCreateRequestHasNoOrigin_then400_andCORS001(final String requestBody) throws Exception {

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(ROOT_PATH),
                                jsonPath("$.requestInfo.method").value("POST"),
                                jsonPath("$.requestInfo.pathInfo").value(ROOT_PATH),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("origin"),
                                jsonPath("$.errors[0].code").value("CORS.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenCreateRequestValid_then201() throws Exception {

        final String requestBody = "{\"origin\":\"testOrigin\"}";

        final Cors corsStub = Cors.builder()
                .id("stubId")
                .origin("stubOrigin")
                .build();

        final ArgumentCaptor<Cors> corsCaptor = ArgumentCaptor.forClass(Cors.class);

        when(corsConfigRepository.save(corsCaptor.capture())).thenReturn(corsStub);

        mockMvc.perform(post(ROOT_PATH)
                        .contentType("application/json")
                        .content(requestBody)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(corsStub))
                );

        final Cors valueForStore = corsCaptor.getValue();

        assertAll(
                () -> verify(corsConfigRepository).save(valueForStore),
                () -> assertThat(valueForStore.getId()).isNull(),
                () -> assertThat(valueForStore.getOrigin()).isEqualTo("testOrigin")
        );
    }

    @Test
    void whenGetAllCorsRequestHasNoParams_then200_andDefaultValuesUsed() throws Exception {

        final Cors firstCors = Cors.builder()
                .id("FIRST_CORS_ID")
                .build();
        final Cors secondCors = Cors.builder()
                .id("SECOND_CORS_ID")
                .build();
        final PageRequest defaultPageRequest = PageRequest.of(0, 40);
        final PageImpl<Cors> responsePage = new PageImpl<>(Arrays.asList(firstCors, secondCors), defaultPageRequest, 2);

        final ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(corsConfigRepository.findAll(pageRequestArgumentCaptor.capture())).thenReturn(responsePage);

        mockMvc.perform(get(ROOT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );

        final PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

        assertAll(
                () -> verify(corsConfigRepository).findAll(capturedPageRequest),
                () -> assertThat(capturedPageRequest.getPageSize()).isEqualTo(40),
                () -> assertThat(capturedPageRequest.getPageNumber()).isZero()
        );
    }

    @Test
    void whenGetAllCorsRequestHasParams_then200() throws Exception {

        final String testUri = ROOT_PATH + "?page=11&size=23";

        final Cors firstCors = Cors.builder()
                .id("FIRST_CORS_ID")
                .build();
        final Cors secondCors = Cors.builder()
                .id("SECOND_CORS_ID")
                .build();
        final PageRequest currentPageRequest = PageRequest.of(11, 23);
        final PageImpl<Cors> responsePage = new PageImpl<>(Arrays.asList(firstCors, secondCors), currentPageRequest, 2);

        final ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(corsConfigRepository.findAll(pageRequestArgumentCaptor.capture())).thenReturn(responsePage);

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );

        final PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

        assertAll(
                () -> verify(corsConfigRepository).findAll(capturedPageRequest),
                () -> assertThat(capturedPageRequest.getPageSize()).isEqualTo(23),
                () -> assertThat(capturedPageRequest.getPageNumber()).isEqualTo(11)
        );
    }

    @Test
    void whenGetCorsRequestValid_andCorsNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(get(testUri)
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
    }

    @Test
    void whenGetCorsRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";
        final Cors cors = Cors.builder()
                .id("any_id")
                .origin("any_origin")
                .build();

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.of(cors));

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(cors))
                );
    }

    @Test
    void whenUpdateCorsRequestValid_andCorsNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors").isEmpty()
                        )
                );
    }

    @Test
    void whenUpdateRequestHasNoOrigin_then400_andCORS001() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final Cors cors = Cors.builder()
                .id("any_id")
                .origin("")
                .build();

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.of(cors));

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        matchAll(
                                jsonPath("$.timestamp").value(ERROR_TIMESTAMP),
                                jsonPath("$.requestInfo.uri").value(testUri),
                                jsonPath("$.requestInfo.method").value("PUT"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("origin"),
                                jsonPath("$.errors[0].code").value("CORS.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenUpdateCorsRequestValid_andCorsFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final Cors cors = Cors.builder()
                .id("any_id")
                .origin("origin")
                .build();
        final ArgumentCaptor<CorsInfo> corsInfoArgumentCaptor = ArgumentCaptor.forClass(CorsInfo.class);

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.of(cors));
        doNothing().when(corsMapper).updateCorsFromDTO(corsInfoArgumentCaptor.capture(), eq(cors));
        when(corsConfigRepository.save(cors)).thenReturn(cors);

        mockMvc.perform(put(testUri)
                        .contentType("application/json")
                        .content("{\"origin\":\"newOrigin\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("UPDATED")
                        )
                );

        final CorsInfo corsInfo = corsInfoArgumentCaptor.getValue();

        assertAll(
                () -> verify(corsConfigRepository).findById("any_id"),
                () -> verify(corsConfigRepository).save(cors),
                () -> verify(corsMapper).updateCorsFromDTO(corsInfo, cors),
                () -> assertThat(corsInfo.getOrigin()).isEqualTo("newOrigin")
        );
    }

    @Test
    void whenDeleteCorsRequestValid_andCorsNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.empty());

        mockMvc.perform(delete(testUri)
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
    }

    @Test
    void whenDeleteCorsRequestValid_andCorsFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final Cors cors = Cors.builder()
                .id("any_id")
                .origin("origin")
                .build();

        when(corsConfigRepository.findById("any_id")).thenReturn(Optional.of(cors));
        doNothing().when(corsConfigRepository).deleteById("any_id");

        mockMvc.perform(delete(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(
                        matchAll(
                                jsonPath("$.id").value("any_id"),
                                jsonPath("$.status").value("DELETED")
                        )
                );

        assertAll(
                () -> verify(corsConfigRepository).findById("any_id"),
                () -> verify(corsConfigRepository).deleteById("any_id")
        );
    }
}