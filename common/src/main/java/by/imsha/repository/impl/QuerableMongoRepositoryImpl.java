package by.imsha.repository.impl;

import by.imsha.repository.QuerableMongoRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;

import java.io.Serializable;
import java.util.List;

/**
 * @author Alena Misan
 */
public class QuerableMongoRepositoryImpl<T, ID extends Serializable> extends SimpleMongoRepository<T, ID> implements QuerableMongoRepository<T, ID> {


    private MongoOperations mongoOperations;

    public QuerableMongoRepositoryImpl(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
        super(metadata, mongoOperations);
        this.mongoOperations = mongoOperations;
    }


    @Override
    public List<T> search(Query query, Class<T> classEntity) {
        return mongoOperations.find (query, classEntity);
    }
}
