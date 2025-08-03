package by.imsha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"api_specification.by.imsha.server", "by.imsha"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}


