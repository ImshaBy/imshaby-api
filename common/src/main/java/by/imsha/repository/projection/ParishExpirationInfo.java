package by.imsha.repository.projection;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Информация об устаревании расписаний парафий
 */
@Data
public class ParishExpirationInfo {

    /**
     * Парафии, расписание которых в скором времени устареет
     */
    private List<ParishData> almostExpiredParishes;
    /**
     * Парафии, расписание который уже не актуально
     */
    private List<ParishData> expiredParishes;

    @Data
    public static class ParishData {
        private String id;
        private String key;
        private String name;
        private String shortName;
        private Integer updatePeriodInDays;
        @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
        private LocalDateTime lastConfirmRelevance;
    }
}
