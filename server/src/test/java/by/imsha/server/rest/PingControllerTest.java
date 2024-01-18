package by.imsha.server.rest;

import by.imsha.rest.PingController;
import by.imsha.server.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PingController.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class PingControllerTest {

    private static final String PING_END_POINT_PATH = "/";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenPing_then200() throws Exception {

        mockMvc.perform(get(PING_END_POINT_PATH)
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(
                        //тянется из spring.web.locale
                        jsonPath("$.name").value("locale: be_BY")
                );

    }
}
