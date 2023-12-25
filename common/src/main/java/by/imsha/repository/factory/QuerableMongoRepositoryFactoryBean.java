package by.imsha.repository.factory;

import by.imsha.repository.QuerableMongoRepository;
import by.imsha.repository.impl.QuerableMongoRepositoryImpl;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.io.Serializable;

/**
 * @author Alena Misan
 */

public class QuerableMongoRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends MongoRepositoryFactoryBean<T, S, ID> {


    public QuerableMongoRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(MongoOperations operations) {
        return new QuerableMongoRepositoryFactory<S, ID>(operations);
    }

    private static class QuerableMongoRepositoryFactory<S, ID extends Serializable> extends MongoRepositoryFactory{

        private final MongoOperations mongoOperations;

        private QuerableMongoRepositoryFactory(MongoOperations mongoOperations) {
            super(mongoOperations);
            this.mongoOperations = mongoOperations;
        }

        @Override
        protected Object getTargetRepository(RepositoryInformation information) {
            MongoEntityInformation entityInformation =  super.getEntityInformation(information.getDomainType());
            return new QuerableMongoRepositoryImpl<S, ID>(entityInformation, this.mongoOperations);
        }

        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            // The RepositoryMetadata can be safely ignored, it is used by the JpaRepositoryFactory
            //to check for QueryDslJpaRepository's which is out of scope.
            return QuerableMongoRepository.class;
        }
    }
}
