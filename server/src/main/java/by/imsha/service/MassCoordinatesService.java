package by.imsha.service;

import by.imsha.mapper.MassCoordinatesMapper;
import by.imsha.rest.dto.MassCoordinatesResponse.MassCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MassCoordinatesService {

    private final ParishService parishService;
    private final MassCoordinatesMapper massCoordinatesMapper;

    @Cacheable("notApprovedAndNotDisabledParishesMassCoordinates")
    public List<MassCoordinate> getNotApprovedAndNotDisabledParishesMassCoordinates() {
        return massCoordinatesMapper.map(parishService.findAllNotApprovedAndNotDisabled());
    }
}
