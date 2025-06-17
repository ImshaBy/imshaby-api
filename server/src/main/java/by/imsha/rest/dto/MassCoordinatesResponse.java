package by.imsha.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;

public record MassCoordinatesResponse(List<MassCoordinate> massCoordinates) {

    public record MassCoordinate(@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                 OffsetDateTime massDateTime,
                                 Parish parish) {
    }

    public record Parish(String id, Geo geo, Boolean actual, String state) {
    }

    public record Geo(Double lat, Double lng) {
    }
}
