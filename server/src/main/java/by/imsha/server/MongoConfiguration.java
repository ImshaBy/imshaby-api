package by.imsha.server;

import by.imsha.repository.factory.QuerableMongoRepositoryFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        repositoryFactoryBeanClass = QuerableMongoRepositoryFactoryBean.class
)
@EnableMongoAuditing
public class MongoConfiguration {
}
