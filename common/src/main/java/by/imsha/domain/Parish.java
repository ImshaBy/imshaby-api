package by.imsha.domain;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.serializers.TrimStringDeserializer;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongodb.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static by.imsha.utils.ServiceUtils.BEL_ZONE_ID;

/**
 * Represent Parish class
 */
@Document
@Data
public class Parish {

    @Id
    private String id;
    @NotNull(message = "PARISH.007")
    private State state;
    private String imgPath;
    private String broadcastUrl;
    private String userId;
    @NotEmpty(message = "PARISH.003")
    private String name;
    private String shortName;
    private String address;
    private Coordinate gps;
    @JsonDeserialize(using = TrimStringDeserializer.class)
    @Indexed(unique = true)
    private String key;
    private Integer updatePeriodInDays = 14;
    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();
    private boolean needUpdate;
    @NotEmpty(message = "PARISH.004")
    private String cityId;
    private String phone;
    private String supportPhone;
    @Email(message = "PARISH.005")
    private String email;
    @Email(message = "PARISH.006")
    private String lastModifiedEmail;
    private String website;
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastMassActualDate;
    @LastModifiedDate
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;
    @Nullable
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime  lastConfirmRelevance;

    //TODO В дальнейшем нужно удалить метод и передавать lastConfirmRelevance
    @Deprecated
    public boolean isNeedUpdate() {
        return ServiceUtils.needUpdateFromNow(Optional.ofNullable(lastConfirmRelevance).orElse(lastModifiedDate),
                LocalDateTime.now(BEL_ZONE_ID), getUpdatePeriodInDays());
    }

    /**
     * Состояние парафии
     */
    public enum State {
        /**
         * Исходное состояние
         */
        INITIAL,
        /**
         * Ожидает подтверждения
         */
        PENDING,
        /**
         * Подтверждена
         */
        APPROVED,
        /**
         * Отключена (запрет на отображение)
         */
        DISABLED,
    }

}
