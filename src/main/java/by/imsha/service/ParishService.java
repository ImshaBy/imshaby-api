package by.imsha.service;


import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassParishInfo;
import by.imsha.domain.dto.ParishInfo;
import by.imsha.domain.dto.ParishKeyUpdateInfo;
import by.imsha.domain.dto.mapper.MassParishInfoMapper;
import by.imsha.domain.dto.mapper.ParishInfoMapper;
import by.imsha.domain.dto.mapper.ParishKeyUpdateInfoMapper;
import by.imsha.repository.ParishRepository;
import by.imsha.utils.ServiceUtils;
import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import static by.imsha.utils.Constants.LIMIT;
import static by.imsha.utils.Constants.PAGE;

@Service
public class ParishService {

    private static ParishService INSTANCE;

    @PostConstruct
    public void initInstance(){
        INSTANCE = this;
    }

    public static MassParishInfo extractMassParishInfo(String parishId){
        Parish parish = INSTANCE.getParish(parishId).get();
        return MassParishInfoMapper.MAPPER.toMassParishInfo(parish);
    }

    public static ParishKeyUpdateInfo extractParishKeyUpdateInfo(String parishId){
        Parish parish = INSTANCE.getParish(parishId).get();
        return ParishKeyUpdateInfoMapper.MAPPER.toParishKeyUpdateInfo(parish);
    }


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
        ParishInfoMapper.MAPPER.updateParishFromDTO(parishInfo, parishToUpdate);
        return parishRepository.save(parishToUpdate);
    }



    public Parish updateParish(Parish parishToUpdate){
        return parishRepository.save(parishToUpdate);
    }

    //TODO enable for production env
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void removeParish(String id){
        parishRepository.deleteParishById(id);
    }


}
