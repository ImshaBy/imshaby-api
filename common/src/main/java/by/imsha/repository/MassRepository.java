package by.imsha.repository;

import by.imsha.domain.Mass;

import java.util.List;

/**
 * @author Alena Misan
 */
public interface MassRepository extends QuerableMongoRepository<Mass, String> {
    List<Mass> findByParishId(String parishId);
    List<Mass> deleteByParishId(String parishId);
    List<Mass> findByCityIdAndDeleted(String cityId, boolean deleted);
}
