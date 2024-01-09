package by.imsha.timing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControllerTest {

    @Autowired
    private ServiceTest serviceTest;

    @GetMapping("/test/timing")
    public void test() {
        serviceTest.test();
    }
}
