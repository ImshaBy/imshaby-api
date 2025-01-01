package by.imsha;

import by.imsha.service.MassIndexService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class Application {

    public static void main(String[] args) {
        MassIndexService massIndexService = SpringApplication.run(Application.class, args)
                .getBean(MassIndexService.class);
        massIndexService.updateIndex();
    }

}


