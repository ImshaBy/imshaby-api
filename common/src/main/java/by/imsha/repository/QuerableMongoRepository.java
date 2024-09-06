package by.imsha.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * @author Alena Misan
 */

@NoRepositoryBean
public interface QuerableMongoRepository<T, ID extends Serializable> extends MongoRepository<T, ID>{

    public List<T> search(Query query, Class<T> classEntity);

}
