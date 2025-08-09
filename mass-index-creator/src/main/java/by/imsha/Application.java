package by.imsha;

import by.imsha.service.MassIndexService;
import by.imsha.utils.Constants;
import by.imsha.utils.UserLocaleHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("api_specification.by.imsha.common")
public class Application {

    public static void main(String[] args) {
        //docker run -it --rm -p 7700:7700 -e MEILI_MASTER_KEY=8Yy6fAZCCIPDiQk4yay7_xrHoxconfRKC5HKhuoij8o getmeili/meilisearch:v1.12.1
        //FIXME устанавливаем дефолтную локаль (все остальные не учитываются)
        UserLocaleHolder.setUserLocale(Constants.DEFAULT_LANG);
        MassIndexService massIndexService = SpringApplication.run(Application.class, args)
                .getBean(MassIndexService.class);
        massIndexService.updateIndex();
    }

}


