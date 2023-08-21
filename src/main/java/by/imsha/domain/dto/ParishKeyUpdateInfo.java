package by.imsha.domain.dto;


import lombok.Data;

@Data
public class ParishKeyUpdateInfo {
    private String key;
    private String name;
    private String id;
    private Integer updatePeriodInDays;
    private String supportPhone;
    private String phone;
}
