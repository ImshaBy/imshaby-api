package by.imsha.domain;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.rest.serializers.TrimStringDeserializer;
import by.imsha.service.MassService;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongodb.lang.Nullable;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    @Indexed(unique=true)
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
        return ServiceUtils.needUpdateFromNow(lastConfirmRelevance, getUpdatePeriodInDays());
    }

    public String getName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedName = name;
        if(localizedBaseInfo != null){
            calculatedName = ((LocalizedParish) localizedBaseInfo).getName();
        }
        return calculatedName;
    }

    public String getShortName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedShortName = shortName;
        if(localizedBaseInfo != null){
            calculatedShortName = ((LocalizedParish) localizedBaseInfo).getShortName();
        }
        return calculatedShortName;
    }

    public String getAddress() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calcAddress = address;
        if(localizedBaseInfo != null){
            calcAddress = ((LocalizedParish)localizedBaseInfo).getAddress();
        }
        return calcAddress;
    }

    /**
     * Состояние парафии
     */
    public enum State {
        /**
         * Ожидает подтверждения
         */
        PENDING,
        /**
         * Подтверждена
         */
        APPROVED,
    }

}
