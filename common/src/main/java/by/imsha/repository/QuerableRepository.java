package by.imsha.repository;

import org.springframework.data.repository.NoRepositoryBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alena Misan
 */
@NoRepositoryBean
public interface QuerableRepository<T, ID extends Serializable> {
    public Page<T> search(Query query, Pageable pageable);

}
