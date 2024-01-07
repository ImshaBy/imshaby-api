package by.imsha.server.rest.passwordless.adapter;

import by.imsha.server.TestTimeConfiguration;
import by.imsha.server.ValidationConfiguration;
import by.imsha.server.properties.PasswordlessApiProperties;
import by.imsha.server.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.server.rest.passwordless.handler.LoginHandler;
import by.imsha.server.rest.passwordless.handler.StartHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static by.imsha.server.TestTimeConfiguration.ERROR_TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PasswordlessApiAdapter.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class PasswordlessApiAdapterTest {

    private static final String ROOT_PATH = "/api/passwordless";

    @MockBean
    private PasswordlessApiProperties passwordlessApiProperties;
    @MockBean
    private StartHandler startHandler;
    @MockBean
    private LoginHandler loginHandler;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenStartRequestHasNoEmail_then400_andPASSWORDLESS001() throws Exception {
        final String testUri = ROOT_PATH + "/start";

        mockMvc.perform(post(testUri)
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
                                jsonPath("$.requestInfo.method").value("POST"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("email"),
                                jsonPath("$.errors[0].code").value("PASSWORDLESS.001"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenStartRequestHasValidEmail_then200_andStartHandlerParamsValid() throws Exception {
        final String testUri = ROOT_PATH + "/start";

        final ArgumentCaptor<StartHandler.Input> inputArgumentCaptor = ArgumentCaptor.forClass(StartHandler.Input.class);

        when(passwordlessApiProperties.getApplicationId()).thenReturn("testApplicationId");
        doNothing().when(startHandler).handle(inputArgumentCaptor.capture());

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"email\":\"testEmail\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());

        final StartHandler.Input inputData = inputArgumentCaptor.getValue();

        assertAll(
                () -> verify(startHandler).handle(inputData),
                () -> assertThat(inputData.getApplicationId()).isEqualTo("testApplicationId"),
                () -> assertThat(inputData.getLoginId()).isEqualTo("testEmail")
        );
    }

    @Test
    void whenLoginRequestHasNoCode_then400_andPASSWORDLESS002() throws Exception {
        final String testUri = ROOT_PATH + "/login";

        mockMvc.perform(post(testUri)
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
                                jsonPath("$.requestInfo.method").value("POST"),
                                jsonPath("$.requestInfo.pathInfo").value(testUri),
                                jsonPath("$.requestInfo.query").isEmpty(),
                                jsonPath("$.errors", hasSize(1)),
                                jsonPath("$.errors[0].field").value("code"),
                                jsonPath("$.errors[0].code").value("PASSWORDLESS.002"),
                                jsonPath("$.errors[0].payload").isEmpty()
                        )
                );
    }

    @Test
    void whenLoginRequestHasValidCode_then200_andStartHandlerParamsValid() throws Exception {
        final String testUri = ROOT_PATH + "/login";

        final ArgumentCaptor<LoginHandler.Input> inputArgumentCaptor = ArgumentCaptor.forClass(LoginHandler.Input.class);

        when(loginHandler.handle(inputArgumentCaptor.capture())).thenReturn("testToken");

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"code\":\"testCode\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("testToken"));

        final LoginHandler.Input inputData = inputArgumentCaptor.getValue();

        assertAll(
                () -> verify(loginHandler).handle(inputData),
                () -> assertThat(inputData.getCode()).isEqualTo("testCode")
        );
    }

    @Test
    void whenStartRequest_andNotNotifiablePasswordlessApiExceptionThrown_then200() throws Exception {
        final String testUri = ROOT_PATH + "/start";

        doThrow(new PasswordlessApiException("test", false)).when(startHandler).handle(any(StartHandler.Input.class));

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"email\":\"testEmail\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenStartRequest_andNotifiablePasswordlessApiExceptionThrown_then400() throws Exception {
        final String testUri = ROOT_PATH + "/start";

        doThrow(new PasswordlessApiException("test", true)).when(startHandler).handle(any(StartHandler.Input.class));

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"email\":\"testEmail\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void whenLoginRequest_andNotNotifiablePasswordlessApiExceptionThrown_then200() throws Exception {
        final String testUri = ROOT_PATH + "/login";

        doThrow(new PasswordlessApiException("test", false)).when(loginHandler).handle(any(LoginHandler.Input.class));

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"code\":\"test\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void whenLoginRequest_andNotifiablePasswordlessApiExceptionThrown_then200() throws Exception {
        final String testUri = ROOT_PATH + "/login";

        doThrow(new PasswordlessApiException("test", true)).when(loginHandler).handle(any(LoginHandler.Input.class));

        mockMvc.perform(post(testUri)
                        .contentType("application/json")
                        .content("{\"code\":\"test\"}")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
}
