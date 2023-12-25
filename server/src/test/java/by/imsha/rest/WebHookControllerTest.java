package by.imsha.rest;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.domain.EntityWebhook;
import by.imsha.domain.dto.WebHookInfo;
import by.imsha.service.EntityWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static by.imsha.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({WebHookController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class WebHookControllerTest {

    private static final String ROOT_PATH = "/hook";

    @MockBean
    private EntityWebhookService entityWebhookService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void whenCreateCityWebhookRequestHasEmptyBody_then400_andWEBHOOK001_andWEBHOOK002() throws Exception {

        final String testUri = ROOT_PATH + "/city";

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().json("{\"timestamp\":\"2023-07-16T16:43:01+03:00\",\"requestInfo\":{\"uri\":\"/hook/city\",\"method\":\"POST\",\"pathInfo\":\"/hook/city\",\"query\":null},\"errors\":[{\"field\":\"url\",\"code\":\"WEBHOOK.002\",\"payload\":null},{\"field\":\"key\",\"code\":\"WEBHOOK.001\",\"payload\":null}]}")
                );
    }

    @Test
    void whenCreateCityWebhookRequestHasValidBody_then200() throws Exception {

        final String testUri = ROOT_PATH + "/city";
        final EntityWebhook entityWebhook = EntityWebhook.builder()
                .id("hookId")
                .key("testKey")
                .url("testUrl")
                .type("city")
                .build();

        final ArgumentCaptor<WebHookInfo> webHookInfoArgumentCaptor = ArgumentCaptor.forClass(WebHookInfo.class);
        when(entityWebhookService.createCityWebHook(webHookInfoArgumentCaptor.capture())).thenReturn(entityWebhook);

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"key\":\"testKey\",\"url\":\"testUrl\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(entityWebhook))
                );

        final WebHookInfo capturedWebHookInfo = webHookInfoArgumentCaptor.getValue();

        assertAll(
                () -> verify(entityWebhookService).createCityWebHook(capturedWebHookInfo),
                () -> assertThat(capturedWebHookInfo.getKey()).isEqualTo("testKey"),
                () -> assertThat(capturedWebHookInfo.getUrl()).isEqualTo("testUrl")
        );
    }

    @Test
    void whenCreateParishWebhookRequestHasEmptyBody_then400_andWEBHOOK001_andWEBHOOK002() throws Exception {

        final String testUri = ROOT_PATH + "/parish";

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().json("{\"timestamp\":\"2023-07-16T16:43:01+03:00\",\"requestInfo\":{\"uri\":\"/hook/parish\",\"method\":\"POST\",\"pathInfo\":\"/hook/parish\",\"query\":null},\"errors\":[{\"field\":\"url\",\"code\":\"WEBHOOK.002\",\"payload\":null},{\"field\":\"key\",\"code\":\"WEBHOOK.001\",\"payload\":null}]}")
                );
    }

    @Test
    void whenCreateParishWebhookRequestHasValidBody_then200() throws Exception {

        final String testUri = ROOT_PATH + "/parish";
        final EntityWebhook entityWebhook = EntityWebhook.builder()
                .id("hookId")
                .key("testKey")
                .url("testUrl")
                .type("city")
                .build();

        final ArgumentCaptor<WebHookInfo> webHookInfoArgumentCaptor = ArgumentCaptor.forClass(WebHookInfo.class);
        when(entityWebhookService.createParishWebHook(webHookInfoArgumentCaptor.capture())).thenReturn(entityWebhook);

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"key\":\"testKey\",\"url\":\"testUrl\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(entityWebhook))
                );

        final WebHookInfo capturedWebHookInfo = webHookInfoArgumentCaptor.getValue();

        assertAll(
                () -> verify(entityWebhookService).createParishWebHook(capturedWebHookInfo),
                () -> assertThat(capturedWebHookInfo.getKey()).isEqualTo("testKey"),
                () -> assertThat(capturedWebHookInfo.getUrl()).isEqualTo("testUrl")
        );
    }

    @Test
    void whenGetAllHooksRequestHasNoParams_then200_andDefaultValuesUsed() throws Exception {

        final EntityWebhook firstEntityWebhook = EntityWebhook.builder()
                .id("FIRST_WEBHOOK_ID")
                .type("city")
                .key("key1")
                .url("url1")
                .build();
        final EntityWebhook secondEntityWebhook = EntityWebhook.builder()
                .id("SECOND_WEBHOOK_ID")
                .type("city")
                .key("key2")
                .url("url2")
                .build();
        final PageRequest defaultPageRequest = PageRequest.of(0, 40);
        final PageImpl<EntityWebhook> responsePage = new PageImpl<>(Arrays.asList(firstEntityWebhook, secondEntityWebhook),
                defaultPageRequest, 2);

        when(entityWebhookService.getAllHooks(0, 40)).thenReturn(responsePage);

        mockMvc.perform(get(ROOT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );


        verify(entityWebhookService).getAllHooks(0, 40);
    }

    @Test
    void whenGetAllHooksRequestHasParams_then200() throws Exception {

        final String testUri = ROOT_PATH + "?page=11&size=23";

        final EntityWebhook firstEntityWebhook = EntityWebhook.builder()
                .id("FIRST_WEBHOOK_ID")
                .type("city")
                .key("key1")
                .url("url1")
                .build();
        final EntityWebhook secondEntityWebhook = EntityWebhook.builder()
                .id("SECOND_WEBHOOK_ID")
                .type("city")
                .key("key2")
                .url("url2")
                .build();
        final PageRequest currentPageRequest = PageRequest.of(11, 23);
        final PageImpl<EntityWebhook> responsePage = new PageImpl<>(Arrays.asList(firstEntityWebhook, secondEntityWebhook),
                currentPageRequest, 2);

        when(entityWebhookService.getAllHooks(11, 23)).thenReturn(responsePage);

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(responsePage))
                );


        verify(entityWebhookService).getAllHooks(11, 23);
    }

    @Test
    void whenGetHookRequestValid_andHookNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(entityWebhookService.retrieveHook("any_id")).thenReturn(Optional.empty());

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
    void whenGetHookRequestValid_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final EntityWebhook entityWebhook = EntityWebhook.builder()
                .id("WEBHOOK_ID")
                .type("city")
                .key("testKey")
                .url("testUrl")
                .build();

        when(entityWebhookService.retrieveHook("any_id")).thenReturn(Optional.of(entityWebhook));

        mockMvc.perform(get(testUri)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        content().json(objectMapper.writeValueAsString(entityWebhook))
                );
    }

    @Test
    void whenDeleteCityRequestValid_andCityNotFound_then404() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        when(entityWebhookService.retrieveHook("any_id")).thenReturn(Optional.empty());

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
    void whenDeleteCityRequestValid_andCityFound_then200() throws Exception {

        final String testUri = ROOT_PATH + "/any_id";

        final EntityWebhook entityWebhook = EntityWebhook.builder()
                .id("WEBHOOK_ID")
                .type("city")
                .key("testKey")
                .url("testUrl")
                .build();

        when(entityWebhookService.retrieveHook("any_id")).thenReturn(Optional.of(entityWebhook));
        doNothing().when(entityWebhookService).removeHook("any_id");

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
                () -> verify(entityWebhookService).retrieveHook("any_id"),
                () -> verify(entityWebhookService).removeHook("any_id")
        );
    }
}
