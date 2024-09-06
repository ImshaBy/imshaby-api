package by.imsha.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum EntityWebHookType {
    PARISH ("parish"),
    CITY ("city");
    @Getter
    private String type;
}
