package by.imsha.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel
@Data
public class LocalizedBaseInfo {
    @JsonIgnore
    private String lang;
    @JsonIgnore
    private String originObjId;

    public LocalizedBaseInfo() {
    }

    public LocalizedBaseInfo(String lang, String originObjId) {
        this.lang = lang;
        this.originObjId = originObjId;
    }


}
