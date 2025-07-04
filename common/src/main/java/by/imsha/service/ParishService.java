package by.imsha.service;


import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.ParishInfo;
import by.imsha.domain.dto.mapper.ParishInfoMapper;
import by.imsha.repository.ParishRepository;
import by.imsha.repository.projection.ParishExpirationInfo;
import by.imsha.utils.ServiceUtils;
import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static by.imsha.utils.Constants.LIMIT;
import static by.imsha.utils.Constants.PAGE;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ParishService {

    @Autowired
    private ParishInfoMapper parishInfoMapper;

    private static Logger logger = LoggerFactory.getLogger(ParishService.class);

    private QueryConversionPipeline pipeline = QueryConversionPipeline.defaultPipeline();

    private MongoVisitor mongoVisitor = new MongoVisitor();

    @Autowired
    ParishRepository parishRepository;

    public Parish createParish(Parish parish){
        return parishRepository.save(parish);
    }

    public List<Parish> createParishesWithList(List<Parish> parishes){
        return parishRepository.saveAll(parishes);
    }

    public Parish getParishByUser(String userId){
        return parishRepository.findByUserId(userId);
    }

    public Optional<Parish> getParish(String id){
        return parishRepository.findById(id);
    }

    public List<Parish> search(String filter){
        // TODO can be added default sorting
        return search(filter, PAGE, LIMIT, null);
    }

    public List<Parish> search(String filter, int offset, int limit, String sort){
        if(StringUtils.isBlank(filter)){
            if(logger.isInfoEnabled()){
                logger.info("No searching parishes: query is blank");
            }
            return null;
        }

        int[] offsetAndLimit = ServiceUtils.calculateOffsetAndLimit(offset, limit);
        Condition<GeneralQueryBuilder> condition = pipeline.apply(filter, Parish.class);
        Query query = ServiceUtils.buildMongoQuery(sort, offsetAndLimit[0], offsetAndLimit[1], condition, mongoVisitor);
        List<Parish> parishes = this.parishRepository.search(query, Parish.class);
        return parishes;
    }





    //    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Parish updateParish(ParishInfo parishInfo, Parish parishToUpdate){
        parishInfoMapper.updateParishFromDTO(parishInfo, parishToUpdate);
        return parishRepository.save(parishToUpdate);
    }

    public Parish updateParish(Parish parishToUpdate){
        return parishRepository.save(parishToUpdate);
    }

    public Parish updateLocalizedParishInfo(LocalizedParish localizedParishInput, Parish parish){
        LocalizedParish currentParishInfo = (LocalizedParish)parish.getLocalizedInfo().get(localizedParishInput.getLang());
        if(currentParishInfo != null){
            parishInfoMapper.updateLocalizedParishFromDTO(localizedParishInput, currentParishInfo);
        }else{
            parish.getLocalizedInfo().put(localizedParishInput.getLang(), localizedParishInput);
        }
        return updateParish(parish);
    }

    //TODO enable for production env
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void removeParish(String id){
        parishRepository.deleteParishById(id);
    }


    @Cacheable(cacheNames = "pendingParishes", key = "'parishCity:' + #cityId")
    public Set<String> getNotApprovedParishIds(final String cityId) {
        Query query = new Query();
        query.fields()
                .include("id");
        query.addCriteria(
                where("state").ne(Parish.State.APPROVED).and("cityId").is(cityId)
        );

        return parishRepository.search(query, Parish.class).stream()
                .map(Parish::getId)
                .collect(Collectors.toSet());
    }

    public List<Parish> findAllNotApprovedAndNotDisabled() {
        return parishRepository.findAllNotApprovedAndNotDisabled();
    }

    public Optional<Parish> findParishByKey(final String key) {
        return parishRepository.findParishByKey(key);
    }

    public ParishExpirationInfo getParishExpirationInfo() {
        return parishRepository.getParishExpirationData(LocalDateTime.now());
    }
}
