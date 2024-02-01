package by.imsha.timing;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.aop.timing.TimingAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAspectJAutoProxy
@WebMvcTest({ControllerTest.class, ServiceTest.class, TimingAspect.class, ValidationConfiguration.class, TestTimeConfiguration.class})
class TimingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testServerTiming_then200() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/test/timing")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Server-Timing"))
                .andReturn().getResponse();

        assertThat(response.getHeader("Server-Timing")).matches("^app=\\d+;controller=\\d+;service=\\d+$");
    }
}
