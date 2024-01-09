package by.imsha.timing;

import by.imsha.TestTimeConfiguration;
import by.imsha.ValidationConfiguration;
import by.imsha.aop.timing.TimingAspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableAspectJAutoProxy
@WebMvcTest({ControllerTest.class, ServiceTest.class, TimingAspect.class, ValidationConfiguration.class, TestTimeConfiguration.class})
public class TimingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testServerTiming_then200() throws Exception {
        mockMvc.perform(get("/test/timing")
                        .with(csrf())
                        .with(jwt()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Server-Timing"))
                .andExpect(
                        result -> result.getResponse().getHeader("Server-Timing").matches("^app=\\S+;controller=\\S+;service=\\S+;repository=\\S+$")
                );
    }
}
