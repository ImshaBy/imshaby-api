package by.imsha.domain;

import lombok.Data;

@Data
public class LocalizedParish extends LocalizedBaseInfo {
    private String name;
    private String address;
    public LocalizedParish(String lang, String originObjId) {
        super(lang, originObjId);
    }


}
