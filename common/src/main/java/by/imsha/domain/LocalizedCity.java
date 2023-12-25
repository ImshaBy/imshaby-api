package by.imsha.domain;

import lombok.Data;
@Data
public class LocalizedCity extends LocalizedBaseInfo {

    public LocalizedCity(String lang, String originObjId, String name) {
        super(lang, originObjId);
        this.name = name;
    }

    private String name;

}
